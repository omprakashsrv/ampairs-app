package com.ampairs.auth.domain

/**
 * Result wrapper for Firebase authentication operations
 */
sealed class FirebaseAuthResult<out T> {
    data class Success<T>(val data: T) : FirebaseAuthResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : FirebaseAuthResult<Nothing>()
    data object Loading : FirebaseAuthResult<Nothing>()
}

/**
 * Phone verification state during Firebase auth flow
 */
sealed class PhoneVerificationState {
    data object Idle : PhoneVerificationState()
    data object CodeSent : PhoneVerificationState()
    data class VerificationCompleted(val userId: String) : PhoneVerificationState()
    data class VerificationFailed(val message: String) : PhoneVerificationState()
}
