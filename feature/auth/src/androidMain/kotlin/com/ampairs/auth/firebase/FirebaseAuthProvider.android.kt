package com.ampairs.auth.firebase

import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.PhoneVerificationState
import com.ampairs.common.CurrentActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Native Android Firebase Phone Authentication Implementation
 *
 * Uses official Firebase Android SDK for phone authentication
 * Requires:
 * - google-services.json in androidApp/ (automatically processed by Google Services plugin)
 * - Firebase project with Phone Auth enabled
 *
 * Firebase is automatically initialized by the Google Services Gradle plugin
 * which processes google-services.json at build time.
 *
 * Activity context is obtained from CurrentActivity, which tracks the foreground Activity
 * automatically via Application.ActivityLifecycleCallbacks registered in MainApp.
 */
actual class FirebaseAuthProvider {

    // Firebase Auth instance (automatically initialized by Google Services plugin)
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _verificationState = MutableStateFlow<PhoneVerificationState>(PhoneVerificationState.Idle)
    actual val verificationState: StateFlow<PhoneVerificationState> = _verificationState.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // Track auto-verification state to prevent duplicate attempts
    @Volatile
    private var autoVerificationInProgress = false
    @Volatile
    private var autoVerificationCompleted = false

    actual suspend fun initialize(): FirebaseAuthResult<Unit> {
        return try {
            // Firebase is automatically initialized by Google Services plugin
            // This method exists for consistency with the expect interface
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error("Firebase initialization failed: ${e.message}", e)
        }
    }

    actual suspend fun sendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                println("FirebaseAuth: 📱 Starting verification for: $phoneNumber")

                val activity = CurrentActivity.get()
                if (activity == null) {
                    println("FirebaseAuth: ❌ No activity available")
                    _verificationState.value = PhoneVerificationState.VerificationFailed(
                        "Activity not available. Please ensure the app is in foreground."
                    )
                    if (continuation.isActive) {
                        continuation.resume(
                            FirebaseAuthResult.Error("Activity context not available for Firebase Phone Auth")
                        )
                    }
                    return@suspendCancellableCoroutine
                }

                _verificationState.value = PhoneVerificationState.Idle

                // Reset auto-verification flags for new verification attempt
                autoVerificationInProgress = false
                autoVerificationCompleted = false

                val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber"
                println("FirebaseAuth: 📞 Formatted phone: ${formattedPhone.take(5)}...")

                val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            // Auto-verification succeeded (instant verification without SMS)
                            // This happens when Google Play Services auto-reads SMS or device is trusted
                            println("FirebaseAuth: ✅ Auto-verification completed")

                            // Check if auto-verification is already in progress or completed
                            // This prevents duplicate auto-sign-in attempts
                            synchronized(this@FirebaseAuthProvider) {
                                if (autoVerificationInProgress || autoVerificationCompleted) {
                                    println("FirebaseAuth: ⚠️ Auto-verification already in progress/completed, skipping duplicate")
                                    return
                                }
                                autoVerificationInProgress = true
                            }

                            _verificationState.value = PhoneVerificationState.CodeSent

                            // DON'T overwrite storedVerificationId here - keep it for manual verification
                            // The credential already contains everything needed for auto-sign-in

                            // Auto-sign in with the credential
                            signInWithCredential(credential) { result ->
                                synchronized(this@FirebaseAuthProvider) {
                                    autoVerificationInProgress = false
                                }

                                when (result) {
                                    is FirebaseAuthResult.Success -> {
                                        println("FirebaseAuth: ✅ Auto-sign-in successful")
                                        synchronized(this@FirebaseAuthProvider) {
                                            autoVerificationCompleted = true
                                        }
                                        _verificationState.value = PhoneVerificationState.VerificationCompleted(result.data)
                                        if (continuation.isActive) {
                                            continuation.resume(FirebaseAuthResult.Success(storedVerificationId ?: "auto"))
                                        }
                                    }
                                    is FirebaseAuthResult.Error -> {
                                        println("FirebaseAuth: ❌ Auto-sign-in failed: ${result.message}")
                                        if (continuation.isActive) {
                                            continuation.resume(result)
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }

                        override fun onVerificationFailed(exception: FirebaseException) {
                            println("FirebaseAuth: ❌ Verification failed: ${exception.message}")
                            println("FirebaseAuth: ❌ Exception type: ${exception.javaClass.simpleName}")
                            exception.printStackTrace()

                            _verificationState.value = PhoneVerificationState.VerificationFailed(
                                exception.message ?: "Verification failed"
                            )
                            if (continuation.isActive) {
                                continuation.resume(
                                    FirebaseAuthResult.Error("Verification failed: ${exception.message}", exception)
                                )
                            }
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            println("FirebaseAuth: ✅ Code sent successfully")
                            println("FirebaseAuth: 🔑 Verification ID: ${verificationId.take(20)}...")
                            println("FirebaseAuth: 🔑 Stored verification ID: ${verificationId.take(20)}...")

                            storedVerificationId = verificationId
                            resendToken = token
                            _verificationState.value = PhoneVerificationState.CodeSent

                            if (continuation.isActive) {
                                continuation.resume(FirebaseAuthResult.Success(verificationId))
                            }
                        }
                    })
                    .build()

                PhoneAuthProvider.verifyPhoneNumber(options)

            } catch (e: Exception) {
                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    e.message ?: "Failed to send verification code"
                )
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    actual suspend fun verifyCode(verificationId: String, code: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                println("FirebaseAuth: 🔐 Verifying code...")
                println("FirebaseAuth: 🔑 Using verification ID: ${verificationId.take(20)}...")
                println("FirebaseAuth: 🔑 Stored verification ID: ${storedVerificationId?.take(20)}...")
                println("FirebaseAuth: 🔢 Code length: ${code.length}")
                println("FirebaseAuth: ✓ IDs match: ${verificationId == storedVerificationId}")

                // Check if auto-verification already completed
                // If so, the code has already been consumed
                if (autoVerificationCompleted) {
                    println("FirebaseAuth: ⚠️ Auto-verification already completed, code may be expired")
                    println("FirebaseAuth: ℹ️ Attempting manual verification anyway...")
                }

                val credential = PhoneAuthProvider.getCredential(verificationId, code)
                println("FirebaseAuth: 📝 Credential created, attempting sign-in...")

                signInWithCredential(credential) { result ->
                    if (continuation.isActive) {
                        continuation.resume(result)
                    }
                }

            } catch (e: Exception) {
                println("FirebaseAuth: ❌ Exception during verifyCode: ${e.message}")
                e.printStackTrace()

                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    e.message ?: "Invalid verification code"
                )
                if (continuation.isActive) {
                    continuation.resume(FirebaseAuthResult.Error("Verification failed: ${e.message}", e))
                }
            }
        }
    }

    private fun signInWithCredential(
        credential: PhoneAuthCredential,
        onComplete: (FirebaseAuthResult<String>) -> Unit
    ) {
        println("FirebaseAuth: 🔓 Starting signInWithCredential...")

        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                println("FirebaseAuth: ✅ signInWithCredential SUCCESS")
                val user = authResult.user
                if (user != null) {
                    println("FirebaseAuth: 👤 User obtained: ${user.uid}")
                    // Get the ID token (JWT) instead of just the UID
                    user.getIdToken(false)
                        .addOnSuccessListener { tokenResult ->
                            val idToken = tokenResult.token
                            if (idToken != null) {
                                println("FirebaseAuth: 🎫 ID token obtained successfully")
                                // Pass the JWT ID token (not UID) to state for auto-verification flow
                                _verificationState.value = PhoneVerificationState.VerificationCompleted(idToken)
                                onComplete(FirebaseAuthResult.Success(idToken))
                            } else {
                                println("FirebaseAuth: ❌ No ID token in result")
                                _verificationState.value = PhoneVerificationState.VerificationFailed("No ID token returned")
                                onComplete(FirebaseAuthResult.Error("Authentication succeeded but no ID token returned"))
                            }
                        }
                        .addOnFailureListener { tokenException ->
                            println("FirebaseAuth: ❌ Failed to get ID token: ${tokenException.message}")
                            tokenException.printStackTrace()

                            _verificationState.value = PhoneVerificationState.VerificationFailed(
                                "Failed to get ID token: ${tokenException.message}"
                            )
                            onComplete(FirebaseAuthResult.Error("Failed to get ID token: ${tokenException.message}", tokenException))
                        }
                } else {
                    println("FirebaseAuth: ❌ No user in auth result")
                    _verificationState.value = PhoneVerificationState.VerificationFailed("No user returned")
                    onComplete(FirebaseAuthResult.Error("Authentication succeeded but no user returned"))
                }
            }
            .addOnFailureListener { exception ->
                println("FirebaseAuth: ❌ signInWithCredential FAILED: ${exception.message}")
                println("FirebaseAuth: ❌ Exception type: ${exception.javaClass.simpleName}")
                println("FirebaseAuth: ❌ Full error: ${exception}")
                exception.printStackTrace()

                _verificationState.value = PhoneVerificationState.VerificationFailed(
                    exception.message ?: "Authentication failed"
                )
                onComplete(FirebaseAuthResult.Error("Sign in failed: ${exception.message}", exception))
            }
    }

    actual suspend fun resendVerificationCode(phoneNumber: String): FirebaseAuthResult<String> {
        return suspendCancellableCoroutine { continuation ->
            try {
                val activity = CurrentActivity.get()
                if (activity == null) {
                    _verificationState.value = PhoneVerificationState.VerificationFailed(
                        "Activity not available. Please ensure the app is in foreground."
                    )
                    if (continuation.isActive) {
                        continuation.resume(
                            FirebaseAuthResult.Error("Activity context not available for Firebase Phone Auth")
                        )
                    }
                    return@suspendCancellableCoroutine
                }

                val formattedPhone = if (phoneNumber.startsWith("+")) phoneNumber else "+$phoneNumber"

                val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
                    .setPhoneNumber(formattedPhone)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            _verificationState.value = PhoneVerificationState.CodeSent
                            if (continuation.isActive) {
                                continuation.resume(FirebaseAuthResult.Success(storedVerificationId ?: "auto"))
                            }
                        }

                        override fun onVerificationFailed(exception: FirebaseException) {
                            _verificationState.value = PhoneVerificationState.VerificationFailed(
                                exception.message ?: "Verification failed"
                            )
                            if (continuation.isActive) {
                                continuation.resume(
                                    FirebaseAuthResult.Error("Resend failed: ${exception.message}", exception)
                                )
                            }
                        }

                        override fun onCodeSent(
                            verificationId: String,
                            token: PhoneAuthProvider.ForceResendingToken
                        ) {
                            storedVerificationId = verificationId
                            resendToken = token
                            _verificationState.value = PhoneVerificationState.CodeSent

                            if (continuation.isActive) {
                                continuation.resume(FirebaseAuthResult.Success(verificationId))
                            }
                        }
                    })

                // Use resend token if available
                resendToken?.let { optionsBuilder.setForceResendingToken(it) }

                PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())

            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }
        }
    }

    actual suspend fun getCurrentUserId(): String? {
        return try {
            firebaseAuth.currentUser?.uid
        } catch (e: Exception) {
            null
        }
    }

    actual suspend fun signOut(): FirebaseAuthResult<Unit> {
        return try {
            firebaseAuth.signOut()
            _verificationState.value = PhoneVerificationState.Idle
            storedVerificationId = null
            resendToken = null
            FirebaseAuthResult.Success(Unit)
        } catch (e: Exception) {
            FirebaseAuthResult.Error("Sign out failed: ${e.message}", e)
        }
    }

    actual fun isSupported(): Boolean = true
}
