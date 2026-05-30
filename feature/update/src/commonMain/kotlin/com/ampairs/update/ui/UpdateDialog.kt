package com.ampairs.update.ui

import ampairsapp.feature.update.generated.resources.Res
import ampairsapp.feature.update.generated.resources.update_btn_cancel
import ampairsapp.feature.update.generated.resources.update_btn_close
import ampairsapp.feature.update.generated.resources.update_btn_download
import ampairsapp.feature.update.generated.resources.update_btn_install
import ampairsapp.feature.update.generated.resources.update_btn_later
import ampairsapp.feature.update.generated.resources.update_download_complete
import ampairsapp.feature.update.generated.resources.update_download_failed
import ampairsapp.feature.update.generated.resources.update_downloading
import ampairsapp.feature.update.generated.resources.update_install_failed
import ampairsapp.feature.update.generated.resources.update_install_instructions
import ampairsapp.feature.update.generated.resources.update_installed
import ampairsapp.feature.update.generated.resources.update_installing
import ampairsapp.feature.update.generated.resources.update_mandatory_badge
import ampairsapp.feature.update.generated.resources.update_size
import ampairsapp.feature.update.generated.resources.update_title_optional
import ampairsapp.feature.update.generated.resources.update_title_required
import ampairsapp.feature.update.generated.resources.update_version
import ampairsapp.feature.update.generated.resources.update_whats_new
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.common.util.formatDecimal
import com.ampairs.update.domain.UpdateDownloadState
import com.ampairs.update.domain.UpdateInfo
import com.ampairs.update.domain.UpdateInstallState
import com.ampairs.update.viewmodel.UpdateViewModel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun UpdateDialog(
    updateInfo: UpdateInfo,
    onDismiss: () -> Unit,
    onDismissUpdate: () -> Unit,
    viewModel: UpdateViewModel = assistedMetroViewModel<UpdateViewModel, UpdateViewModel.Factory>(
        key = updateInfo.version
    ) { create(updateInfo) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = {
            if (!updateInfo.isMandatory) {
                viewModel.reset()
                onDismiss()
            }
        },
        title = {
            Text(
                text = if (updateInfo.isMandatory) {
                    stringResource(Res.string.update_title_required)
                } else {
                    stringResource(Res.string.update_title_optional)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(Res.string.update_version, updateInfo.version),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(Res.string.update_size, updateInfo.fileSizeMb.formatDecimal(1)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (updateInfo.isMandatory) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(Res.string.update_mandatory_badge),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                if (!updateInfo.releaseNotes.isNullOrBlank()) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(Res.string.update_whats_new),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = updateInfo.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when (val ds = state.downloadState) {
                    is UpdateDownloadState.Downloading -> Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.update_downloading, (ds.progress * 100).toInt()),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        LinearProgressIndicator(progress = { ds.progress }, modifier = Modifier.fillMaxWidth())
                    }
                    is UpdateDownloadState.Downloaded -> Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(Res.string.update_download_complete),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    is UpdateDownloadState.Failed -> Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(Res.string.update_download_failed, ds.error),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {}
                }
                when (val installState = state.installState) {
                    is UpdateInstallState.Installing -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(
                            text = stringResource(Res.string.update_installing),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is UpdateInstallState.Installed -> Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(Res.string.update_installed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(Res.string.update_install_instructions),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    is UpdateInstallState.Failed -> Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = stringResource(Res.string.update_install_failed, installState.error),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    else -> {}
                }
            }
        },
        confirmButton = {
            when {
                state.installState is UpdateInstallState.Installed -> TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.update_btn_close))
                }
                state.downloadedFilePath != null -> Button(
                    onClick = { viewModel.installUpdate() },
                    enabled = state.installState !is UpdateInstallState.Installing
                ) {
                    Text(stringResource(Res.string.update_btn_install))
                }
                state.downloadState is UpdateDownloadState.Downloading -> OutlinedButton(onClick = {
                    viewModel.cancelDownload()
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.update_btn_cancel))
                }
                else -> Button(onClick = { viewModel.downloadUpdate() }) {
                    Text(stringResource(Res.string.update_btn_download))
                }
            }
        },
        dismissButton = {
            if (!updateInfo.isMandatory &&
                state.downloadedFilePath == null &&
                state.installState !is UpdateInstallState.Installed
            ) {
                TextButton(onClick = {
                    onDismissUpdate()
                    viewModel.reset()
                    onDismiss()
                }) {
                    Text(stringResource(Res.string.update_btn_later))
                }
            }
        }
    )
}
