package com.ampairs.customer.ui.components.images

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FileUploadStatus

@Composable
fun CustomerImageGrid(
    images: List<FileItem>,
    onAddImage: (() -> Unit)?,
    onImageClick: (FileItem) -> Unit,
    onDeleteImage: ((FileItem) -> Unit)?,
    onSetPrimary: ((FileItem) -> Unit)?,
    modifier: Modifier = Modifier,
    maxImages: Int = 10
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customer Images" + if (onAddImage == null) " (Read-Only)" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "${images.size}/$maxImages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            if (images.size < maxImages && onAddImage != null) {
                item {
                    AddImageCard(onClick = onAddImage, modifier = Modifier.aspectRatio(1f))
                }
            }

            items(images, key = { it.uid }) { image ->
                CustomerImageCard(
                    image = image,
                    onClick = { onImageClick(image) },
                    onDelete = if (onDeleteImage != null) { { onDeleteImage(image) } } else null,
                    onSetPrimary = if (onSetPrimary != null) { { onSetPrimary(image) } } else null,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }

        if (images.isNotEmpty()) {
            ImageStatusSummary(images = images)
        }
    }
}

@Composable
private fun AddImageCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Image", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Add Image", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerImageCard(
    image: FileItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onSetPrimary: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (image.isPrimary) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageModel = when {
                image.uploadStatus == FileUploadStatus.PENDING || image.uploadStatus == FileUploadStatus.UPLOADING ->
                    image.localPath?.let { "file://$it" }
                image.thumbnailUrl.isNotBlank() -> image.thumbnailUrl
                image.imageUrl.isNotBlank() -> image.imageUrl
                else -> null
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Customer image",
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (image.isPrimary) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Text(text = "Primary", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            if (image.uploadStatus != FileUploadStatus.COMPLETED) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = when (image.uploadStatus) {
                        FileUploadStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        FileUploadStatus.UPLOADING -> MaterialTheme.colorScheme.secondary
                        FileUploadStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }
                ) {
                    Text(
                        text = when (image.uploadStatus) {
                            FileUploadStatus.PENDING -> "Pending"
                            FileUploadStatus.UPLOADING -> "Uploading"
                            FileUploadStatus.FAILED -> "Failed"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (image.uploadStatus) {
                            FileUploadStatus.PENDING -> MaterialTheme.colorScheme.onTertiary
                            FileUploadStatus.UPLOADING -> MaterialTheme.colorScheme.onSecondary
                            FileUploadStatus.FAILED -> MaterialTheme.colorScheme.onError
                            else -> Color.Transparent
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            val hasActions = onDelete != null || onSetPrimary != null
            var showActions by remember { mutableStateOf(false) }

            if (showActions && hasActions) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        if (onSetPrimary != null) {
                            IconButton(onClick = { onSetPrimary(); showActions = false }) {
                                Icon(
                                    if (image.isPrimary) Icons.Default.StarBorder else Icons.Default.Star,
                                    contentDescription = if (image.isPrimary) "Remove primary" else "Set primary",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = { onDelete(); showActions = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageStatusSummary(images: List<FileItem>, modifier: Modifier = Modifier) {
    val pendingCount = images.count { it.uploadStatus == FileUploadStatus.PENDING }
    val uploadingCount = images.count { it.uploadStatus == FileUploadStatus.UPLOADING }
    val failedCount = images.count { it.uploadStatus == FileUploadStatus.FAILED }
    val completedCount = images.count { it.uploadStatus == FileUploadStatus.COMPLETED }

    if (pendingCount > 0 || uploadingCount > 0 || failedCount > 0) {
        Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Upload Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (completedCount > 0) StatusChip("Completed", completedCount, MaterialTheme.colorScheme.primary)
                    if (uploadingCount > 0) StatusChip("Uploading", uploadingCount, MaterialTheme.colorScheme.secondary)
                    if (pendingCount > 0) StatusChip("Pending", pendingCount, MaterialTheme.colorScheme.tertiary)
                    if (failedCount > 0) StatusChip("Failed", failedCount, MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.12f)) {
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
