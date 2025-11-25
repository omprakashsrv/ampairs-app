package com.ampairs.subscription.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO for subscription operations
 */
@Dao
interface SubscriptionDao {

    // ======================
    // Subscription Operations
    // ======================

    @Query("SELECT * FROM subscriptions WHERE workspace_id = :workspaceId LIMIT 1")
    fun observeSubscription(workspaceId: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE workspace_id = :workspaceId LIMIT 1")
    suspend fun getSubscription(workspaceId: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Update
    suspend fun updateSubscription(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE workspace_id = :workspaceId")
    suspend fun deleteSubscription(workspaceId: String)

    @Query("DELETE FROM subscriptions")
    suspend fun clearAllSubscriptions()

    // ======================
    // Plan Operations
    // ======================

    @Query("SELECT * FROM subscription_plans ORDER BY display_order ASC")
    fun observeAllPlans(): Flow<List<SubscriptionPlanEntity>>

    @Query("SELECT * FROM subscription_plans ORDER BY display_order ASC")
    suspend fun getAllPlans(): List<SubscriptionPlanEntity>

    @Query("SELECT * FROM subscription_plans WHERE plan_code = :planCode LIMIT 1")
    suspend fun getPlanByCode(planCode: String): SubscriptionPlanEntity?

    @Query("SELECT * FROM subscription_plans WHERE uid = :uid LIMIT 1")
    suspend fun getPlanByUid(uid: String): SubscriptionPlanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: SubscriptionPlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlans(plans: List<SubscriptionPlanEntity>)

    @Query("DELETE FROM subscription_plans")
    suspend fun clearAllPlans()

    // ======================
    // Device Operations
    // ======================

    @Query("SELECT * FROM device_registrations WHERE workspace_id = :workspaceId AND is_active = 1")
    fun observeDevices(workspaceId: String): Flow<List<DeviceRegistrationEntity>>

    @Query("SELECT * FROM device_registrations WHERE workspace_id = :workspaceId AND is_active = 1")
    suspend fun getDevices(workspaceId: String): List<DeviceRegistrationEntity>

    @Query("SELECT * FROM device_registrations WHERE device_id = :deviceId AND workspace_id = :workspaceId LIMIT 1")
    suspend fun getDeviceByDeviceId(deviceId: String, workspaceId: String): DeviceRegistrationEntity?

    @Query("SELECT * FROM device_registrations WHERE uid = :uid LIMIT 1")
    suspend fun getDeviceByUid(uid: String): DeviceRegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: DeviceRegistrationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<DeviceRegistrationEntity>)

    @Update
    suspend fun updateDevice(device: DeviceRegistrationEntity)

    @Query("UPDATE device_registrations SET is_active = 0 WHERE uid = :uid")
    suspend fun deactivateDevice(uid: String)

    @Query("DELETE FROM device_registrations WHERE workspace_id = :workspaceId")
    suspend fun clearDevices(workspaceId: String)

    @Query("SELECT COUNT(*) FROM device_registrations WHERE workspace_id = :workspaceId AND is_active = 1")
    suspend fun getActiveDeviceCount(workspaceId: String): Int

    // ======================
    // Usage Metrics Operations
    // ======================

    @Query("""
        SELECT * FROM usage_metrics
        WHERE workspace_id = :workspaceId
        AND period_year = :year AND period_month = :month
        LIMIT 1
    """)
    fun observeUsage(workspaceId: String, year: Int, month: Int): Flow<UsageMetricsEntity?>

    @Query("""
        SELECT * FROM usage_metrics
        WHERE workspace_id = :workspaceId
        AND period_year = :year AND period_month = :month
        LIMIT 1
    """)
    suspend fun getUsage(workspaceId: String, year: Int, month: Int): UsageMetricsEntity?

    @Query("""
        SELECT * FROM usage_metrics
        WHERE workspace_id = :workspaceId
        ORDER BY period_year DESC, period_month DESC
        LIMIT 1
    """)
    suspend fun getLatestUsage(workspaceId: String): UsageMetricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: UsageMetricsEntity)

    @Query("DELETE FROM usage_metrics WHERE workspace_id = :workspaceId")
    suspend fun clearUsage(workspaceId: String)

    // ======================
    // Bulk Operations
    // ======================

    @Query("DELETE FROM subscriptions WHERE workspace_id = :workspaceId")
    suspend fun clearWorkspaceData(workspaceId: String)
}
