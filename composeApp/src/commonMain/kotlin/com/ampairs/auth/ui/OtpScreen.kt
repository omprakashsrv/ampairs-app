package com.ampairs.auth.ui

import androidx.lifecycle.ViewModelStoreOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampairs.auth.domain.AuthMethod
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.ui.components.Otp
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.ampairs.auth.domain.PhoneVerificationState
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ampairs.common.localization.localizedString
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OtpScreen(
    viewModelStoreOwner: ViewModelStoreOwner,
    sessionId: String,
    verificationId: String = "", // Firebase verification ID (empty for backend auth)
    onAuthSuccess: () -> Unit,
) {
    val viewModel = koinViewModel<LoginViewModel>(viewModelStoreOwner = viewModelStoreOwner)

    // Phone number is now properly retained in the ViewModel
    val phoneNumber = viewModel.phoneNumber

    // Set the sessionId and verificationId from navigation parameters
    LaunchedEffect(sessionId, verificationId) {
        viewModel.sessionId = sessionId
        viewModel.firebaseVerificationId = verificationId
    }

    // State to track if we're waiting for auto-verification (Firebase only)
    var waitingForAutoVerification by remember {
        mutableStateOf(viewModel.authMethod == AuthMethod.FIREBASE && verificationId.isNotBlank())
    }

    // Resend timer state (60 seconds)
    var resendTimer by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    // Resend timer countdown
    LaunchedEffect(Unit) {
        while (resendTimer > 0) {
            delay(1000)
            resendTimer--
        }
        canResend = true
    }

    // Auto-verification timeout (8 seconds)
    // Restarts whenever waitingForAutoVerification becomes true
    LaunchedEffect(waitingForAutoVerification) {
        if (waitingForAutoVerification) {
            println("OtpScreen: ⏳ Starting 8-second timeout for auto-verification")
            delay(8000) // 8 seconds
            if (waitingForAutoVerification) {
                println("OtpScreen: ⏰ Auto-verification timeout reached, showing manual OTP input")
                waitingForAutoVerification = false
            }
        }
    }

    // Observe Firebase auto-verification state for automatic navigation
    val verificationState by viewModel.firebaseVerificationState.collectAsState()
    LaunchedEffect(verificationState) {
        when (verificationState) {
            is PhoneVerificationState.VerificationCompleted -> {
                // Auto-verification succeeded
                if (viewModel.authMethod == AuthMethod.FIREBASE) {
                    val completedState = verificationState as PhoneVerificationState.VerificationCompleted
                    println("OtpScreen: ✅ Auto-verification succeeded, proceeding with authentication")
                    waitingForAutoVerification = false
                    // completedState.userId contains the Firebase JWT ID token for backend verification
                    viewModel.completeFirebaseAuthenticationWithToken(completedState.userId, onAuthSuccess)
                }
            }
            is PhoneVerificationState.VerificationFailed -> {
                // Auto-verification failed, show manual OTP input
                println("OtpScreen: ❌ Auto-verification failed, showing manual OTP input")
                waitingForAutoVerification = false
            }
            is PhoneVerificationState.CodeSent -> {
                // Code sent, continue waiting for auto-verification
                println("OtpScreen: 📨 Code sent, waiting for auto-verification...")
            }
            PhoneVerificationState.Idle -> {
                // Idle state
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        if (viewModel.displayMessage.isNotEmpty()) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = viewModel.displayMessage,
                    duration = SnackbarDuration.Short
                )
                when (result) {
                    SnackbarResult.Dismissed -> {
                        viewModel.displayMessage = ""
                    }

                    SnackbarResult.ActionPerformed -> {
                        viewModel.displayMessage = ""
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Header and instructions
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                // Message icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = "Verification",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Title and description
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter Verification Code",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (phoneNumber.isNotEmpty()) {
                            "We've sent a 6-digit code to\n+91 $phoneNumber"
                        } else {
                            "We've sent a 6-digit code to your phone"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Middle section - OTP input or auto-verification
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (waitingForAutoVerification) {
                            // Show waiting for auto-verification UI
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    text = localizedString(Res.string.otp_waiting_for_auto_verification),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = localizedString(Res.string.otp_auto_verification_desc),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        println("OtpScreen: 👆 User chose to enter code manually")
                                        waitingForAutoVerification = false
                                    }
                                ) {
                                    Text(localizedString(Res.string.otp_enter_manually))
                                }
                            }
                } else {
                    // Show manual OTP input
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Otp(onValueChange = { viewModel.otp = it })

                        // Show reCAPTCHA status message
                        if (viewModel.progressMessage.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewModel.recaptchaLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text(
                                    text = viewModel.progressMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Timer card
                        if (resendTimer > 0) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Timer",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = "Resend code in ${resendTimer}s",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom section - Action buttons
            if (!waitingForAutoVerification) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Button(
                            onClick = {
                                when (viewModel.authMethod) {
                                    AuthMethod.BACKEND_API -> viewModel.completeAuthentication(onAuthSuccess)
                                    AuthMethod.FIREBASE -> viewModel.completeFirebaseAuthentication(onAuthSuccess)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = viewModel.validPhoneNumber && !viewModel.loading
                        ) {
                            if (viewModel.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .progressSemantics()
                                        .size(24.dp)
                                )
                            } else {
                                Text(localizedString(Res.string.otp_verify))
                            }
                        }

                    OutlinedButton(
                        onClick = {
                            when (viewModel.authMethod) {
                                AuthMethod.BACKEND_API -> {
                                    viewModel.resendOtp { sessionId ->
                                        viewModel.sessionId = sessionId
                                        // Reset timer
                                        resendTimer = 60
                                        canResend = false
                                    }
                                }
                                AuthMethod.FIREBASE -> {
                                    viewModel.resendFirebaseOtp { verificationId ->
                                        viewModel.firebaseVerificationId = verificationId
                                        // Reset waiting state and timer when resending
                                        waitingForAutoVerification = true
                                        resendTimer = 60
                                        canResend = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.loading && resendTimer == 0
                    ) {
                        Text(
                            if (resendTimer > 0) {
                                "Resend code (${resendTimer}s)"
                            } else {
                                localizedString(Res.string.otp_resend)
                            }
                        )
                    }

                    // Help text
                    Text(
                        text = "Didn't receive the code? Check your messages or request a new one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}