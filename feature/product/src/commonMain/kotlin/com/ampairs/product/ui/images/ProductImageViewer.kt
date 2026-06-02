package com.ampairs.product.ui.images

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import com.ampairs.file.api.FileItem
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_cancel
import ampairsapp.feature.product.generated.resources.prod_images_cd_image
import ampairsapp.feature.product.generated.resources.prod_images_delete_btn
import ampairsapp.feature.product.generated.resources.prod_images_primary_badge
import ampairsapp.feature.product.generated.resources.prod_images_set_primary_btn
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductImageViewer(
    image: FileItem?,
    onDismiss: () -> Unit,
    onDelete: (FileItem) -> Unit,
    onSetPrimary: (FileItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (image != null) {
        var showDeleteDialog by remember { mutableStateOf(false) }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
            )
        ) {
            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    TopAppBar(
                        title = {
                            Column {
                                Text(text = image.fileName, style = MaterialTheme.typography.titleMedium)
                                if (image.isPrimary) {
                                    Text(
                                        text = stringResource(Res.string.prod_images_primary_badge),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.prod_cancel))
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { onSetPrimary(image) },
                                enabled = !image.isPrimary,
                            ) {
                                Icon(
                                    if (image.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = stringResource(Res.string.prod_images_set_primary_btn),
                                    tint = if (image.isPrimary) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(Res.string.prod_images_delete_btn),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        ImageContent(image = image)
                    }

                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            DetailRow(label = "File", value = image.fileName)
                            DetailRow(label = "Size", value = formatFileSize(image.fileSize))
                            DetailRow(label = "Type", value = image.contentType)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(stringResource(Res.string.prod_images_delete_btn)) },
                text = { Text("Are you sure you want to delete this image? This action cannot be undone.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(image)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(Res.string.prod_images_delete_btn))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(Res.string.prod_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ImageContent(image: FileItem, modifier: Modifier = Modifier) {
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
        !image.localPath.isNullOrBlank() -> "file://${image.localPath}"
        image.imageUrl.isNotBlank() -> image.imageUrl
        image.thumbnailUrl.isNotBlank() -> image.thumbnailUrl
        else -> null
    }

    if (imageModel != null) {
        ElevatedCard(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        ) {
            AsyncImage(
                model = imageModel,
                contentDescription = stringResource(Res.string.prod_images_cd_image),
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
                        detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero })
                    }
                    .transformable(state = state)
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y),
                contentScale = ContentScale.Fit,
            )
        }
    } else {
        ElevatedCard(
            modifier = modifier.size(200.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Image not available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(2f),
        )
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
}
