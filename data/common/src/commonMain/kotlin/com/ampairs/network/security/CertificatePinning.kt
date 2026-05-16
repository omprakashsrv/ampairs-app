package com.ampairs.network.security

import kotlinx.datetime.LocalDateTime

/**
 * Certificate information for pinning
 */
data class CertificateInfo(
    val hostname: String,
    val pins: List<String>, // SHA-256 hashes of certificate public keys
    val expirationDate: LocalDateTime,
    val isExpired: Boolean = false
)

/**
 * Result of certificate validation
 */
sealed class CertificateValidationResult {
    object Valid : CertificateValidationResult()
    object Expired : CertificateValidationResult()
    object InvalidPin : CertificateValidationResult()
    data class NetworkError(val message: String) : CertificateValidationResult()
}

/**
 * App update requirement status
 */
sealed class AppUpdateStatus {
    object NotRequired : AppUpdateStatus()
    object Required : AppUpdateStatus()
    data class Recommended(val reason: String) : AppUpdateStatus()
}

/**
 * Certificate pinning service interface
 */
interface CertificatePinningService {
    /**
     * Initialize certificate pinning with current certificate information
     */
    suspend fun initialize()
    
    /**
     * Validate certificate for the given hostname
     */
    suspend fun validateCertificate(hostname: String): CertificateValidationResult
    
    /**
     * Check if certificates are expired and app update is required
     */
    suspend fun checkAppUpdateRequired(): AppUpdateStatus
    
    /**
     * Force update certificate information (when app is updated)
     */
    suspend fun updateCertificates()
    
    /**
     * Get current certificate information
     */
    suspend fun getCertificateInfo(hostname: String): CertificateInfo?
}

/**
 * App update enforcement interface
 */
interface AppUpdateEnforcer {
    /**
     * Show app update dialog to user
     */
    suspend fun showUpdateDialog(status: AppUpdateStatus)
    
    /**
     * Force app to close/redirect to update
     */
    suspend fun enforceUpdate()
    
    /**
     * Check if app should allow network requests based on certificate status
     */
    suspend fun shouldAllowNetworkRequests(): Boolean
}