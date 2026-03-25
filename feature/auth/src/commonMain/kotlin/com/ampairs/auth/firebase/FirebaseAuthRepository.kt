package com.ampairs.auth.firebase

import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository for managing Firebase authentication
 *
 * Provides a higher-level API over FirebaseAuthProvider
 * Handles country code formatting and error handling
 */
class FirebaseAuthRepository(
    private val firebaseAuthProvider: FirebaseAuthProvider
) {

    /**
     * Observable verification state
     */
    val verificationState: StateFlow<PhoneVerificationState> = firebaseAuthProvider.verificationState

    /**
     * Last verified phone number (without country code)
     * Stored when OTP is sent successfully
     */
    private var lastPhoneNumber: String = ""

    /**
     * Get the last phone number used for verification
     */
    fun getLastPhoneNumber(): String = lastPhoneNumber

    /**
     * Check if Firebase Auth is supported on current platform
     */
    fun isSupported(): Boolean = firebaseAuthProvider.isSupported()

    /**
     * Initialize Firebase authentication
     */
    suspend fun initialize(): FirebaseAuthResult<Unit> {
        return firebaseAuthProvider.initialize()
    }

    /**
     * Send OTP to phone number
     * @param countryCode Country code without + (e.g., "91" for India)
     * @param phoneNumber Phone number without country code
     * @return Verification ID for later use in verifyOtp
     */
    suspend fun sendOtp(countryCode: String, phoneNumber: String): FirebaseAuthResult<String> {
        if (!isSupported()) {
            return FirebaseAuthResult.Error("Firebase authentication not supported on this platform")
        }

        // Format phone number with country code
        val fullPhoneNumber = "+$countryCode$phoneNumber"

        // Validate phone number format
        if (!isValidPhoneNumber(fullPhoneNumber)) {
            return FirebaseAuthResult.Error("Invalid phone number format")
        }

        val result = firebaseAuthProvider.sendVerificationCode(fullPhoneNumber)

        // Store phone number if OTP sent successfully
        if (result is FirebaseAuthResult.Success) {
            lastPhoneNumber = phoneNumber
        }

        return result
    }

    /**
     * Verify OTP code
     * @param verificationId ID returned from sendOtp
     * @param code OTP code entered by user
     * @return User ID if verification successful
     */
    suspend fun verifyOtp(verificationId: String, code: String): FirebaseAuthResult<String> {
        if (verificationId.isBlank()) {
            return FirebaseAuthResult.Error("Invalid verification ID")
        }

        if (code.isBlank() || code.length < 6) {
            return FirebaseAuthResult.Error("Invalid OTP code")
        }

        return firebaseAuthProvider.verifyCode(verificationId, code)
    }

    /**
     * Resend OTP to phone number
     */
    suspend fun resendOtp(countryCode: String, phoneNumber: String): FirebaseAuthResult<String> {
        if (!isSupported()) {
            return FirebaseAuthResult.Error("Firebase authentication not supported on this platform")
        }

        val fullPhoneNumber = "+$countryCode$phoneNumber"
        return firebaseAuthProvider.resendVerificationCode(fullPhoneNumber)
    }

    /**
     * Get current authenticated Firebase user ID
     */
    suspend fun getCurrentUserId(): String? {
        return firebaseAuthProvider.getCurrentUserId()
    }

    /**
     * Sign out current user
     */
    suspend fun signOut(): FirebaseAuthResult<Unit> {
        return firebaseAuthProvider.signOut()
    }

    /**
     * Validate phone number format
     */
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Basic validation: starts with +, followed by digits, length between 10-15
        val regex = Regex("^\\+\\d{10,15}$")
        return regex.matches(phoneNumber)
    }
}
