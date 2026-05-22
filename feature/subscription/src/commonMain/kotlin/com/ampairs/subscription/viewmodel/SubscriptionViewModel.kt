package com.ampairs.subscription.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.DeviceService
import com.ampairs.common.di.AppScope
import com.ampairs.subscription.domain.model.*
import com.ampairs.subscription.feature.FeatureGate
import com.ampairs.subscription.feature.LimitChecker
import com.ampairs.subscription.feature.LimitUpgradeSuggestion
import com.ampairs.subscription.feature.UsageStatus
import com.ampairs.subscription.repository.SubscriptionRepository
import com.ampairs.workspace.context.WorkspaceContextManager
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Clock

/**
 * ViewModel for subscription management screens
 */
@Inject
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@OptIn(kotlin.time.ExperimentalTime::class)
class SubscriptionViewModel(
    private val repository: SubscriptionRepository,
    private val featureGate: FeatureGate,
    private val limitChecker: LimitChecker,
    private val tokenRepository: TokenRepository,
    private val deviceService: DeviceService
) : ViewModel() {

    companion object {
        // Cache duration for subscription data (5 minutes)
        private val CACHE_DURATION = 5.minutes

        // Singleton cache to persist across ViewModel instances
        private val lastSyncTimes = mutableMapOf<String, kotlin.time.Instant>()

        /**
         * Check if data needs refresh based on last sync time
         */
        private fun needsRefresh(key: String): Boolean {
            val lastSync = lastSyncTimes[key] ?: return true
            val now = Clock.System.now()
            return (now - lastSync) > CACHE_DURATION
        }

        /**
         * Update last sync time for a key
         */
        private fun updateSyncTime(key: String) {
            lastSyncTimes[key] = Clock.System.now()
        }

        /**
         * Clear cache for a workspace (call on logout or workspace switch)
         */
        fun clearCache(workspaceId: String) {
            lastSyncTimes.keys.removeAll { it.startsWith(workspaceId) }
        }
    }

    // Get workspace and user IDs from context managers
    private val workspaceId: String
        get() = WorkspaceContextManager.getInstance().currentWorkspace.value?.id ?: ""

    private val userId: String
        get() = runBlocking { tokenRepository.getCurrentUserId() ?: "" }

    private val deviceId: String
        get() = deviceService.getDeviceId()

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

    // User's workspace count for discount calculation
    private val _userWorkspaceCount = MutableStateFlow(1)
    val userWorkspaceCount: StateFlow<Int> = _userWorkspaceCount.asStateFlow()

    // Is user eligible for "new user" discounts (pre-launch)
    private val _isNewUser = MutableStateFlow(true) // Default to true for pre-launch
    val isNewUser: StateFlow<Boolean> = _isNewUser.asStateFlow()

    // Active seasonal discount (from any plan)
    val activeSeasonalDiscount: StateFlow<com.ampairs.subscription.domain.model.SeasonalDiscount?> = plans
        .map { plansList -> plansList.firstOrNull { it.seasonalDiscount.isActive }?.seasonalDiscount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Active pre-launch discount (from any plan)
    val activePreLaunchDiscount: StateFlow<com.ampairs.subscription.domain.model.PreLaunchDiscount?> = plans
        .map { plansList -> plansList.firstOrNull { it.preLaunchDiscount.isActive }?.preLaunchDiscount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Payment history
    private val _paymentHistory = MutableStateFlow<List<PaymentTransaction>>(emptyList())
    val paymentHistory: StateFlow<List<PaymentTransaction>> = _paymentHistory.asStateFlow()

    // Payment methods
    private val _paymentMethods = MutableStateFlow<List<PaymentMethod>>(emptyList())
    val paymentMethods: StateFlow<List<PaymentMethod>> = _paymentMethods.asStateFlow()

    // Default payment method
    private val _defaultPaymentMethod = MutableStateFlow<PaymentMethod?>(null)
    val defaultPaymentMethod: StateFlow<PaymentMethod?> = _defaultPaymentMethod.asStateFlow()

    // Payment history pagination
    private val _isLoadingPayments = MutableStateFlow(false)
    val isLoadingPayments: StateFlow<Boolean> = _isLoadingPayments.asStateFlow()

    private val _hasMorePayments = MutableStateFlow(true)
    val hasMorePayments: StateFlow<Boolean> = _hasMorePayments.asStateFlow()

    private var currentPaymentPage = 0

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
                // Only sync if cache is expired (5 minutes)
                if (needsRefresh("${workspaceId}_subscription")) {
                    launch {
                        repository.syncSubscription(workspaceId)
                        updateSyncTime("${workspaceId}_subscription")
                    }
                }

                // Plans are global, not workspace-specific
                if (needsRefresh("global_plans")) {
                    launch {
                        repository.syncPlans()
                        updateSyncTime("global_plans")
                    }
                }

                // Usage data changes frequently, but still cache for 5 minutes
                if (needsRefresh("${workspaceId}_usage")) {
                    launch {
                        repository.syncUsage(workspaceId)
                        updateSyncTime("${workspaceId}_usage")
                    }
                }

                // Devices rarely change, good candidate for caching
                if (needsRefresh("${workspaceId}_devices")) {
                    launch {
                        repository.syncDevices(workspaceId)
                        updateSyncTime("${workspaceId}_devices")
                    }
                }
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

    /**
     * Force refresh subscription data (bypasses cache)
     */
    fun refresh(force: Boolean = false) {
        if (force) {
            // Clear cache for this workspace to force refresh
            clearCache(workspaceId)
        }
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
        currency: String = "INR",
        startPolling: Boolean = false
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

                        // Start polling for desktop platforms (Razorpay/Stripe)
                        if (startPolling && (provider == PaymentProvider.RAZORPAY || provider == PaymentProvider.STRIPE)) {
                            startPollingForSubscriptionUpdate()
                        }
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
     * Poll for subscription update after desktop checkout (Razorpay/Stripe)
     * Used when user completes payment in browser
     */
    private fun startPollingForSubscriptionUpdate() {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60 // Poll for 5 minutes (5 seconds * 60)
            val pollInterval = 5000L // 5 seconds

            while (attempts < maxAttempts) {
                kotlinx.coroutines.delay(pollInterval)

                // Sync subscription to check if payment completed
                repository.syncSubscription(workspaceId).fold(
                    onSuccess = { subscription ->
                        if (subscription.status == SubscriptionStatus.ACTIVE) {
                            // Payment verified, stop polling
                            _events.emit(SubscriptionEvent.PurchaseVerified(subscription))
                            return@launch
                        }
                    },
                    onFailure = {
                        // Continue polling on failure
                    }
                )

                attempts++
            }

            // Timeout - show manual refresh instruction
            _events.emit(
                SubscriptionEvent.Error(
                    "Payment verification is taking longer than expected. " +
                    "If you completed the payment, please refresh the screen to check your subscription status."
                )
            )
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

    /**
     * Set user's workspace count for discount calculation
     */
    fun setWorkspaceCount(count: Int) {
        _userWorkspaceCount.value = count
    }

    /**
     * Set whether user is new (eligible for pre-launch/new-user-only discounts)
     */
    fun setIsNewUser(isNew: Boolean) {
        _isNewUser.value = isNew
    }

    /**
     * Calculate price with discount for a plan
     */
    fun calculateDiscountedPrice(plan: SubscriptionPlanDefinition, currency: String = "INR"): Double {
        return plan.getPriceWithDiscount(currency, _userWorkspaceCount.value, _isNewUser.value)
    }

    /**
     * Calculate total savings with discount
     */
    fun calculateSavings(plan: SubscriptionPlanDefinition, currency: String = "INR"): Double {
        return plan.calculateSavings(currency, _userWorkspaceCount.value, _isNewUser.value)
    }

    /**
     * Check if any discount is available for a plan
     */
    fun isDiscountAvailable(plan: SubscriptionPlanDefinition): Boolean {
        return plan.hasAnyDiscount(_userWorkspaceCount.value, _isNewUser.value)
    }

    /**
     * Get workspace count needed to unlock multi-workspace discount
     */
    fun getWorkspacesNeededForDiscount(plan: SubscriptionPlanDefinition): Int {
        val needed = plan.multiWorkspaceDiscount.minWorkspaces - _userWorkspaceCount.value
        return if (needed > 0) needed else 0
    }

    /**
     * Check if seasonal discount is active for a plan
     */
    fun isSeasonalDiscountActive(plan: SubscriptionPlanDefinition): Boolean {
        return plan.seasonalDiscount.isActive
    }

    /**
     * Check if pre-launch discount is active and user is eligible
     */
    fun isPreLaunchDiscountEligible(plan: SubscriptionPlanDefinition): Boolean {
        return plan.preLaunchDiscount.isEligible(_isNewUser.value)
    }

    /**
     * Get discount breakdown for a plan
     */
    fun getDiscountBreakdown(plan: SubscriptionPlanDefinition, currency: String = "INR"): List<com.ampairs.subscription.domain.model.DiscountItem> {
        return plan.getDiscountBreakdown(currency, _userWorkspaceCount.value, _isNewUser.value)
    }

    /**
     * Check if should show seasonal banner
     */
    fun shouldShowSeasonalBanner(): Boolean {
        return activeSeasonalDiscount.value != null
    }

    /**
     * Check if should show pre-launch banner
     */
    fun shouldShowPreLaunchBanner(): Boolean {
        return activePreLaunchDiscount.value != null &&
                activePreLaunchDiscount.value!!.isEligible(_isNewUser.value)
    }

    // ======================
    // Payment History & Methods
    // ======================

    /**
     * Load payment history with pagination
     */
    fun loadPaymentHistory(page: Int = 0, refresh: Boolean = false) {
        if (refresh) {
            currentPaymentPage = 0
            _paymentHistory.value = emptyList()
            _hasMorePayments.value = true
        }

        viewModelScope.launch {
            if (_isLoadingPayments.value) return@launch

            _isLoadingPayments.value = true
            try {
                repository.getPaymentHistory(page).fold(
                    onSuccess = { pageResponse ->
                        val newTransactions = pageResponse.content
                        _paymentHistory.value = if (refresh) {
                            newTransactions
                        } else {
                            _paymentHistory.value + newTransactions
                        }
                        _hasMorePayments.value = !pageResponse.last
                        currentPaymentPage = page
                    },
                    onFailure = { error ->
                        _events.emit(SubscriptionEvent.Error(
                            error.message ?: "Failed to load payment history"
                        ))
                    }
                )
            } finally {
                _isLoadingPayments.value = false
            }
        }
    }

    /**
     * Load next page of payment history
     */
    fun loadMorePayments() {
        if (_hasMorePayments.value && !_isLoadingPayments.value) {
            loadPaymentHistory(currentPaymentPage + 1, refresh = false)
        }
    }

    /**
     * Refresh payment history
     */
    fun refreshPaymentHistory() {
        loadPaymentHistory(page = 0, refresh = true)
    }

    /**
     * Load payment methods
     */
    fun loadPaymentMethods() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getPaymentMethods().fold(
                    onSuccess = { methods ->
                        _paymentMethods.value = methods
                        _defaultPaymentMethod.value = methods.firstOrNull { it.isDefault }
                    },
                    onFailure = { error ->
                        _events.emit(SubscriptionEvent.Error(
                            error.message ?: "Failed to load payment methods"
                        ))
                    }
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Set default payment method
     */
    fun setDefaultPaymentMethod(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.setDefaultPaymentMethod(uid).fold(
                    onSuccess = { updatedMethod ->
                        // Update local state
                        _paymentMethods.value = _paymentMethods.value.map { method ->
                            method.copy(isDefault = method.uid == uid)
                        }
                        _defaultPaymentMethod.value = updatedMethod
                        _events.emit(SubscriptionEvent.PaymentMethodUpdated)
                    },
                    onFailure = { error ->
                        _events.emit(SubscriptionEvent.Error(
                            error.message ?: "Failed to set default payment method"
                        ))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Remove payment method
     */
    fun removePaymentMethod(uid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            try {
                repository.removePaymentMethod(uid).fold(
                    onSuccess = {
                        // Remove from local state
                        _paymentMethods.value = _paymentMethods.value.filter { it.uid != uid }
                        if (_defaultPaymentMethod.value?.uid == uid) {
                            _defaultPaymentMethod.value = _paymentMethods.value.firstOrNull { it.isDefault }
                        }
                        _events.emit(SubscriptionEvent.PaymentMethodRemoved(uid))
                    },
                    onFailure = { error ->
                        _events.emit(SubscriptionEvent.Error(
                            error.message ?: "Failed to remove payment method"
                        ))
                    }
                )
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
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
    data object PaymentMethodUpdated : SubscriptionEvent()
    data class PaymentMethodRemoved(val uid: String) : SubscriptionEvent()
    data class Error(val message: String) : SubscriptionEvent()
}
