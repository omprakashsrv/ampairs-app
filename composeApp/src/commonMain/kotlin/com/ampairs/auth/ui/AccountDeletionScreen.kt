package com.ampairs.auth.ui

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
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.auth.api.model.BlockingWorkspace
import com.ampairs.auth.viewmodel.AccountDeletionViewModel
import com.ampairs.common.model.UiState
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AccountDeletionScreen(
    onDeletionSuccess: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val viewModel: AccountDeletionViewModel = koinViewModel()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error/success messages
    LaunchedEffect(viewModel.displayMessage) {
        if (viewModel.displayMessage.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(
                message = viewModel.displayMessage,
                duration = SnackbarDuration.Long
            )
            when (result) {
                SnackbarResult.Dismissed -> viewModel.clearMessage()
                SnackbarResult.ActionPerformed -> viewModel.clearMessage()
            }
        }
    }

    // Blocking workspaces dialog
    if (viewModel.showBlockingWorkspacesDialog) {
        BlockingWorkspacesDialog(
            blockingWorkspaces = viewModel.blockingWorkspaces,
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

            // Check if account is already pending deletion
            if (viewModel.isDeletionPending) {
                AccountDeletionPendingContent(
                    viewModel = viewModel,
                    onCancelSuccess = onNavigateBack
                )
            } else {
                AccountDeletionRequestContent(
                    viewModel = viewModel,
                    onDeletionSuccess = onDeletionSuccess
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccountDeletionRequestContent(
    viewModel: AccountDeletionViewModel,
    onDeletionSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Warning header
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Delete Account",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Warning card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Warning: This action cannot be undone!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Deleting your account will:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Remove all your personal information\n" +
                            "• Log you out from all devices\n" +
                            "• Deactivate all workspace memberships\n" +
                            "• Begin a 30-day grace period\n" +
                            "• Permanently delete your data after 30 days",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Grace period info
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "30-Day Grace Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You can restore your account within 30 days by logging in again. " +
                            "After 30 days, your data will be permanently deleted and cannot be recovered.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Optional reason field
        OutlinedTextField(
            value = viewModel.reason,
            onValueChange = viewModel::updateReason,
            label = { Text("Reason for deletion (optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            enabled = !viewModel.isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Confirmation checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.confirmed,
                onCheckedChange = { viewModel.toggleConfirmation() },
                enabled = !viewModel.isLoading
            )
            Text(
                text = "I understand that this action cannot be undone and my data will be permanently deleted after 30 days.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Delete button
        Button(
            onClick = { viewModel.requestAccountDeletion(onDeletionSuccess) },
            modifier = Modifier.fillMaxWidth(),
            enabled = viewModel.isFormValid && !viewModel.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            if (viewModel.deletionState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .progressSemantics()
                        .size(20.dp),
                    color = MaterialTheme.colorScheme.onError,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Delete My Account")
            }
        }
    }
}

@Composable
private fun AccountDeletionPendingContent(
    viewModel: AccountDeletionViewModel,
    onCancelSuccess: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Warning",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Account Deletion Pending",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Status card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Your account is scheduled for deletion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(8.dp))

                viewModel.daysRemaining?.let { days ->
                    Text(
                        text = "Days remaining: $days",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your account will be permanently deleted after the grace period expires. " +
                            "If you change your mind, you can cancel the deletion request.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Cancel deletion button
        Button(
            onClick = { viewModel.cancelAccountDeletion(onCancelSuccess) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            if (viewModel.deletionState is UiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .progressSemantics()
                        .size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Cancel Deletion & Restore Account")
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
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text("Cannot Delete Account")
        },
        text = {
            Column {
                Text(
                    text = "You are the sole owner of ${blockingWorkspaces.size} workspace(s). " +
                            "Please transfer ownership or delete these workspaces before deleting your account:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                blockingWorkspaces.forEach { workspace ->
                    Text(
                        text = "• ${workspace.workspaceName} (${workspace.memberCount} members)",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
