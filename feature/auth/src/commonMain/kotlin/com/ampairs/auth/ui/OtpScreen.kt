package com.ampairs.auth.ui

import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.otp_auto_verification_desc
import ampairsapp.feature.auth.generated.resources.otp_cd_timer
import ampairsapp.feature.auth.generated.resources.otp_cd_verification
import ampairsapp.feature.auth.generated.resources.otp_code_sent_no_phone
import ampairsapp.feature.auth.generated.resources.otp_code_sent_with_phone
import ampairsapp.feature.auth.generated.resources.otp_enter_manually
import ampairsapp.feature.auth.generated.resources.otp_enter_verification_code
import ampairsapp.feature.auth.generated.resources.otp_help_text
import ampairsapp.feature.auth.generated.resources.otp_resend
import ampairsapp.feature.auth.generated.resources.otp_resend_timer
import ampairsapp.feature.auth.generated.resources.otp_resend_with_timer
import ampairsapp.feature.auth.generated.resources.otp_verify
import ampairsapp.feature.auth.generated.resources.otp_waiting_for_auto_verification
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.auth.domain.AuthMethod
import com.ampairs.auth.domain.PhoneVerificationState
import com.ampairs.auth.viewmodel.LoginNavEvent
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.common.components.Otp
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

@Composable
fun OtpScreen(
    viewModel: LoginViewModel = metroViewModel(),
    sessionId: String,
    verificationId: String = "",
    phoneNumber: String = "",
    onAuthSuccess: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var waitingForAutoVerification by remember {
        mutableStateOf(state.authMethod == AuthMethod.FIREBASE && verificationId.isNotBlank())
    }
    var timerVersion by remember { mutableStateOf(0) }
    var resendTimer by remember { mutableStateOf(60) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(sessionId, verificationId, phoneNumber) {
        viewModel.initOtpScreen(sessionId, verificationId, phoneNumber)
    }

    LaunchedEffect(timerVersion) {
        resendTimer = 60
        canResend = false
        while (resendTimer > 0) {
            delay(1000)
            resendTimer--
        }
        canResend = true
    }

    LaunchedEffect(waitingForAutoVerification) {
        if (waitingForAutoVerification) {
            delay(8000)
            if (waitingForAutoVerification) waitingForAutoVerification = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.firebaseVerificationState.collect { verificationState ->
            when (verificationState) {
                is PhoneVerificationState.VerificationCompleted -> {
                    if (state.authMethod == AuthMethod.FIREBASE) {
                        waitingForAutoVerification = false
                        viewModel.completeFirebaseAuthenticationWithToken(verificationState.userId)
                    }
                }
                is PhoneVerificationState.VerificationFailed -> waitingForAutoVerification = false
                is PhoneVerificationState.CodeSent -> Unit
                PhoneVerificationState.Idle -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                LoginNavEvent.AuthComplete -> onAuthSuccess()
                LoginNavEvent.OtpResent -> timerVersion++
                LoginNavEvent.FirebaseOtpResent -> {
                    waitingForAutoVerification = true
                    timerVersion++
                }
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(32.dp))

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
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = stringResource(Res.string.otp_cd_verification),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.otp_enter_verification_code),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (state.phoneNumber.isNotEmpty()) {
                            stringResource(Res.string.otp_code_sent_with_phone, state.phoneNumber)
                        } else {
                            stringResource(Res.string.otp_code_sent_no_phone)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (waitingForAutoVerification) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            strokeWidth = 4.dp
                        )
                        Text(
                            text = stringResource(Res.string.otp_waiting_for_auto_verification),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(Res.string.otp_auto_verification_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { waitingForAutoVerification = false }) {
                            Text(stringResource(Res.string.otp_enter_manually))
                        }
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Otp(onValueChange = viewModel::updateOtp)

                        if (state.progressMessage.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (state.recaptchaLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                                Text(
                                    text = state.progressMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

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
                                        contentDescription = stringResource(Res.string.otp_cd_timer),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                    Text(
                                        text = stringResource(Res.string.otp_resend_timer, resendTimer),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!waitingForAutoVerification) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            when (state.authMethod) {
                                AuthMethod.BACKEND_API -> viewModel.completeAuthentication()
                                AuthMethod.FIREBASE -> viewModel.completeFirebaseAuthentication()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.validPhoneNumber && !state.loading
                    ) {
                        if (state.loading) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .progressSemantics()
                                    .size(24.dp)
                            )
                        } else {
                            Text(stringResource(Res.string.otp_verify))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            when (state.authMethod) {
                                AuthMethod.BACKEND_API -> viewModel.resendOtp()
                                AuthMethod.FIREBASE -> viewModel.resendFirebaseOtp()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.loading && canResend
                    ) {
                        Text(
                            if (resendTimer > 0) {
                                stringResource(Res.string.otp_resend_with_timer, resendTimer)
                            } else {
                                stringResource(Res.string.otp_resend)
                            }
                        )
                    }

                    Text(
                        text = stringResource(Res.string.otp_help_text),
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
