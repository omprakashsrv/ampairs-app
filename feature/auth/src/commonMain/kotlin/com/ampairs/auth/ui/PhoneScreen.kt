package com.ampairs.auth.ui

import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.cancel
import ampairsapp.feature.auth.generated.resources.phone_login
import ampairsapp.feature.auth.generated.resources.phone_switch_to_this_user
import ampairsapp.feature.auth.generated.resources.phone_user_already_logged_in
import ampairsapp.feature.auth.generated.resources.phone_user_already_logged_in_desc
import ampairsapp.feature.auth.generated.resources.phone_user_exists
import ampairsapp.feature.auth.generated.resources.phone_would_you_like_to_switch
import ampairsapp.feature.auth.generated.resources.phone_welcome_title
import ampairsapp.feature.auth.generated.resources.phone_tagline
import ampairsapp.feature.auth.generated.resources.phone_enter_number_label
import ampairsapp.feature.auth.generated.resources.phone_feature_secure_title
import ampairsapp.feature.auth.generated.resources.phone_feature_secure_desc
import ampairsapp.feature.auth.generated.resources.phone_feature_fast_title
import ampairsapp.feature.auth.generated.resources.phone_feature_fast_desc
import ampairsapp.feature.auth.generated.resources.phone_by_continuing
import ampairsapp.feature.auth.generated.resources.phone_terms_of_service
import ampairsapp.feature.auth.generated.resources.phone_and
import ampairsapp.feature.auth.generated.resources.phone_privacy_policy_text
import ampairsapp.feature.auth.generated.resources.phone_cd_app_icon
import ampairsapp.feature.auth.generated.resources.user_selection_default_name
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.domain.AuthMethod
import com.ampairs.auth.viewmodel.LoginNavEvent
import com.ampairs.auth.viewmodel.LoginViewModel
import com.ampairs.common.navigation.ExitApp
import com.ampairs.common.navigation.PlatformBackHandler
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun PhoneScreen(
    viewModel: LoginViewModel = metroViewModel(),
    onAuthSuccess: (sessionId: String, verificationId: String) -> Unit,
    onExistingUserSelected: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showExistingUserDialog by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = true) { ExitApp() }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is LoginNavEvent.NavigateToOtp -> onAuthSuccess(event.sessionId, event.verificationId)
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(state.existingUser) {
        if (state.existingUser != null) showExistingUserDialog = true
    }

    if (showExistingUserDialog && state.existingUser != null) {
        ExistingUserDialog(
            user = state.existingUser!!,
            onSelectUser = {
                showExistingUserDialog = false
                viewModel.clearExistingUser()
                viewModel.selectExistingUser(state.existingUser!!.id)
            },
            onDismiss = {
                showExistingUserDialog = false
                viewModel.clearExistingUser()
            }
        )
    }

    val byContinuing = stringResource(Res.string.phone_by_continuing)
    val termsOfService = stringResource(Res.string.phone_terms_of_service)
    val andText = stringResource(Res.string.phone_and)
    val privacyPolicy = stringResource(Res.string.phone_privacy_policy_text)

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
                        .background(color = MaterialTheme.colorScheme.primaryContainer, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = stringResource(Res.string.phone_cd_app_icon),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.phone_welcome_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(Res.string.phone_tagline),
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
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(Res.string.phone_enter_number_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Phone(
                        countryCode = 91,
                        readOnly = state.loading,
                        phone = state.phoneNumber,
                        onValueChange = viewModel::updatePhoneNumber,
                        onValidChange = viewModel::updateValidPhoneNumber
                    )
                    if (state.progressMessage.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.recaptchaLoading || state.loading) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            }
                            Text(
                                text = state.progressMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FeatureItem(
                        icon = Icons.Default.Security,
                        title = stringResource(Res.string.phone_feature_secure_title),
                        description = stringResource(Res.string.phone_feature_secure_desc)
                    )
                    FeatureItem(
                        icon = Icons.Default.Speed,
                        title = stringResource(Res.string.phone_feature_fast_title),
                        description = stringResource(Res.string.phone_feature_fast_desc)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.checkExistingUser {
                            when (state.authMethod) {
                                AuthMethod.BACKEND_API -> viewModel.authenticate()
                                AuthMethod.FIREBASE -> viewModel.authenticateWithFirebase()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.validPhoneNumber && !state.loading
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.progressSemantics().size(24.dp)
                        )
                    } else {
                        Text(stringResource(Res.string.phone_login))
                    }
                }

                val linkColor = MaterialTheme.colorScheme.primary
                val textColor = MaterialTheme.colorScheme.onSurfaceVariant
                val linkStyle = TextLinkStyles(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)
                )
                val annotatedText = buildAnnotatedString {
                    withStyle(SpanStyle(color = textColor)) { append(byContinuing) }
                    withLink(LinkAnnotation.Url("https://www.ampairs.in/terms", linkStyle)) { append(termsOfService) }
                    withStyle(SpanStyle(color = textColor)) { append(andText) }
                    withLink(LinkAnnotation.Url("https://www.ampairs.in/privacy", linkStyle)) { append(privacyPolicy) }
                }
                Text(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.phone_user_already_logged_in_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${user.first_name} ${user.last_name}".trim().ifEmpty { stringResource(Res.string.user_selection_default_name) },
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
            Button(onClick = onSelectUser) {
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
