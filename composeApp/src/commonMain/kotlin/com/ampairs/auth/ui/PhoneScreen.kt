package com.ampairs.auth.ui

import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.cancel
import ampairsapp.composeapp.generated.resources.phone_login
import ampairsapp.composeapp.generated.resources.phone_switch_to_this_user
import ampairsapp.composeapp.generated.resources.phone_user_already_logged_in
import ampairsapp.composeapp.generated.resources.phone_user_already_logged_in_desc
import ampairsapp.composeapp.generated.resources.phone_user_exists
import ampairsapp.composeapp.generated.resources.phone_would_you_like_to_switch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.domain.AuthMethod
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.ui.components.Phone
import androidx.compose.ui.text.font.FontWeight
import com.ampairs.ui.theme.AmpairsTheme
import org.koin.compose.koinInject
import com.ampairs.common.navigation.PlatformBackHandler
import com.ampairs.common.navigation.ExitApp
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhoneScreen(
    viewModel: LoginViewModel = koinInject<LoginViewModel>(),
    onAuthSuccess: (sessionId: String, verificationId: String) -> Unit,
    onExistingUserSelected: () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showExistingUserDialog by remember { mutableStateOf(false) }

    // Handle back button to exit app since this is the initial screen
    PlatformBackHandler(enabled = true) {
        ExitApp()
    }

    // Existing user dialog
    if (showExistingUserDialog && viewModel.existingUser != null) {
        ExistingUserDialog(
            user = viewModel.existingUser!!,
            onSelectUser = {
                viewModel.selectExistingUser(viewModel.existingUser!!.id) {
                    showExistingUserDialog = false
                    onExistingUserSelected()
                }
            },
            onDismiss = {
                showExistingUserDialog = false
            }
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        // Handle error messages
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

        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Phone input section - Center aligned
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 400.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Phone(
                        countryCode = 91,
                        readOnly = viewModel.loading,
                        phone = viewModel.phoneNumber,
                        onValueChange = { viewModel.phoneNumber = it },
                        onValidChange = { viewModel.validPhoneNumber = it }
                    )

                    // Show progress message
                    if (viewModel.progressMessage.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (viewModel.recaptchaLoading || viewModel.loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
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

            // Login button - Bottom aligned
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Button(
                    onClick = {
                        // First check if user already exists
                        viewModel.checkExistingUser(
                            onExistingUserFound = { user ->
                                // Show dialog with existing user
                                showExistingUserDialog = true
                            },
                            onNoExistingUser = {
                                // Proceed with authentication
                                when (viewModel.authMethod) {
                                    AuthMethod.BACKEND_API -> {
                                        viewModel.authenticate { sessionId ->
                                            onAuthSuccess(
                                                sessionId,
                                                ""
                                            ) // Backend API: sessionId populated, verificationId empty
                                        }
                                    }

                                    AuthMethod.FIREBASE -> {
                                        viewModel.authenticateWithFirebase { verificationId ->
                                            onAuthSuccess(
                                                "",
                                                verificationId
                                            ) // Firebase: verificationId populated, sessionId empty
                                        }
                                    }
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .widthIn(min = 280.dp, max = 400.dp)
                        .fillMaxWidth(),
                    enabled = viewModel.validPhoneNumber && !viewModel.loading
                ) {
                    if (viewModel.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    } else {
                        Text(stringResource(Res.string.phone_login))
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneScreenPreview() {
    AmpairsTheme {
        // Mock ViewModel state for preview
        var phoneNumber by remember { mutableStateOf("9876543210") }
        var validPhoneNumber by remember { mutableStateOf(true) }
        var recaptchaMessage by remember { mutableStateOf("Verifying you're human...") }
        var recaptchaLoading by remember { mutableStateOf(true) }
        var loading by remember { mutableStateOf(false) }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 400.dp)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Phone input section
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Phone(
                        countryCode = 91,
                        readOnly = loading,
                        phone = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        onValidChange = { validPhoneNumber = it }
                    )

                    // Show reCAPTCHA status message
                    if (recaptchaMessage.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (recaptchaLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                text = recaptchaMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Login button
                Button(
                    onClick = { loading = !loading },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = validPhoneNumber && !loading
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .progressSemantics()
                                .size(24.dp)
                        )
                    } else {
                        Text(stringResource(Res.string.phone_login))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExistingUserDialog(
    user: UserEntity,
    onSelectUser: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = stringResource(Res.string.phone_user_exists),
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(Res.string.phone_user_already_logged_in),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(Res.string.phone_user_already_logged_in_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${user.first_name} ${user.last_name}".trim().ifEmpty { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "+${user.country_code} ${user.phone}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = stringResource(Res.string.phone_would_you_like_to_switch),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSelectUser
            ) {
                Text(stringResource(Res.string.phone_switch_to_this_user))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.cancel))
            }
        }
    )
}