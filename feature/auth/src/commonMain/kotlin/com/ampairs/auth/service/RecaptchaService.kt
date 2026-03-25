package com.ampairs.auth.service

/**
 * Service for handling Google reCAPTCHA v3 integration
 * Platform-specific implementations handle the actual reCAPTCHA execution
 */
expect class RecaptchaService {
    
    /**
     * Execute reCAPTCHA for login action
     * @param onProgress Callback to update UI with progress messages
     * @return reCAPTCHA token or null if reCAPTCHA is disabled/failed
     */
    suspend fun executeLogin(onProgress: ((String) -> Unit)? = null): String?
    
    /**
     * Execute reCAPTCHA for OTP verification action
     * @param onProgress Callback to update UI with progress messages
     * @return reCAPTCHA token or null if reCAPTCHA is disabled/failed
     */
    suspend fun executeVerifyOtp(onProgress: ((String) -> Unit)? = null): String?
    
    /**
     * Execute reCAPTCHA for resend OTP action
     * @param onProgress Callback to update UI with progress messages
     * @return reCAPTCHA token or null if reCAPTCHA is disabled/failed
     */
    suspend fun executeResendOtp(onProgress: ((String) -> Unit)? = null): String?

    /**
     * Execute reCAPTCHA for Firebase verification action
     * @param onProgress Callback to update UI with progress messages
     * @return reCAPTCHA token or null if reCAPTCHA is disabled/failed
     */
    suspend fun executeFirebaseVerify(onProgress: ((String) -> Unit)? = null): String?

    /**
     * Check if reCAPTCHA is enabled for the current environment
     * @return true if reCAPTCHA is enabled, false otherwise
     */
    fun isEnabled(): Boolean
    
    /**
     * Initialize reCAPTCHA service (load scripts, prepare environment)
     */
    suspend fun initialize()
}

/**
 * Configuration for reCAPTCHA service
 */
data class RecaptchaConfig(
    val siteKey: String,
    val enabled: Boolean = true
)