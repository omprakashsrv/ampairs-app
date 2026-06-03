package com.ampairs.customer.ui.components.images

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.ampairs.customer.util.CustomerLogger
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FileUploadStatus
import ampairsapp.feature.customer.generated.resources.Res
import ampairsapp.feature.customer.generated.resources.customer_cancel
import ampairsapp.feature.customer.generated.resources.customer_delete
import ampairsapp.feature.customer.generated.resources.customer_image_available
import ampairsapp.feature.customer.generated.resources.customer_image_close_cd
import ampairsapp.feature.customer.generated.resources.customer_image_delete_cd
import ampairsapp.feature.customer.generated.resources.customer_image_delete_confirm
import ampairsapp.feature.customer.generated.resources.customer_image_delete_title
import ampairsapp.feature.customer.generated.resources.customer_image_detail_content_type
import ampairsapp.feature.customer.generated.resources.customer_image_detail_filename
import ampairsapp.feature.customer.generated.resources.customer_image_detail_filesize
import ampairsapp.feature.customer.generated.resources.customer_image_detail_local_copy
import ampairsapp.feature.customer.generated.resources.customer_image_detail_server_url
import ampairsapp.feature.customer.generated.resources.customer_image_detail_status
import ampairsapp.feature.customer.generated.resources.customer_image_detail_thumbnail
import ampairsapp.feature.customer.generated.resources.customer_image_details_collapse
import ampairsapp.feature.customer.generated.resources.customer_image_details_expand
import ampairsapp.feature.customer.generated.resources.customer_image_details_title
import ampairsapp.feature.customer.generated.resources.customer_image_is_primary_cd
import ampairsapp.feature.customer.generated.resources.customer_image_not_available
import ampairsapp.feature.customer.generated.resources.customer_image_primary_label
import ampairsapp.feature.customer.generated.resources.customer_image_set_primary_cd
import ampairsapp.feature.customer.generated.resources.customer_image_status_completed
import ampairsapp.feature.customer.generated.resources.customer_image_status_failed
import ampairsapp.feature.customer.generated.resources.customer_image_status_pending
import ampairsapp.feature.customer.generated.resources.customer_image_status_unknown
import ampairsapp.feature.customer.generated.resources.customer_image_status_uploading
import ampairsapp.feature.customer.generated.resources.customer_image_viewer_cd
import org.jetbrains.compose.resources.stringResource

