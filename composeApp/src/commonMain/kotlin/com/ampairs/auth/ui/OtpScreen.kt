package com.ampairs.auth.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import org.jetbrains.compose.resources.stringResource
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.resend_otp
import ampairsapp.composeapp.generated.resources.verify_otp
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun OtpScreen(
    sessionId: String,
    verificationId: String = "", // Firebase verification ID (empty for backend auth)
    onAuthSuccess: () -> Unit,
) {
    val viewModel = koinInject<LoginViewModel>()

    // Set the sessionId and verificationId from navigation parameters
    LaunchedEffect(sessionId, verificationId) {
        viewModel.sessionId = sessionId
        viewModel.firebaseVerificationId = verificationId
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(0.dp, 360.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Otp(onValueChange = { viewModel.otp = it })
                        
                        // Show reCAPTCHA status message
                        if (viewModel.progressMessage.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (viewModel.recaptchaLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.size(8.dp))
                                }
                                Text(
                                    text = viewModel.progressMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                }
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
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
                            Text(stringResource(Res.string.verify_otp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            when (viewModel.authMethod) {
                                AuthMethod.BACKEND_API -> {
                                    viewModel.resendOtp { sessionId ->
                                        viewModel.sessionId = sessionId
                                    }
                                }
                                AuthMethod.FIREBASE -> {
                                    viewModel.resendFirebaseOtp { verificationId ->
                                        viewModel.firebaseVerificationId = verificationId
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !viewModel.loading
                    ) {
                        Text(stringResource(Res.string.resend_otp))
                    }
                }
            }
        }
    }
}