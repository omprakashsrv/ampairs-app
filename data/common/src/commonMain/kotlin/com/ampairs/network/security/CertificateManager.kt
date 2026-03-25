package com.ampairs.network.security

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Serializable
data class StoredCertificateInfo(
    val hostname: String,
    val pins: List<String>,
    val expirationDate: String, // ISO string format
    val lastUpdated: String
)

/**
 * Certificate manager for storing and retrieving certificate information
 */
class CertificateManager(
    private val storage: CertificateStorage
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        const val CERTIFICATE_STORAGE_KEY = "ampairs_certificate_pins"
        const val BACKEND_HOSTNAME = "localhost:8080" // Configure based on environment
        
        // Backend certificate pins (SHA-256 hashes) - these should be updated with actual pins
        val DEFAULT_CERTIFICATE_PINS = listOf(
            "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", // Primary certificate pin
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB="  // Backup certificate pin
        )
    }
    
    /**
     * Initialize with default certificate information
     */
    suspend fun initialize() {
        val stored = storage.getString(CERTIFICATE_STORAGE_KEY)
        if (stored == null) {
            // Store default certificate information
            val defaultCertInfo = CertificateInfo(
                hostname = BACKEND_HOSTNAME,
                pins = DEFAULT_CERTIFICATE_PINS,
                expirationDate = LocalDateTime(2025, 9, 1, 0, 0, 0) // Default 1 month validity from August 2025
            )
            storeCertificateInfo(defaultCertInfo)
        }
    }
    
    /**
     * Store certificate information
     */
    suspend fun storeCertificateInfo(certInfo: CertificateInfo) {
        val storedInfo = StoredCertificateInfo(
            hostname = certInfo.hostname,
            pins = certInfo.pins,
            expirationDate = certInfo.expirationDate.toString(),
            lastUpdated = LocalDateTime(2025, 8, 5, 12, 0, 0).toString() // Static timestamp for now
        )
        
        val jsonString = json.encodeToString(storedInfo)
        storage.putString(CERTIFICATE_STORAGE_KEY, jsonString)
    }
    
    /**
     * Get certificate information for hostname
     */
    suspend fun getCertificateInfo(hostname: String): CertificateInfo? {
        val stored = storage.getString(CERTIFICATE_STORAGE_KEY) ?: return null
        
        return try {
            val storedInfo = json.decodeFromString<StoredCertificateInfo>(stored)
            if (storedInfo.hostname == hostname) {
                val expirationDate = LocalDateTime.parse(storedInfo.expirationDate)
                val currentTime = LocalDateTime(2025, 8, 5, 12, 0, 0) // Static current time for now
                
                CertificateInfo(
                    hostname = storedInfo.hostname,
                    pins = storedInfo.pins,
                    expirationDate = expirationDate,
                    isExpired = currentTime > expirationDate
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Check if certificates are expired
     */
    suspend fun areCertificatesExpired(): Boolean {
        val certInfo = getCertificateInfo(BACKEND_HOSTNAME)
        return certInfo?.isExpired ?: true
    }
    
    /**
     * Update certificate information (called when app is updated)
     */
    suspend fun updateCertificates(newPins: List<String>, newExpirationDate: LocalDateTime) {
        val updatedCertInfo = CertificateInfo(
            hostname = BACKEND_HOSTNAME,
            pins = newPins,
            expirationDate = newExpirationDate
        )
        storeCertificateInfo(updatedCertInfo)
    }
    
    /**
     * Clear all stored certificate information
     */
    suspend fun clearCertificates() {
        storage.remove(CERTIFICATE_STORAGE_KEY)
    }
}

/**
 * Platform-specific storage interface for certificate information
 */
interface CertificateStorage {
    suspend fun getString(key: String): String?
    suspend fun putString(key: String, value: String)
    suspend fun remove(key: String)
}