package com.ampairs.auth.ui

import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.auth_action_required
import ampairsapp.feature.auth.generated.resources.auth_before_you_proceed
import ampairsapp.feature.auth.generated.resources.auth_blocking_workspace_entry
import ampairsapp.feature.auth.generated.resources.auth_cancel_deletion
import ampairsapp.feature.auth.generated.resources.auth_cannot_delete_title
import ampairsapp.feature.auth.generated.resources.auth_cd_warning
import ampairsapp.feature.auth.generated.resources.auth_confirm_deletion_checkbox
import ampairsapp.feature.auth.generated.resources.auth_days_remaining_format
import ampairsapp.feature.auth.generated.resources.auth_delete_account_title
import ampairsapp.feature.auth.generated.resources.auth_delete_my_account
import ampairsapp.feature.auth.generated.resources.auth_delete_or_transfer
import ampairsapp.feature.auth.generated.resources.auth_deletion_effects_list
import ampairsapp.feature.auth.generated.resources.auth_deletion_effects_title
import ampairsapp.feature.auth.generated.resources.auth_deletion_pending_title
import ampairsapp.feature.auth.generated.resources.auth_deletion_scheduled
import ampairsapp.feature.auth.generated.resources.auth_deletion_scheduled_desc
import ampairsapp.feature.auth.generated.resources.auth_grace_period_desc
import ampairsapp.feature.auth.generated.resources.auth_grace_period_title
import ampairsapp.feature.auth.generated.resources.auth_reason_label
import ampairsapp.feature.auth.generated.resources.auth_member_count_format
import ampairsapp.feature.auth.generated.resources.auth_sole_owner_blocking
import ampairsapp.feature.auth.generated.resources.auth_sole_owner_warning
import ampairsapp.feature.auth.generated.resources.auth_warning_cannot_undo
import ampairsapp.feature.auth.generated.resources.cancel
import ampairsapp.feature.auth.generated.resources.ok
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.auth.api.model.AccountDeletionStatus
import com.ampairs.auth.api.model.BlockingWorkspace
import com.ampairs.auth.viewmodel.AccountDeletionNavEvent
import com.ampairs.auth.viewmodel.AccountDeletionViewModel
import com.ampairs.common.model.UiState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccountDeletionScreen(
    viewModel: AccountDeletionViewModel = metroViewModel(),
    onDeletionSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val isLoading = state.deletionState is UiState.Loading || state.statusState is UiState.Loading
    @Suppress("UNCHECKED_CAST")
    val successStatus = state.statusState as? UiState.Success<AccountDeletionStatus>
    val isDeletionPending = successStatus?.data?.isDeleted == true
    val daysRemaining = successStatus?.data?.daysRemaining

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                AccountDeletionNavEvent.DeletionSuccess -> onDeletionSuccess()
                AccountDeletionNavEvent.CancellationSuccess -> onNavigateBack()
                else -> {}
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Long)
        }
    }

    if (state.showBlockingWorkspacesDialog) {
        BlockingWorkspacesDialog(
            blockingWorkspaces = state.blockingWorkspaces,
            onDismiss = viewModel::dismissBlockingWorkspacesDialog
        )
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            if (isDeletionPending) {
                AccountDeletionPendingContent(
                    daysRemaining = daysRemaining,
                    isLoading = isLoading,
                    deletionState = state.deletionState,
                    onCancelDeletion = viewModel::cancelAccountDeletion
                )
            } else {
                AccountDeletionRequestContent(
                    reason = state.reason,
                    confirmed = state.confirmed,
                    ownedWorkspaces = state.ownedWorkspaces,
                    deletionState = state.deletionState,
                    isLoading = isLoading,
                    isFormValid = state.confirmed,
                    onUpdateReason = viewModel::updateReason,
                    onToggleConfirmation = viewModel::toggleConfirmation,
                    onRequestDeletion = viewModel::requestAccountDeletion
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountDeletionRequestContent(
    reason: String,
    confirmed: Boolean,
    ownedWorkspaces: List<BlockingWorkspace>,
    deletionState: UiState<*>,
    isLoading: Boolean,
    isFormValid: Boolean,
    onUpdateReason: (String) -> Unit,
    onToggleConfirmation: () -> Unit,
    onRequestDeletion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = stringResource(Res.string.auth_cd_warning),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.auth_delete_account_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (ownedWorkspaces.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = stringResource(Res.string.auth_cd_warning),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        Text(
                            text = stringResource(Res.string.auth_action_required),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(Res.string.auth_sole_owner_warning, ownedWorkspaces.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ownedWorkspaces.forEach { workspace ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = workspace.workspaceName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = stringResource(Res.string.auth_member_count_format, workspace.memberCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.auth_warning_cannot_undo),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_before_you_proceed),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.auth_delete_or_transfer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.auth_deletion_effects_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.auth_deletion_effects_list),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.auth_grace_period_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_grace_period_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = onUpdateReason,
            label = { Text(stringResource(Res.string.auth_reason_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = confirmed,
                onCheckedChange = { onToggleConfirmation() },
                enabled = !isLoading
            )
            Text(
                text = stringResource(Res.string.auth_confirm_deletion_checkbox),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRequestDeletion,
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            if (deletionState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.progressSemantics().size(20.dp),
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(Res.string.auth_delete_my_account))
            }
        }
    }
}

@Composable
private fun AccountDeletionPendingContent(
    daysRemaining: Int?,
    isLoading: Boolean,
    deletionState: UiState<*>,
    onCancelDeletion: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = stringResource(Res.string.auth_cd_warning),
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.auth_deletion_pending_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.auth_deletion_scheduled),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                daysRemaining?.let { days ->
                    Text(
                        text = stringResource(Res.string.auth_days_remaining_format, days),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.auth_deletion_scheduled_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onCancelDeletion,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (deletionState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.progressSemantics().size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(stringResource(Res.string.auth_cancel_deletion))
            }
        }
    }
}

@Composable
private fun BlockingWorkspacesDialog(
    blockingWorkspaces: List<BlockingWorkspace>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = stringResource(Res.string.auth_cd_warning),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(Res.string.auth_cannot_delete_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(Res.string.auth_sole_owner_blocking, blockingWorkspaces.size),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                blockingWorkspaces.forEach { workspace ->
                    Text(
                        text = stringResource(
                            Res.string.auth_blocking_workspace_entry,
                            workspace.workspaceName,
                            workspace.memberCount
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) }
        }
    )
}
