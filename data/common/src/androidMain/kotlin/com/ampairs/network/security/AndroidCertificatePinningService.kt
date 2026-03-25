package com.ampairs.network.security

import android.content.Context
import androidx.core.content.edit
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.net.ssl.SSLPeerUnverifiedException

class AndroidCertificatePinningService(
    private val context: Context,
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
}

/**
 * Android implementation of certificate storage using SharedPreferences
 */
class AndroidCertificateStorage(private val context: Context) : CertificateStorage {
    private val prefs = context.getSharedPreferences("ampairs_certificates", Context.MODE_PRIVATE)
    
    override suspend fun getString(key: String): String? {
        return prefs.getString(key, null)
    }
    
    override suspend fun putString(key: String, value: String) {
        prefs.edit {
            putString(key, value)
        }
    }
    
    override suspend fun remove(key: String) {
        prefs.edit {
            remove(key)
        }
    }
}