@Composable
fun CustomerImageViewer(
    image: FileItem?,
    onDismiss: () -> Unit,
    onDelete: (FileItem) -> Unit,
    onSetPrimary: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDetailsExpanded by remember { mutableStateOf(false) }

    if (image != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        ) {
            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    ImageViewerHeader(
                        image = image,
                        onClose = onDismiss,
                        onDelete = { onDelete(image) },
                        onSetPrimary = { onSetPrimary(image) }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ImageContent(image = image)
                    }

                    ImageDetailsExpandable(
                        image = image,
                        isExpanded = isDetailsExpanded,
                        onToggleExpanded = { isDetailsExpanded = !isDetailsExpanded },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageViewerHeader(
    image: FileItem,
    onClose: () -> Unit,
    onDelete: () -> Unit,
    onSetPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(
                    text = image.fileName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (image.isPrimary) {
                    Text(
                        text = stringResource(Res.string.customer_image_primary_label),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.customer_image_close_cd)
                )
            }
        },
        actions = {
            IconButton(
                onClick = onSetPrimary,
                enabled = !image.isPrimary
            ) {
                Icon(
                    if (image.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (image.isPrimary)
                        stringResource(Res.string.customer_image_is_primary_cd)
                    else
                        stringResource(Res.string.customer_image_set_primary_cd),
                    tint = if (image.isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(Res.string.customer_image_delete_cd),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier
    )

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(Res.string.customer_image_delete_title)) },
            text = { Text(stringResource(Res.string.customer_image_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(Res.string.customer_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(Res.string.customer_cancel))
                }
            }
        )
    }
}

@Composable
private fun ImageContent(
    image: FileItem,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    val imageModel = when {
        (image.uploadStatus == FileUploadStatus.PENDING || image.uploadStatus == FileUploadStatus.UPLOADING) && !image.localPath.isNullOrBlank() -> {
            val filePath = "file://${image.localPath}"
            CustomerLogger.d("CustomerImageViewer", "Using local file: $filePath")
            filePath
        }
        image.imageUrl.isNotBlank() -> {
            CustomerLogger.d("CustomerImageViewer", "Loading image URL: ${image.imageUrl}")
            image.imageUrl
        }
        image.thumbnailUrl.isNotBlank() -> {
            CustomerLogger.d("CustomerImageViewer", "Loading thumbnail URL: ${image.thumbnailUrl}")
            image.thumbnailUrl
        }
        !image.localPath.isNullOrBlank() -> {
            val filePath = "file://${image.localPath}"
            CustomerLogger.d("CustomerImageViewer", "Using local file fallback: $filePath")
            filePath
        }
        else -> {
            CustomerLogger.d("CustomerImageViewer", "No image available for ${image.uid}")
            null
        }
    }

    if (imageModel != null) {
        ElevatedCard(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = stringResource(Res.string.customer_image_viewer_cd, image.fileName),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Scroll) {
                                    val scrollDelta = event.changes.first().scrollDelta.y
                                    val zoomChange = if (scrollDelta > 0) 0.9f else 1.1f
                                    scale = (scale * zoomChange).coerceIn(1f, 5f)
                                    if (scale == 1f) offset = Offset.Zero
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        )
                    }
                    .transformable(state = state)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit,
                onError = { error ->
                    CustomerLogger.w("CustomerImageViewer", "Failed to load image: $imageModel", error.result.throwable)
                },
                onSuccess = {
                    CustomerLogger.d("CustomerImageViewer", "Successfully loaded image: $imageModel")
                }
            )
        }
    } else {
        ElevatedCard(
            modifier = modifier.size(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(Res.string.customer_image_not_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageDetailsExpandable(
    image: FileItem,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.customer_image_details_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded)
                        stringResource(Res.string.customer_image_details_collapse)
                    else
                        stringResource(Res.string.customer_image_details_expand),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            if (isExpanded) {
                ImageDetails(
                    image = image,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun ImageDetails(
    image: FileItem,
    modifier: Modifier = Modifier
) {
    val labelFileName = stringResource(Res.string.customer_image_detail_filename)
    val labelFileSize = stringResource(Res.string.customer_image_detail_filesize)
    val labelContentType = stringResource(Res.string.customer_image_detail_content_type)
    val labelStatus = stringResource(Res.string.customer_image_detail_status)
    val labelServerUrl = stringResource(Res.string.customer_image_detail_server_url)
    val labelThumbnail = stringResource(Res.string.customer_image_detail_thumbnail)
    val labelLocalCopy = stringResource(Res.string.customer_image_detail_local_copy)
    val statusPending = stringResource(Res.string.customer_image_status_pending)
    val statusUploading = stringResource(Res.string.customer_image_status_uploading)
    val statusCompleted = stringResource(Res.string.customer_image_status_completed)
    val statusFailed = stringResource(Res.string.customer_image_status_failed)
    val statusUnknown = stringResource(Res.string.customer_image_status_unknown)
    val valueAvailable = stringResource(Res.string.customer_image_available)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DetailRow(label = labelFileName, value = image.fileName)
        DetailRow(label = labelFileSize, value = formatFileSize(image.fileSize))
        DetailRow(label = labelContentType, value = image.contentType)

        DetailRow(
            label = labelStatus,
            value = when (image.uploadStatus) {
                FileUploadStatus.PENDING -> statusPending
                FileUploadStatus.UPLOADING -> statusUploading
                FileUploadStatus.COMPLETED -> statusCompleted
                FileUploadStatus.FAILED -> statusFailed
                else -> statusUnknown
            }
        )

        if (image.imageUrl.isNotBlank()) {
            DetailRow(label = labelServerUrl, value = valueAvailable, isUrl = true)
        }
        if (image.thumbnailUrl.isNotBlank()) {
            DetailRow(label = labelThumbnail, value = valueAvailable, isUrl = true)
        }
        if (!image.localPath.isNullOrBlank()) {
            DetailRow(label = labelLocalCopy, value = valueAvailable, isUrl = true)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isUrl: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isUrl) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(2f)
        )
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
