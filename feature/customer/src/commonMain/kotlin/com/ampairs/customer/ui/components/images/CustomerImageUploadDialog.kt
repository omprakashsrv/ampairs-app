package com.ampairs.customer.ui.components.images

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
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_cancel
import ampairsapp.feature.customer.generated.resources.customer_image_desc_label
import ampairsapp.feature.customer.generated.resources.customer_image_desc_placeholder
import ampairsapp.feature.customer.generated.resources.customer_image_file_info_title
import ampairsapp.feature.customer.generated.resources.customer_image_file_name_label
import ampairsapp.feature.customer.generated.resources.customer_image_file_size_label
import ampairsapp.feature.customer.generated.resources.customer_image_file_type_label
import ampairsapp.feature.customer.generated.resources.customer_image_options_title
import ampairsapp.feature.customer.generated.resources.customer_image_primary_desc
import ampairsapp.feature.customer.generated.resources.customer_image_set_primary_label
import ampairsapp.feature.customer.generated.resources.customer_image_upload_btn
import ampairsapp.feature.customer.generated.resources.customer_image_upload_title
import ampairsapp.feature.customer.generated.resources.customer_image_uploading_btn
import ampairsapp.feature.customer.generated.resources.customer_image_uploading_desc
import ampairsapp.feature.customer.generated.resources.customer_image_uploading_title
import org.jetbrains.compose.resources.stringResource

data class ImageUploadData(
    val fileName: String,
    val fileSize: Long,
    val contentType: String,
    val imageData: ByteArray,
    val description: String = "",
    val isPrimary: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageUploadData

        if (fileName != other.fileName) return false
        if (fileSize != other.fileSize) return false
        if (contentType != other.contentType) return false
        if (!imageData.contentEquals(other.imageData)) return false
        if (description != other.description) return false
        if (isPrimary != other.isPrimary) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + imageData.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + isPrimary.hashCode()
        return result
    }
}

@Composable
fun CustomerImageUploadDialog(
    uploadData: ImageUploadData?,
    onDismiss: () -> Unit,
    onUpload: (ImageUploadData) -> Unit,
    isUploading: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (uploadData != null) {
        var description by remember(uploadData) { mutableStateOf(uploadData.description) }
        var isPrimary by remember(uploadData) { mutableStateOf(uploadData.isPrimary) }

        Dialog(
            onDismissRequest = { if (!isUploading) onDismiss() },
            properties = DialogProperties(
                dismissOnBackPress = !isUploading,
                dismissOnClickOutside = !isUploading
            )
        ) {
            Surface(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.customer_image_upload_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    FileInfoCard(uploadData = uploadData)

                    ImageOptionsSection(
                        description = description,
                        onDescriptionChange = { description = it },
                        isPrimary = isPrimary,
                        onPrimaryChange = { isPrimary = it },
                        enabled = !isUploading
                    )

                    if (isUploading) {
                        UploadProgressSection()
                    }

                    ActionButtonsSection(
                        onCancel = onDismiss,
                        onUpload = {
                            onUpload(
                                uploadData.copy(
                                    description = description.trim(),
                                    isPrimary = isPrimary
                                )
                            )
                        },
                        isUploading = isUploading
                    )
                }
            }
        }
    }
}

@Composable
private fun FileInfoCard(
    uploadData: ImageUploadData,
    modifier: Modifier = Modifier
) {
    val labelName = stringResource(Res.string.customer_image_file_name_label)
    val labelSize = stringResource(Res.string.customer_image_file_size_label)
    val labelType = stringResource(Res.string.customer_image_file_type_label)

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.customer_image_file_info_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            FileInfoRow(label = labelName, value = uploadData.fileName)
            FileInfoRow(label = labelSize, value = formatFileSize(uploadData.fileSize))
            FileInfoRow(label = labelType, value = uploadData.contentType)
        }
    }
}

@Composable
private fun FileInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ImageOptionsSection(
    description: String,
    onDescriptionChange: (String) -> Unit,
    isPrimary: Boolean,
    onPrimaryChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(Res.string.customer_image_options_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(Res.string.customer_image_desc_label)) },
            leadingIcon = {
                Icon(Icons.Default.Description, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            maxLines = 3,
            placeholder = { Text(stringResource(Res.string.customer_image_desc_placeholder)) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Star,
                contentDescription = null,
                tint = if (isPrimary) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.customer_image_set_primary_label),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = stringResource(Res.string.customer_image_primary_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = isPrimary,
                onCheckedChange = onPrimaryChange,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun UploadProgressSection(
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Column {
                Text(
                    text = stringResource(Res.string.customer_image_uploading_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = stringResource(Res.string.customer_image_uploading_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ActionButtonsSection(
    onCancel: () -> Unit,
    onUpload: () -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.weight(1f),
            enabled = !isUploading
        ) {
            Text(stringResource(Res.string.customer_cancel))
        }

        Button(
            onClick = onUpload,
            modifier = Modifier.weight(1f),
            enabled = !isUploading
        ) {
            if (isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isUploading) stringResource(Res.string.customer_image_uploading_btn)
                else stringResource(Res.string.customer_image_upload_btn)
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}
