package com.ampairs.auth.firebase

import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-specific Firebase authentication provider
 *
 * Android & iOS: Phone number authentication with SMS OTP
 * Desktop: QR code authentication (to be implemented)
 */
expect class FirebaseAuthProvider {

    /**
     * Observable state of phone verification process
     */
    val verificationState: StateFlow<PhoneVerificationState>

    /**
     * Initialize Firebase (if needed on platform)
     */
    suspend fun initialize(): FirebaseAuthResult<Unit>

    /**
     * Send verification code to phone number
     * @param phoneNumber Phone number with country code (e.g., "+919876543210")
     * @return Result with verification ID or error
     */
    suspend fun sendVerificationCode(phoneNumber: String): FirebaseAuthResult<String>

    /**
     * Verify the OTP code sent to phone
     * @param verificationId The verification ID returned from sendVerificationCode
     * @param code The OTP code entered by user
     * @return Result with user ID or error
     */
    suspend fun verifyCode(verificationId: String, code: String): FirebaseAuthResult<String>

    /**
     * Resend verification code to the same phone number
     * @param phoneNumber Phone number with country code
     * @return Result with new verification ID or error
     */
    suspend fun resendVerificationCode(phoneNumber: String): FirebaseAuthResult<String>

    /**
     * Get current authenticated user ID (if any)
     * @return User ID if authenticated, null otherwise
     */
    suspend fun getCurrentUserId(): String?

    /**
     * Sign out current user
     */
    suspend fun signOut(): FirebaseAuthResult<Unit>

    /**
     * Check if Firebase Auth is supported on this platform
     */
    fun isSupported(): Boolean
}
