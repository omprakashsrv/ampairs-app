package com.ampairs.update.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.update.domain.UpdateDownloadState
import com.ampairs.update.domain.UpdateInfo
import com.ampairs.update.domain.UpdateInstallState
import com.ampairs.update.service.UpdateDownloader
import com.ampairs.update.service.UpdateInstaller
import kotlinx.coroutines.launch

/**
 * Dialog to show update information and handle download/installation
 *
 * Features:
 * - Shows update details (version, size, release notes)
 * - Download progress indicator
 * - Mandatory vs optional update handling
 * - Install button after download
 */
@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    downloader: UpdateDownloader,
    installer: UpdateInstaller,
    onDismiss: () -> Unit,
    onDismissUpdate: () -> Unit,
) {
    val downloadState by downloader.downloadState.collectAsState()
    val installState by installer.installState.collectAsState()
    val scope = rememberCoroutineScope()

    var downloadedFilePath by remember { mutableStateOf<String?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }

    // Handle successful download
    LaunchedEffect(downloadState) {
        if (downloadState is UpdateDownloadState.Downloaded) {
            downloadedFilePath = (downloadState as UpdateDownloadState.Downloaded).filePath
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) {
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (updateInfo.isMandatory) "Required Update Available" else "Update Available",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Version info
                Text(
                    text = "Version ${updateInfo.version}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                // File size
                Text(
                    text = "Size: ${String.format("%.1f", updateInfo.fileSizeMb)} MB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Mandatory badge
                if (updateInfo.isMandatory) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "⚠️ This update is mandatory",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }

                // Release notes
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    Divider()
                    Text(
                        text = "What's New:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Download progress
                when (downloadState) {
                    is UpdateDownloadState.Downloading -> {
                        val progress = (downloadState as UpdateDownloadState.Downloading).progress
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Downloading... ${(progress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    is UpdateDownloadState.Downloaded -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "✅ Download complete!",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    is UpdateDownloadState.Failed -> {
                        val error = (downloadState as UpdateDownloadState.Failed).error
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "❌ Download failed: $error",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    else -> {}
                }

                // Installation status
                when (installState) {
                    is UpdateInstallState.Installing -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text(
                                text = "Opening installer...",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    is UpdateInstallState.Installed -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = "✅ Installer opened successfully!",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Please follow the installer instructions to complete the update.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    is UpdateInstallState.Failed -> {
                        val error = (installState as UpdateInstallState.Failed).error
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = "❌ Failed to open installer: $error",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    else -> {}
                }

                // Error message
                showError?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                installState is UpdateInstallState.Installed -> {
                    // After installation is complete, show Close button
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }

                downloadedFilePath != null -> {
                    // After download, show Install button
                    Button(
                        onClick = {
                            scope.launch {
                                installer.installUpdate(downloadedFilePath!!, updateInfo)
                            }
                        },
                        enabled = installState !is UpdateInstallState.Installing
                    ) {
                        Text("Install Now")
                    }
                }

                downloadState is UpdateDownloadState.Downloading -> {
                    // During download, show Cancel button
                    OutlinedButton(
                        onClick = {
                            downloader.cancelDownload()
                            onDismiss()
                        }
                    ) {
                        Text("Cancel")
                    }
                }

                else -> {
                    // Initial state, show Download button
                    Button(
                        onClick = {
                            scope.launch {
                                downloader.downloadUpdate(updateInfo)
                            }
                        }
                    ) {
                        Text("Download Update")
                    }
                }
            }
        },
        dismissButton = {
            // Only show dismiss/later button for optional updates
            if (!updateInfo.isMandatory && downloadedFilePath == null && installState !is UpdateInstallState.Installed) {
                TextButton(
                    onClick = {
                        onDismissUpdate()
                        onDismiss()
                    }
                ) {
                    Text("Later")
                }
            }
        }
    )
}
