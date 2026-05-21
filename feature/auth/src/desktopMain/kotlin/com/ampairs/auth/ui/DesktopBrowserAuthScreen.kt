package com.ampairs.auth.ui

import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.desktop_auth_back
import ampairsapp.feature.auth.generated.resources.desktop_auth_browser_no_deep_links
import ampairsapp.feature.auth.generated.resources.desktop_auth_cancel
import ampairsapp.feature.auth.generated.resources.desktop_auth_click_button_below
import ampairsapp.feature.auth.generated.resources.desktop_auth_complete_in_browser
import ampairsapp.feature.auth.generated.resources.desktop_auth_credentials_not_stored
import ampairsapp.feature.auth.generated.resources.desktop_auth_how_it_works
import ampairsapp.feature.auth.generated.resources.desktop_auth_json_tokens
import ampairsapp.feature.auth.generated.resources.desktop_auth_paste_entire_json
import ampairsapp.feature.auth.generated.resources.desktop_auth_paste_instructions
import ampairsapp.feature.auth.generated.resources.desktop_auth_paste_tokens
import ampairsapp.feature.auth.generated.resources.desktop_auth_sign_in
import ampairsapp.feature.auth.generated.resources.desktop_auth_sign_in_securely
import ampairsapp.feature.auth.generated.resources.desktop_auth_sign_in_with_browser
import ampairsapp.feature.auth.generated.resources.desktop_auth_step_1
import ampairsapp.feature.auth.generated.resources.desktop_auth_step_2
import ampairsapp.feature.auth.generated.resources.desktop_auth_step_3
import ampairsapp.feature.auth.generated.resources.desktop_auth_waiting_for_authentication
import ampairsapp.feature.auth.generated.resources.desktop_auth_welcome
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ampairs.auth.deeplink.DeepLinkEvent
import com.ampairs.auth.deeplink.DeepLinkHandler
import com.ampairs.auth.viewmodel.LoginViewModel
import dev.zacsweers.metrox.viewmodel.metroViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * Desktop Browser Authentication Screen
 *
 * This screen guides the user to authenticate via their web browser.
 * Flow:
 * 1. User clicks "Sign in with Browser" button
 * 2. Default browser opens to web authentication page
 * 3. User completes phone authentication in browser
 * 4. Web page redirects to ampairs://auth deep link with tokens
 * 5. Desktop app receives tokens and completes login
 *
 * Similar to Slack, WhatsApp Desktop, and other desktop apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopBrowserAuthScreen(
    viewModel: LoginViewModel = metroViewModel(),
    onAuthSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isWaitingForAuth by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showManualPaste by remember { mutableStateOf(false) }
    var manualTokenJson by remember { mutableStateOf("") }
    var pasteError by remember { mutableStateOf<String?>(null) }

    // Listen for deep link events
    LaunchedEffect(Unit) {
        DeepLinkHandler.deepLinkEvents.collectLatest { event ->
            when (event) {
                is DeepLinkEvent.AuthCallback -> {
                    println("DesktopBrowserAuthScreen: Received auth tokens from deep link")
                    isWaitingForAuth = false

                    // Handle token storage and navigation
                    scope.launch {
                        try {
                            viewModel.handleBrowserAuthTokens(
                                accessToken = event.accessToken,
                                refreshToken = event.refreshToken,
                                onSuccess = {
                                    println("DesktopBrowserAuthScreen: Auth successful, navigating to main app")
                                    onAuthSuccess()
                                },
                                onError = { error ->
                                    println("DesktopBrowserAuthScreen: Auth failed: $error")
                                    errorMessage = error
                                    isWaitingForAuth = false
                                }
                            )
                        } catch (e: Exception) {
                            println("DesktopBrowserAuthScreen: Exception handling tokens: ${e.message}")
                            errorMessage = "Failed to process authentication: ${e.message}"
                            isWaitingForAuth = false
                        }
                    }
                }
                is DeepLinkEvent.Error -> {
                    println("DesktopBrowserAuthScreen: Deep link error: ${event.message}")
                    errorMessage = event.message
                    isWaitingForAuth = false
                }
                is DeepLinkEvent.Unknown -> {
                    println("DesktopBrowserAuthScreen: Unknown deep link: ${event.url}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.desktop_auth_sign_in)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo placeholder
            Card(
                modifier = Modifier.size(120.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "A",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Title
            Text(
                text = stringResource(Res.string.desktop_auth_welcome),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Instructions
            if (isWaitingForAuth) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!showManualPaste) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = stringResource(Res.string.desktop_auth_waiting_for_authentication),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(Res.string.desktop_auth_complete_in_browser),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Option to paste tokens manually
                            OutlinedButton(
                                onClick = { showManualPaste = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(Res.string.desktop_auth_browser_no_deep_links))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    isWaitingForAuth = false
                                    errorMessage = null
                                    showManualPaste = false
                                    manualTokenJson = ""
                                    pasteError = null
                                }
                            ) {
                                Text(stringResource(Res.string.desktop_auth_cancel))
                            }
                        } else {
                            // Manual token paste UI
                            Text(
                                text = stringResource(Res.string.desktop_auth_paste_tokens),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(Res.string.desktop_auth_paste_instructions),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = manualTokenJson,
                                onValueChange = {
                                    manualTokenJson = it
                                    pasteError = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                label = { Text(stringResource(Res.string.desktop_auth_json_tokens)) },
                                placeholder = {
                                    Text(
                                        """{"access_token": "...", "refresh_token": "..."}""",
                                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                    )
                                },
                                supportingText = {
                                    Text(stringResource(Res.string.desktop_auth_paste_entire_json))
                                },
                                isError = pasteError != null,
                                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )

                            pasteError?.let { error ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showManualPaste = false
                                        manualTokenJson = ""
                                        pasteError = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(stringResource(Res.string.desktop_auth_back))
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            val result = DeepLinkHandler.processManualTokens(manualTokenJson)
                                            if (result.isSuccess) {
                                                // Token processing will trigger deep link event
                                                // UI will automatically update via the flow collector
                                                pasteError = null
                                                println("DesktopBrowserAuthScreen: Manual tokens processed successfully")
                                            } else {
                                                pasteError = result.exceptionOrNull()?.message ?: "Failed to process tokens"
                                                println("DesktopBrowserAuthScreen: Manual token processing failed: $pasteError")
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = manualTokenJson.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(Res.string.desktop_auth_sign_in))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextButton(
                                onClick = {
                                    isWaitingForAuth = false
                                    errorMessage = null
                                    showManualPaste = false
                                    manualTokenJson = ""
                                    pasteError = null
                                }
                            ) {
                                Text(stringResource(Res.string.desktop_auth_cancel))
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.desktop_auth_sign_in_securely),
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.desktop_auth_click_button_below),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                println("DesktopBrowserAuthScreen: Opening browser for authentication")
                                isWaitingForAuth = true
                                errorMessage = null
                                DeepLinkHandler.openAuthenticationBrowser()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isWaitingForAuth
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.desktop_auth_sign_in_with_browser),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        // Error message
                        errorMessage?.let { error ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Information card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.desktop_auth_how_it_works),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BulletPoint(stringResource(Res.string.desktop_auth_step_1))
                        BulletPoint(stringResource(Res.string.desktop_auth_step_2))
                        BulletPoint(stringResource(Res.string.desktop_auth_step_3))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(Res.string.desktop_auth_credentials_not_stored),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
