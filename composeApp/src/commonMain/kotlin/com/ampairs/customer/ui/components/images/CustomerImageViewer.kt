package com.ampairs.customer.ui.components.images

import androidx.compose.foundation.background
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
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.customer.domain.CustomerImage
import com.ampairs.customer.domain.CustomerImageStatus
import com.ampairs.customer.util.CustomerLogger

@Composable
fun CustomerImageViewer(
    image: CustomerImage?,
    onDismiss: () -> Unit,
    onDelete: (CustomerImage) -> Unit,
    onSetPrimary: (CustomerImage) -> Unit,
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
                    // Header
                    ImageViewerHeader(
                        image = image,
                        onClose = onDismiss,
                        onDelete = { onDelete(image) },
                        onSetPrimary = { onSetPrimary(image) }
                    )

                    // Image Content - takes all available space
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ImageContent(image = image)
                    }

                    // Expandable Image Details
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
    image: CustomerImage,
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
                        text = "Primary Image",
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
                    contentDescription = "Close"
                )
            }
        },
        actions = {
            // Set Primary
            IconButton(
                onClick = onSetPrimary,
                enabled = !image.isPrimary
            ) {
                Icon(
                    if (image.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (image.isPrimary) "Primary image" else "Set as primary",
                    tint = if (image.isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete image",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        },
        modifier = modifier
    )

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Image") },
            text = {
                Text("Are you sure you want to delete this image? This action cannot be undone.")
            },
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
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ImageContent(
    image: CustomerImage,
    modifier: Modifier = Modifier
) {
    // Zoom state
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Transformable state for pinch-to-zoom and pan
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)

        // Only allow panning when zoomed in
        if (scale > 1f) {
            offset += offsetChange
        } else {
            offset = Offset.Zero
        }
    }

    // Use the same URL building logic as CustomerImageGrid for consistency - respect sync status
    val imageModel = when {
        // Prefer local file if available (for uploaded images)
        !image.localPath.isNullOrBlank() -> {
            val filePath = "file://${image.localPath}"
            CustomerLogger.d("CustomerImageViewer", "Using local file: $filePath")
            filePath
        }
        // For unsynced images, only load from local files - don't try server URLs
        // Note: CustomerImageViewer uses CustomerImage (full object), so we need to check if it has synced status
        // Since CustomerImage doesn't have synced field, we'll assume if it has server URLs it's synced
        // Use server image URL - Coil will download, cache, and handle offline automatically
        !image.imageUrl.isNullOrBlank() -> {
            val completeUrl = ApiUrlBuilder.buildCompleteUrl(image.imageUrl)
            CustomerLogger.d("CustomerImageViewer", "Loading image URL: ${image.imageUrl} -> $completeUrl")
            completeUrl
        }
        // Fallback to thumbnail URL if main image URL is not available
        !image.thumbnailUrl.isNullOrBlank() -> {
            val completeUrl = ApiUrlBuilder.buildCompleteUrl(image.thumbnailUrl)
            CustomerLogger.d("CustomerImageViewer", "Loading thumbnail URL: ${image.thumbnailUrl} -> $completeUrl")
            completeUrl
        }
        // No image available
        else -> {
            CustomerLogger.d("CustomerImageViewer", "No image available for ${image.uid}")
            null
        }
    }

    if (imageModel != null) {
        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = "Customer image: ${image.fileName}",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // Handle both tap gestures and scroll events
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()

                                // Handle mouse scroll for desktop zoom
                                if (event.type == PointerEventType.Scroll) {
                                    val scrollDelta = event.changes.first().scrollDelta.y

                                    // Zoom with mouse wheel (scroll up = zoom in, scroll down = zoom out)
                                    val zoomChange = if (scrollDelta > 0) 0.9f else 1.1f
                                    scale = (scale * zoomChange).coerceIn(1f, 5f)

                                    // Reset offset when zooming out to 1x
                                    if (scale == 1f) {
                                        offset = Offset.Zero
                                    }

                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        // Double-tap to reset zoom (separate pointerInput for tap gestures)
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
                onSuccess = { success ->
                    CustomerLogger.d("CustomerImageViewer", "Successfully loaded image: $imageModel")
                }
            )
        }
    } else {
        // Placeholder for no image
        Card(
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
                        text = "Image not available",
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
    image: CustomerImage,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            // Toggle Button - entire row is clickable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Image Details",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse details" else "Expand details",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // Details Content - shown only when expanded
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
    image: CustomerImage,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // File Information
        DetailRow(label = "File Name", value = image.fileName)
        DetailRow(label = "File Size", value = formatFileSize(image.fileSize))
        DetailRow(label = "Content Type", value = image.contentType)

        if (!image.description.isNullOrBlank()) {
            DetailRow(label = "Description", value = image.description)
        }

        // Status Information
        DetailRow(
            label = "Upload Status",
            value = when (image.uploadStatus) {
                CustomerImageStatus.PENDING -> "Pending Upload"
                CustomerImageStatus.UPLOADING -> "Uploading..."
                CustomerImageStatus.COMPLETED -> "Uploaded Successfully"
                CustomerImageStatus.FAILED -> "Upload Failed"
                else -> "Unknown"
            }
        )

        // URLs (if available)
        if (!image.imageUrl.isNullOrBlank()) {
            DetailRow(label = "Server URL", value = "Available", isUrl = true)
        }
        if (!image.thumbnailUrl.isNullOrBlank()) {
            DetailRow(label = "Thumbnail", value = "Available", isUrl = true)
        }
        if (!image.localPath.isNullOrBlank()) {
            DetailRow(label = "Local Copy", value = "Available", isUrl = true)
        }

        // Timestamps
        image.createdAt?.let { createdAt ->
            DetailRow(label = "Created", value = createdAt)
        }
        image.updatedAt?.let { updatedAt ->
            DetailRow(label = "Updated", value = updatedAt)
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