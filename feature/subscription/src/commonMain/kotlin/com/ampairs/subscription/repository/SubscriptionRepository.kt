@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.ampairs.subscription.repository

import com.ampairs.common.model.PageResponse
import com.ampairs.subscription.api.SubscriptionApi
import com.ampairs.subscription.db.*
import com.ampairs.subscription.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/**
 * Repository for subscription data with offline-first pattern.
 *
 * Key principles:
 * 1. Database-first operations - all reads come from local DB
 * 2. Background sync - server operations happen asynchronously
 * 3. Graceful fallback - continues working if server unavailable
 */
class SubscriptionRepository(
    private val api: SubscriptionApi,
    private val dao: SubscriptionDao
) {

    // ======================
    // Subscription Operations
    // ======================

    /**
     * Observe subscription state (reactive, from local DB)
     */
    fun observeSubscription(workspaceId: String): Flow<SubscriptionState?> {
        return dao.observeSubscription(workspaceId).map { it?.toDomain() }
    }

    /**
     * Get current subscription (from local DB)
     */
    suspend fun getSubscription(workspaceId: String): SubscriptionState? {
        return dao.getSubscription(workspaceId)?.toDomain()
    }

    /**
     * Update subscription locally (for optimistic updates)
     */
    suspend fun updateSubscriptionLocally(subscription: SubscriptionState) {
        dao.insertSubscription(subscription.toEntity())
    }

    /**
     * Sync subscription from server
     */
    suspend fun syncSubscription(workspaceId: String): Result<SubscriptionState> {
        return try {
            val response = api.getCurrentSubscription()
            val subscription = response.toModel()
            dao.insertSubscription(subscription.toEntity())
            Result.success(subscription)
        } catch (e: Exception) {
            // Return cached data if available
            val cached = dao.getSubscription(workspaceId)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Get or sync subscription - returns cached if available, otherwise fetches
     */
    suspend fun getOrSyncSubscription(workspaceId: String): SubscriptionState {
        return dao.getSubscription(workspaceId)?.toDomain()
            ?: syncSubscription(workspaceId).getOrNull()
            ?: SubscriptionState.createFree(workspaceId)
    }

    // ======================
    // Plan Operations
    // ======================

    /**
     * Observe all plans (reactive, from local DB)
     */
    fun observePlans(): Flow<List<SubscriptionPlanDefinition>> {
        return dao.observeAllPlans().map { plans -> plans.map { it.toDomain() } }
    }

    /**
     * Get all plans from local cache
     */
    suspend fun getPlans(): List<SubscriptionPlanDefinition> {
        return dao.getAllPlans().map { it.toDomain() }
    }

    /**
     * Get plan by code
     */
    suspend fun getPlanByCode(planCode: String): SubscriptionPlanDefinition? {
        return dao.getPlanByCode(planCode)?.toDomain()
    }

    /**
     * Sync plans from server with deactivation handling
     *
     * This method:
     * 1. Fetches active plans from the server
     * 2. Inserts/updates them in the local database (marked as active)
     * 3. Deactivates any cached plans that are no longer in the server response
     */
    suspend fun syncPlans(): Result<List<SubscriptionPlanDefinition>> {
        return try {
            // Fetch active plans from server
            val serverPlans = api.getPlans().map { it.toModel() }
            val serverPlanUids = serverPlans.map { it.uid }.toSet()

            // Get all plans from local cache (including inactive ones)
            val cachedPlans = dao.getAllPlansIncludingInactive()

            // Find plans that exist in cache but not in server response (deactivated plans)
            val deactivatedPlanUids = cachedPlans
                .filter { it.is_active && it.uid !in serverPlanUids }
                .map { it.uid }

            // Insert/update active plans from server (marked as active)
            dao.insertPlans(serverPlans.map { it.toEntity() })

            // Mark deactivated plans as inactive (don't delete them in case user is subscribed)
            if (deactivatedPlanUids.isNotEmpty()) {
                dao.deactivatePlans(deactivatedPlanUids)
            }

            Result.success(serverPlans)
        } catch (e: Exception) {
            // Return cached active plans if available
            val cached = dao.getAllPlans().map { it.toDomain() }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    // ======================
    // Purchase Operations
    // ======================

    /**
     * Initiate purchase (for desktop - returns checkout URL)
     */
    suspend fun initiatePurchase(
        request: InitiatePurchaseRequest,
        provider: PaymentProvider
    ): Result<InitiatePurchaseResponse> {
        return try {
            val response = api.initiatePurchase(request, provider)
            Result.success(response.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Verify in-app purchase (for mobile)
     */
    suspend fun verifyPurchase(
        request: VerifyPurchaseRequest,
        workspaceId: String
    ): Result<SubscriptionState> {
        return try {
            val response = api.verifyPurchase(request)
            val subscription = response.toModel()
            dao.insertSubscription(subscription.toEntity())
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Change subscription plan
     */
    suspend fun changePlan(
        request: ChangePlanRequest,
        workspaceId: String
    ): Result<ChangePlanResponse> {
        return try {
            val response = api.changePlan(request)
            val result = response.toModel()
            dao.insertSubscription(result.subscription.toEntity())
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cancel subscription
     */
    suspend fun cancelSubscription(
        request: CancelSubscriptionRequest,
        workspaceId: String
    ): Result<SubscriptionState> {
        return try {
            val response = api.cancelSubscription(request)
            val subscription = response.toModel()
            dao.insertSubscription(subscription.toEntity())
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Pause subscription
     */
    suspend fun pauseSubscription(
        request: PauseSubscriptionRequest,
        workspaceId: String
    ): Result<SubscriptionState> {
        return try {
            val response = api.pauseSubscription(request)
            val subscription = response.toModel()
            dao.insertSubscription(subscription.toEntity())
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume subscription
     */
    suspend fun resumeSubscription(workspaceId: String): Result<SubscriptionState> {
        return try {
            val response = api.resumeSubscription()
            val subscription = response.toModel()
            dao.insertSubscription(subscription.toEntity())
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Start trial
     */
    suspend fun startTrial(
        planCode: String,
        trialDays: Int,
        workspaceId: String
    ): Result<SubscriptionState> {
        return try {
            val response = api.startTrial(planCode, trialDays)
            val subscription = response.toModel()
            dao.insertSubscription(subscription.toEntity())
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ======================
    // Usage Operations
    // ======================

    /**
     * Observe current usage
     */
    fun observeUsage(workspaceId: String): Flow<UsageMetrics?> {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return dao.observeUsage(workspaceId, now.year, now.monthNumber).map { it?.toDomain() }
    }

    /**
     * Get current usage from cache
     */
    suspend fun getUsage(workspaceId: String): UsageMetrics? {
        return dao.getLatestUsage(workspaceId)?.toDomain()
    }

    /**
     * Sync usage from server
     */
    suspend fun syncUsage(workspaceId: String): Result<UsageMetrics> {
        return try {
            val response = api.getUsage()
            val usage = response.toModel()
            dao.insertUsage(usage.toEntity())
            Result.success(usage)
        } catch (e: Exception) {
            val cached = dao.getLatestUsage(workspaceId)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Check resource limit
     */
    suspend fun checkLimit(
        resourceType: String,
        currentCount: Int
    ): Result<LimitCheckResult> {
        return try {
            val response = api.checkLimit(resourceType, currentCount)
            Result.success(response.toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check feature availability
     */
    suspend fun hasFeature(feature: String): Boolean {
        return try {
            api.hasFeature(feature)
        } catch (e: Exception) {
            false
        }
    }

    // ======================
    // Device Operations
    // ======================

    /**
     * Observe devices for workspace
     */
    fun observeDevices(workspaceId: String): Flow<List<DeviceRegistration>> {
        return dao.observeDevices(workspaceId).map { devices -> devices.map { it.toDomain() } }
    }

    /**
     * Get devices from cache
     */
    suspend fun getDevices(workspaceId: String): List<DeviceRegistration> {
        return dao.getDevices(workspaceId).map { it.toDomain() }
    }

    /**
     * Register device
     */
    suspend fun registerDevice(
        request: RegisterDeviceRequest,
        userId: String,
        workspaceId: String
    ): Result<DeviceRegistration> {
        return try {
            val response = api.registerDevice(request, userId)
            val device = response.toModel()
            dao.insertDevice(device.toEntity(workspaceId))
            Result.success(device)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh device token
     */
    suspend fun refreshDeviceToken(
        request: RefreshDeviceTokenRequest,
        workspaceId: String
    ): Result<DeviceRegistration> {
        return try {
            val response = api.refreshDeviceToken(request)
            val device = response.toModel()
            dao.insertDevice(device.toEntity(workspaceId))
            Result.success(device)
        } catch (e: Exception) {
            // Return cached device if available
            val cached = dao.getDeviceByDeviceId(request.deviceId, workspaceId)?.toDomain()
            if (cached != null) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    /**
     * Get device access mode
     */
    suspend fun getDeviceAccessMode(
        deviceId: String,
        workspaceId: String
    ): SubscriptionAccessMode {
        // First check local device registration
        val localDevice = dao.getDeviceByDeviceId(deviceId, workspaceId)?.toDomain()
        if (localDevice != null) {
            val calculatedMode = localDevice.calculateAccessMode()
            // If token still valid, return local result
            if (calculatedMode == SubscriptionAccessMode.FULL_ACCESS ||
                calculatedMode == SubscriptionAccessMode.OFFLINE_GRACE) {
                return calculatedMode
            }
        }

        // Try to fetch from server
        return try {
            api.getDeviceAccessMode(deviceId)
        } catch (e: Exception) {
            localDevice?.calculateAccessMode() ?: SubscriptionAccessMode.LOCKED
        }
    }

    /**
     * Deactivate device
     */
    suspend fun deactivateDevice(deviceUid: String, reason: String? = null): Result<Unit> {
        return try {
            api.deactivateDevice(deviceUid, reason)
            dao.deactivateDevice(deviceUid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sync devices from server
     */
    suspend fun syncDevices(workspaceId: String): Result<List<DeviceRegistration>> {
        return try {
            val devices = api.getDevices().map { it.toModel() }
            dao.clearDevices(workspaceId)
            dao.insertDevices(devices.map { it.toEntity(workspaceId) })
            Result.success(devices)
        } catch (e: Exception) {
            val cached = dao.getDevices(workspaceId).map { it.toDomain() }
            if (cached.isNotEmpty()) {
                Result.success(cached)
            } else {
                Result.failure(e)
            }
        }
    }

    // ======================
    // Payment History & Methods
    // ======================

    /**
     * Get payment history with pagination
     */
    suspend fun getPaymentHistory(page: Int = 0, size: Int = 20): Result<PageResponse<PaymentTransaction>> {
        return try {
            val response = api.getPaymentHistory(page, size)
            // Map DTOs to domain models
            val pageResponse = PageResponse(
                content = response.content.map { it.toModel() },
                pageNumber = response.pageNumber,
                pageSize = response.pageSize,
                totalElements = response.totalElements,
                totalPages = response.totalPages,
                hasNext = response.hasNext,
                hasPrevious = response.hasPrevious,
                first = response.first,
                last = response.last
            )
            Result.success(pageResponse)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all payment methods
     */
    suspend fun getPaymentMethods(): Result<List<PaymentMethod>> {
        return try {
            val response = api.getPaymentMethods()
            val methods = response.map { it.toModel() }
            Result.success(methods)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get default payment method
     */
    suspend fun getDefaultPaymentMethod(): Result<PaymentMethod?> {
        return try {
            val response = api.getDefaultPaymentMethod()
            val method = response?.toModel()
            Result.success(method)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set default payment method
     */
    suspend fun setDefaultPaymentMethod(uid: String): Result<PaymentMethod> {
        return try {
            val response = api.setDefaultPaymentMethod(uid)
            val method = response.toModel()
            Result.success(method)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Remove payment method
     */
    suspend fun removePaymentMethod(uid: String): Result<Unit> {
        return try {
            api.removePaymentMethod(uid)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ======================
    // Full Sync
    // ======================

    /**
     * Full sync for offline-first apps
     */
    suspend fun fullSync(
        deviceId: String,
        userId: String,
        workspaceId: String,
        lastSyncAt: String? = null
    ): Result<SyncSubscriptionResponse> {
        return try {
            val request = SyncSubscriptionRequest(
                deviceId = deviceId,
                lastSyncAt = lastSyncAt
            )
            val response = api.syncSubscription(request, userId)
            val result = response.toModel()

            // Update local cache
            dao.insertSubscription(result.subscription.toEntity())
            dao.insertDevice(result.device.toEntity(workspaceId))
            result.usage?.let { dao.insertUsage(it.toEntity()) }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ======================
    // Cache Management
    // ======================

    /**
     * Clear all cached data for a workspace
     */
    suspend fun clearWorkspaceCache(workspaceId: String) {
        dao.deleteSubscription(workspaceId)
        dao.clearDevices(workspaceId)
        dao.clearUsage(workspaceId)
    }

    /**
     * Clear all cached data
     */
    suspend fun clearAllCache() {
        dao.clearAllSubscriptions()
        dao.clearAllPlans()
    }
}
