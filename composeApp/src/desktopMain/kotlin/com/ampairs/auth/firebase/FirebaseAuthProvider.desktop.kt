package com.ampairs.auth.firebase

import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Desktop stub implementation of Firebase Authentication
 *
 * NOTE: Firebase Phone Auth is not supported on Desktop.
 * Future implementation will use QR code authentication (like WhatsApp Web):
 * 1. Generate QR code with session token
 * 2. User scans with mobile app
 * 3. Mobile app authenticates and sends token back
 * 4. Desktop app receives authentication confirmation
 *
 * For now, this is a stub that returns "not supported" errors.
 */
actual class FirebaseAuthProvider {

    private val _verificationState = MutableStateFlow<PhoneVerificationState>(PhoneVerificationState.Idle)
    actual val verificationState: StateFlow<PhoneVerificationState> = _verificationState.asStateFlow()

    actual suspend fun initialize(): FirebaseAuthResult<Unit> {
        return FirebaseAuthResult.Error(
            "Firebase authentication is not supported on Desktop. " +
            "QR code authentication will be implemented in a future update."
        )
    }

    actual suspend fun sendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return FirebaseAuthResult.Error(
            "Phone authentication not supported on Desktop. Please use the mobile app or web version."
        )
    }

    actual suspend fun verifyCode(verificationId: String, code: String): FirebaseAuthResult<String> {
        return FirebaseAuthResult.Error(
            "Phone authentication not supported on Desktop. Please use the mobile app or web version."
        )
    }

    actual suspend fun resendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return FirebaseAuthResult.Error(
            "Phone authentication not supported on Desktop. Please use the mobile app or web version."
        )
    }

    actual suspend fun getCurrentUserId(): String? {
        return null
    }

    actual suspend fun signOut(): FirebaseAuthResult<Unit> {
        return FirebaseAuthResult.Error("Not supported on Desktop")
    }

    actual fun isSupported(): Boolean = false
}

/**
 * TODO: Future QR Code Authentication Implementation
 *
 * Implementation plan:
 * 1. Generate unique session token
 * 2. Create QR code containing: session_token + desktop_device_id
 * 3. Display QR code on desktop login screen
 * 4. Mobile app scans QR code
 * 5. Mobile app sends authentication request to backend with:
 *    - session_token
 *    - desktop_device_id
 *    - mobile_user_auth_token
 * 6. Backend validates and creates desktop session
 * 7. Desktop polls backend for session confirmation
 * 8. Once confirmed, desktop receives auth tokens
 *
 * Similar to WhatsApp Web authentication flow.
 */
