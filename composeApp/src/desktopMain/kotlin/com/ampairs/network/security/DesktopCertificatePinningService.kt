package com.ampairs.network.security

import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.*
import javax.net.ssl.SSLPeerUnverifiedException

class DesktopCertificatePinningService(
    private val certificateManager: CertificateManager
) : CertificatePinningService {
    
    private var okHttpClient: OkHttpClient? = null
    private var appUpdateEnforcer: AppUpdateEnforcer? = null
    
    fun setAppUpdateEnforcer(enforcer: AppUpdateEnforcer) {
        this.appUpdateEnforcer = enforcer
    }
    
    override suspend fun initialize() {
        certificateManager.initialize()
        updateOkHttpClient()
    }
    
    private suspend fun updateOkHttpClient() {
        val certInfo = certificateManager.getCertificateInfo(CertificateManager.BACKEND_HOSTNAME)
        
        if (certInfo != null && !certInfo.isExpired) {
            val certificatePinnerBuilder = CertificatePinner.Builder()
            
            // Add all certificate pins for the hostname
            certInfo.pins.forEach { pin ->
                certificatePinnerBuilder.add(certInfo.hostname, "sha256/$pin")
            }
            
            val certificatePinner = certificatePinnerBuilder.build()
            
            okHttpClient = OkHttpClient.Builder()
                .certificatePinner(certificatePinner)
                .build()
        } else {
            // Certificates are expired or missing - enforce app update
            appUpdateEnforcer?.enforceUpdate()
        }
    }
    
    override suspend fun validateCertificate(hostname: String): CertificateValidationResult {
        val certInfo = certificateManager.getCertificateInfo(hostname)
            ?: return CertificateValidationResult.InvalidPin
        
        if (certInfo.isExpired) {
            return CertificateValidationResult.Expired
        }
        
        return try {
            val client = okHttpClient ?: return CertificateValidationResult.InvalidPin
            
            // Perform a simple HEAD request to validate certificate pinning
            val request = Request.Builder()
                .url("https://$hostname")
                .head()
                .build()
            
            val response = client.newCall(request).execute()
            response.close()
            
            CertificateValidationResult.Valid
        } catch (e: SSLPeerUnverifiedException) {
            CertificateValidationResult.InvalidPin
        } catch (e: IOException) {
            CertificateValidationResult.NetworkError(e.message ?: "Network error")
        } catch (e: Exception) {
            CertificateValidationResult.NetworkError(e.message ?: "Unknown error")
        }
    }
    
    override suspend fun checkAppUpdateRequired(): AppUpdateStatus {
        val areCertificatesExpired = certificateManager.areCertificatesExpired()
        
        return if (areCertificatesExpired) {
            AppUpdateStatus.Required
        } else {
            val certInfo = certificateManager.getCertificateInfo(CertificateManager.BACKEND_HOSTNAME)
            if (certInfo != null) {
                // Simple check - if certificate is not expired, it's likely valid for now
                // More sophisticated date arithmetic can be added later
                AppUpdateStatus.NotRequired
            } else {
                AppUpdateStatus.Required
            }
        }
    }
    
    override suspend fun updateCertificates() {
        // This would typically fetch new certificate information from a secure endpoint
        // For now, we'll reinitialize with default values
        certificateManager.clearCertificates()
        certificateManager.initialize()
        updateOkHttpClient()
    }
    
    override suspend fun getCertificateInfo(hostname: String): CertificateInfo? {
        return certificateManager.getCertificateInfo(hostname)
    }
    
    /**
     * Get configured OkHttpClient with certificate pinning
     */
    fun getOkHttpClient(): OkHttpClient? = okHttpClient
    
    /**
     * Extract certificate pins from a certificate file (for development/testing)
     */
    fun extractCertificatePin(certificateFile: File): String? {
        return try {
            val certificateFactory = CertificateFactory.getInstance("X.509")
            val certificate = FileInputStream(certificateFile).use { fis ->
                certificateFactory.generateCertificate(fis) as X509Certificate
            }
            
            val publicKeyBytes = certificate.publicKey.encoded
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(publicKeyBytes)
            Base64.getEncoder().encodeToString(hash)
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Desktop implementation of certificate storage using file system
 */
class DesktopCertificateStorage : CertificateStorage {
    private val storageDir = File(System.getProperty("user.home"), ".ampairs")
    private val certificateFile = File(storageDir, "certificates.properties")
    
    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }
    
    override suspend fun getString(key: String): String? {
        return try {
            if (!certificateFile.exists()) return null
            
            val properties = Properties()
            FileInputStream(certificateFile).use { fis ->
                properties.load(fis)
            }
            properties.getProperty(key)
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun putString(key: String, value: String) {
        try {
            val properties = Properties()
            
            // Load existing properties if file exists
            if (certificateFile.exists()) {
                FileInputStream(certificateFile).use { fis ->
                    properties.load(fis)
                }
            }
            
            // Update the property
            properties.setProperty(key, value)
            
            // Save back to file
            FileOutputStream(certificateFile).use { fos ->
                properties.store(fos, "Ampairs Certificate Storage")
            }
        } catch (e: Exception) {
            // Handle storage error
        }
    }
    
    override suspend fun remove(key: String) {
        try {
            if (!certificateFile.exists()) return
            
            val properties = Properties()
            FileInputStream(certificateFile).use { fis ->
                properties.load(fis)
            }
            
            properties.remove(key)
            
            FileOutputStream(certificateFile).use { fos ->
                properties.store(fos, "Ampairs Certificate Storage")
            }
        } catch (e: Exception) {
            // Handle removal error
        }
    }
}