package com.ampairs.product.ui.images

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_cancel
import ampairsapp.feature.product.generated.resources.prod_images_description_label
import ampairsapp.feature.product.generated.resources.prod_images_file_info
import ampairsapp.feature.product.generated.resources.prod_images_set_primary
import ampairsapp.feature.product.generated.resources.prod_images_upload_btn
import ampairsapp.feature.product.generated.resources.prod_images_upload_dialog_title
import ampairsapp.feature.product.generated.resources.prod_images_uploading
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductImageUploadDialog(
    uploadData: ProductImageUploadData?,
    onDismiss: () -> Unit,
    onUpload: (ProductImageUploadData) -> Unit,
    isUploading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (uploadData != null) {
        var description by remember(uploadData) { mutableStateOf(uploadData.description) }
        var isPrimary by remember(uploadData) { mutableStateOf(uploadData.isPrimary) }

        Dialog(
            onDismissRequest = { if (!isUploading) onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = !isUploading,
                dismissOnClickOutside = !isUploading,
            )
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.prod_images_upload_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    // File info card
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = stringResource(Res.string.prod_images_file_info),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            FileInfoRow(label = "Name", value = uploadData.pickerResult.fileName)
                            FileInfoRow(label = "Size", value = formatFileSize(uploadData.pickerResult.fileSize))
                            FileInfoRow(label = "Type", value = uploadData.pickerResult.contentType)
                        }
                    }

                    // Description field
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(Res.string.prod_images_description_label)) },
                        leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isUploading,
                        maxLines = 3,
                    )

                    // Set primary toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isPrimary) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(Res.string.prod_images_set_primary),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = isPrimary,
                            onCheckedChange = { isPrimary = it },
                            enabled = !isUploading,
                        )
                    }

                    // Upload progress indicator
                    if (isUploading) {
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = stringResource(Res.string.prod_images_uploading),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            enabled = !isUploading,
                        ) {
                            Text(stringResource(Res.string.prod_cancel))
                        }

                        Button(
                            onClick = {
                                onUpload(uploadData.copy(description = description.trim(), isPrimary = isPrimary))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isUploading,
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(
                                    Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.prod_images_upload_btn))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileInfoRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
}
