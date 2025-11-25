package com.ampairs.subscription.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.feature.FeatureGate
import com.ampairs.subscription.feature.LimitChecker
import com.ampairs.subscription.feature.LimitUpgradeSuggestion
import com.ampairs.subscription.feature.UsageStatus
import com.ampairs.subscription.repository.SubscriptionRepository
import com.ampairs.workspace.context.WorkspaceContextManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for subscription management screens
 */
class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
    private val featureGate: FeatureGate,
    private val limitChecker: LimitChecker,
    private val userIdProvider: () -> String,
    private val deviceIdProvider: () -> String
) : ViewModel() {

    // Get workspace and user IDs from context managers
    private val workspaceId: String
        get() = WorkspaceContextManager.getInstance().currentWorkspace.value?.id ?: ""

    private val userId: String
        get() = userIdProvider()

    private val deviceId: String
        get() = deviceIdProvider()

    // ======================
    // UI State
    // ======================

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SubscriptionEvent>()
    val events: SharedFlow<SubscriptionEvent> = _events.asSharedFlow()

    // ======================
    // Reactive Flows
    // ======================

    val subscription: StateFlow<SubscriptionState?> = repository
        .observeSubscription(workspaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val plans: StateFlow<List<SubscriptionPlanDefinition>> = repository
        .observePlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val devices: StateFlow<List<DeviceRegistration>> = repository
        .observeDevices(workspaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val usageStatus: StateFlow<UsageStatus?> = limitChecker
        .observeUsageStatus(workspaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentPlan: StateFlow<SubscriptionPlan> = featureGate
        .observeCurrentPlan(workspaceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SubscriptionPlan.FREE)

    // ======================
    // Initialization
    // ======================

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Sync subscription and plans in parallel
                launch { repository.syncSubscription(workspaceId) }
                launch { repository.syncPlans() }
                launch { repository.syncUsage(workspaceId) }
                launch { repository.syncDevices(workspaceId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ======================
    // Actions
    // ======================

    fun refresh() {
        loadInitialData()
    }

    fun selectPlan(planCode: String) {
        _uiState.update { it.copy(selectedPlanCode = planCode) }
    }

    fun selectBillingCycle(billingCycle: BillingCycle) {
        _uiState.update { it.copy(selectedBillingCycle = billingCycle) }
    }

    /**
     * Start trial for selected plan
     */
    fun startTrial(planCode: String, trialDays: Int = 14) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.startTrial(planCode, trialDays, workspaceId).fold(
                    onSuccess = {
                        _events.emit(SubscriptionEvent.TrialStarted(planCode, trialDays))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to start trial"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Initiate purchase (for desktop checkout)
     */
    fun initiatePurchase(
        planCode: String,
        billingCycle: BillingCycle,
        provider: PaymentProvider,
        currency: String = "INR"
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val request = InitiatePurchaseRequest(
                    planCode = planCode,
                    billingCycle = billingCycle,
                    currency = currency
                )
                repository.initiatePurchase(request, provider).fold(
                    onSuccess = { response ->
                        _events.emit(SubscriptionEvent.CheckoutReady(response))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to initiate purchase"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Verify in-app purchase (after Google Play / App Store purchase)
     */
    fun verifyPurchase(
        provider: PaymentProvider,
        purchaseToken: String,
        productId: String,
        orderId: String? = null,
        packageName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val request = VerifyPurchaseRequest(
                    provider = provider,
                    purchaseToken = purchaseToken,
                    productId = productId,
                    orderId = orderId,
                    packageName = packageName
                )
                repository.verifyPurchase(request, workspaceId).fold(
                    onSuccess = { subscription ->
                        _events.emit(SubscriptionEvent.PurchaseVerified(subscription))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to verify purchase"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Change subscription plan
     */
    fun changePlan(newPlanCode: String, billingCycle: BillingCycle, immediate: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val request = ChangePlanRequest(
                    newPlanCode = newPlanCode,
                    billingCycle = billingCycle,
                    immediate = immediate
                )
                repository.changePlan(request, workspaceId).fold(
                    onSuccess = { response ->
                        _events.emit(SubscriptionEvent.PlanChanged(response))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to change plan"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Cancel subscription
     */
    fun cancelSubscription(immediate: Boolean = false, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val request = CancelSubscriptionRequest(
                    immediate = immediate,
                    reason = reason
                )
                repository.cancelSubscription(request, workspaceId).fold(
                    onSuccess = {
                        _events.emit(SubscriptionEvent.SubscriptionCancelled(immediate))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to cancel subscription"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Pause subscription
     */
    fun pauseSubscription(pauseDays: Int = 30, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                val request = PauseSubscriptionRequest(
                    pauseDays = pauseDays,
                    reason = reason
                )
                repository.pauseSubscription(request, workspaceId).fold(
                    onSuccess = {
                        _events.emit(SubscriptionEvent.SubscriptionPaused(pauseDays))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to pause subscription"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Resume subscription
     */
    fun resumeSubscription() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.resumeSubscription(workspaceId).fold(
                    onSuccess = {
                        _events.emit(SubscriptionEvent.SubscriptionResumed)
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to resume subscription"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Deactivate a device
     */
    fun deactivateDevice(deviceUid: String, reason: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.deactivateDevice(deviceUid, reason).fold(
                    onSuccess = {
                        _events.emit(SubscriptionEvent.DeviceDeactivated(deviceUid))
                    },
                    onFailure = { error ->
                        _uiState.update { it.copy(error = error.message) }
                        _events.emit(SubscriptionEvent.Error(error.message ?: "Failed to deactivate device"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Full sync (for offline-first)
     */
    fun fullSync(lastSyncAt: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            try {
                repository.fullSync(deviceId, userId, workspaceId, lastSyncAt).fold(
                    onSuccess = { response ->
                        _events.emit(SubscriptionEvent.SyncCompleted(response.serverTime))
                    },
                    onFailure = { error ->
                        _events.emit(SubscriptionEvent.SyncFailed(error.message ?: "Sync failed"))
                    }
                )
            } finally {
                _uiState.update { it.copy(isSyncing = false) }
            }
        }
    }

    /**
     * Get upgrade suggestion for a resource limit
     */
    suspend fun getUpgradeSuggestion(resourceType: String): LimitUpgradeSuggestion? {
        return limitChecker.getUpgradeSuggestion(workspaceId, resourceType)
    }

    /**
     * Check if feature is available
     */
    suspend fun hasFeature(feature: com.ampairs.subscription.feature.SubscriptionFeature): Boolean {
        return featureGate.hasFeature(workspaceId, feature)
    }

    /**
     * Check if module is available
     */
    suspend fun hasModule(moduleCode: String): Boolean {
        return featureGate.hasModule(workspaceId, moduleCode)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for subscription screens
 */
data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val selectedPlanCode: String? = null,
    val selectedBillingCycle: BillingCycle = BillingCycle.ANNUAL
)

/**
 * One-time events from the ViewModel
 */
sealed class SubscriptionEvent {
    data class TrialStarted(val planCode: String, val trialDays: Int) : SubscriptionEvent()
    data class CheckoutReady(val response: InitiatePurchaseResponse) : SubscriptionEvent()
    data class PurchaseVerified(val subscription: SubscriptionState) : SubscriptionEvent()
    data class PlanChangedInstantly(val newPlanCode: String) : SubscriptionEvent()
    data class PlanChanged(val response: ChangePlanResponse) : SubscriptionEvent()
    data class SubscriptionCancelled(val immediate: Boolean) : SubscriptionEvent()
    data class SubscriptionPaused(val pauseDays: Int) : SubscriptionEvent()
    data object SubscriptionResumed : SubscriptionEvent()
    data class DeviceDeactivated(val deviceUid: String) : SubscriptionEvent()
    data class SyncCompleted(val serverTime: String) : SubscriptionEvent()
    data class SyncFailed(val reason: String) : SubscriptionEvent()
    data class Error(val message: String) : SubscriptionEvent()
}
