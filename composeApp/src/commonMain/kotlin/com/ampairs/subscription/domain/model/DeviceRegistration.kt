@file:OptIn(ExperimentalTime::class)

package com.ampairs.subscription.domain.model

import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.periodUntil
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Device registration for offline subscription enforcement.
 * Tracks registered devices with token-based expiry.
 */
@Serializable
data class DeviceRegistration(
    val uid: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    val platform: DevicePlatform,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("os_version")
    val osVersion: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    @SerialName("token_expires_at")
    val tokenExpiresAt: String,
    @SerialName("last_sync_at")
    val lastSyncAt: String? = null,
    @SerialName("last_activity_at")
    val lastActivityAt: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("access_mode")
    val accessMode: SubscriptionAccessMode = SubscriptionAccessMode.FULL_ACCESS,
    @SerialName("created_at")
    val createdAt: String? = null
) {
    companion object {
        /** Token validity period in days */
        const val TOKEN_VALIDITY_DAYS = 7L

        /** Grace period after token expiry in days */
        const val GRACE_PERIOD_DAYS = 3L

        /** Maximum offline days before lock */
        const val MAX_OFFLINE_DAYS = 14L
    }

    /**
     * Check if token is valid
     */
    fun isTokenValid(): Boolean {
        if (!isActive) return false
        return try {
            val expiresAt = Instant.parse(tokenExpiresAt)
            Clock.System.now() < expiresAt
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device is in grace period
     */
    fun isInGracePeriod(): Boolean {
        if (!isActive) return false
        return try {
            val expiresAt = Instant.parse(tokenExpiresAt)
            val now = Clock.System.now()
            val graceEnd = expiresAt.plus(DateTimePeriod(days = GRACE_PERIOD_DAYS.toInt()), TimeZone.UTC)
            now > expiresAt && now < graceEnd
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device should be locked
     */
    fun shouldBeLocked(): Boolean {
        if (!isActive) return true
        return try {
            val expiresAt = Instant.parse(tokenExpiresAt)
            val now = Clock.System.now()
            val lockTime = expiresAt.plus(DateTimePeriod(days = MAX_OFFLINE_DAYS.toInt()), TimeZone.UTC)
            now > lockTime
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Determine access mode based on token status
     */
    fun calculateAccessMode(): SubscriptionAccessMode {
        return when {
            !isActive -> SubscriptionAccessMode.LOCKED
            isTokenValid() -> SubscriptionAccessMode.FULL_ACCESS
            isInGracePeriod() -> SubscriptionAccessMode.OFFLINE_GRACE
            shouldBeLocked() -> SubscriptionAccessMode.LOCKED
            else -> SubscriptionAccessMode.READ_ONLY
        }
    }

    /**
     * Get days until token expires (negative if already expired)
     */
    fun getDaysUntilExpiry(): Long {
        return try {
            val expiresAt = Instant.parse(tokenExpiresAt)
            val now = Clock.System.now()
            val period = now.periodUntil(expiresAt, TimeZone.UTC)
            period.days.toLong()
        } catch (e: Exception) {
            0L
        }
    }
}

/**
 * Result of device access check
 */
sealed class DeviceAccessResult {
    /** Device is allowed full access */
    data object Allowed : DeviceAccessResult()

    /** Device is in grace period - show warning but allow access */
    data class GracePeriod(val daysRemaining: Long) : DeviceAccessResult()

    /** Device token expired - must sync to continue */
    data object ExpiredMustSync : DeviceAccessResult()

    /** Device is locked - must go online */
    data object Locked : DeviceAccessResult()

    /** Device not registered */
    data object NotRegistered : DeviceAccessResult()

    /** Device limit exceeded - cannot register new device */
    data class LimitExceeded(
        val currentCount: Int,
        val maxAllowed: Int,
        val registeredDevices: List<DeviceRegistration>
    ) : DeviceAccessResult()
}

/**
 * Request to register a device
 */
@Serializable
data class RegisterDeviceRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    val platform: DevicePlatform,
    @SerialName("device_model")
    val deviceModel: String? = null,
    @SerialName("os_version")
    val osVersion: String? = null,
    @SerialName("app_version")
    val appVersion: String? = null,
    @SerialName("push_token")
    val pushToken: String? = null,
    @SerialName("push_token_type")
    val pushTokenType: String? = null
)

/**
 * Request to refresh device token
 */
@Serializable
data class RefreshDeviceTokenRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("app_version")
    val appVersion: String? = null
)
