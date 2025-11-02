package com.ampairs.customer.ui.components.images

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.customer.domain.CustomerImageListItem
import com.ampairs.customer.domain.CustomerImageStatus
import com.ampairs.customer.util.CustomerLogger

@Composable
fun CustomerImageGrid(
    images: List<CustomerImageListItem>,
    onAddImage: (() -> Unit)?,
    onImageClick: (CustomerImageListItem) -> Unit,
    onDeleteImage: ((CustomerImageListItem) -> Unit)?,
    onSetPrimary: ((CustomerImageListItem) -> Unit)?,
    modifier: Modifier = Modifier,
    maxImages: Int = 10
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customer Images" + if (onAddImage == null) " (Read-Only)" else "",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "${images.size}/$maxImages",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Image Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp)
        ) {
            // Add Image Card (if under limit and not read-only)
            if (images.size < maxImages && onAddImage != null) {
                item {
                    AddImageCard(
                        onClick = onAddImage,
                        modifier = Modifier.aspectRatio(1f)
                    )
                }
            }

            // Existing Images
            items(images, key = { it.uid }) { image ->
                CustomerImageCard(
                    image = image,
                    onClick = { onImageClick(image) },
                    onDelete = if (onDeleteImage != null) {
                        { onDeleteImage(image) }
                    } else null,
                    onSetPrimary = if (onSetPrimary != null) {
                        { onSetPrimary(image) }
                    } else null,
                    modifier = Modifier.aspectRatio(1f)
                )
            }
        }

        // Status Summary
        if (images.isNotEmpty()) {
            ImageStatusSummary(images = images)
        }
    }
}

@Composable
private fun AddImageCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Add Image",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add Image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerImageCard(
    image: CustomerImageListItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onSetPrimary: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (image.isPrimary)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image Content - Respect sync status for loading strategy
            val imageModel = when {
                // Prefer local file if available (for uploaded images)
                !image.localPath.isNullOrBlank() -> {
                    val filePath = "file://${image.localPath}"
                    CustomerLogger.d("CustomerImageGrid", "Using local file: $filePath")
                    filePath
                }
                // For unsynced images, only load from local files - don't try server URLs
                !image.synced -> {
                    CustomerLogger.d("CustomerImageGrid", "Unsynced image ${image.uid} - no local file available, skipping server load")
                    null
                }
                // For synced images, use server thumbnail URL - Coil will download, cache, and handle offline automatically
                !image.thumbnailUrl.isNullOrBlank() -> {
                    val completeUrl = ApiUrlBuilder.buildCompleteUrl(image.thumbnailUrl!!)
                    CustomerLogger.d("CustomerImageGrid", "Loading synced image thumbnail URL: ${image.thumbnailUrl} -> $completeUrl")
                    completeUrl
                }
                // No image available
                else -> {
                    CustomerLogger.d("CustomerImageGrid", "No image available for ${image.uid}")
                    null
                }
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = "Customer image",
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface), // Add background to check if space is available
                    contentScale = ContentScale.Crop,
                    onError = { error ->
                        CustomerLogger.e("CustomerImageGrid", "Failed to load image: $imageModel", error.result.throwable)
                        CustomerLogger.e("CustomerImageGrid", "Error details: ${error.result.throwable?.message}")
                    },
                    onSuccess = { success ->
                        CustomerLogger.d("CustomerImageGrid", "Successfully loaded image: $imageModel")
                        CustomerLogger.d("CustomerImageGrid", "Image loaded successfully from: ${success.result.dataSource}")
                    },
                    onLoading = {
                        CustomerLogger.d("CustomerImageGrid", "Loading image: $imageModel")
                    },
                    placeholder = null, // Remove any default placeholder
                    error = null // Remove any default error drawable
                )
            } else {
                // Placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Primary Badge
            if (image.isPrimary) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "Primary",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Upload Status
            if (image.uploadStatus != CustomerImageStatus.COMPLETED) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = when (image.uploadStatus) {
                        CustomerImageStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                        CustomerImageStatus.UPLOADING -> MaterialTheme.colorScheme.secondary
                        CustomerImageStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }
                ) {
                    if (image.uploadStatus != CustomerImageStatus.COMPLETED) {
                        Text(
                            text = when (image.uploadStatus) {
                                CustomerImageStatus.PENDING -> "Pending"
                                CustomerImageStatus.UPLOADING -> "Uploading"
                                CustomerImageStatus.FAILED -> "Failed"
                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = when (image.uploadStatus) {
                                CustomerImageStatus.PENDING -> MaterialTheme.colorScheme.onTertiary
                                CustomerImageStatus.UPLOADING -> MaterialTheme.colorScheme.onSecondary
                                CustomerImageStatus.FAILED -> MaterialTheme.colorScheme.onError
                                else -> Color.Transparent
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Action Buttons (shown on long press or context menu) - only if not read-only
            val hasActions = onDelete != null || onSetPrimary != null
            var showActions by remember { mutableStateOf(false) }

            // Actions Overlay
            if (showActions && hasActions) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Set Primary/Unset Primary
                        if (onSetPrimary != null) {
                            IconButton(
                                onClick = {
                                    onSetPrimary()
                                    showActions = false
                                }
                            ) {
                                Icon(
                                    if (image.isPrimary) Icons.Default.StarBorder else Icons.Default.Star,
                                    contentDescription = if (image.isPrimary) "Remove primary" else "Set primary",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Delete
                        if (onDelete != null) {
                            IconButton(
                                onClick = {
                                    onDelete()
                                    showActions = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageStatusSummary(
    images: List<CustomerImageListItem>,
    modifier: Modifier = Modifier
) {
    val statusCounts = images.groupingBy { it.uploadStatus }.eachCount()
    val pendingCount = statusCounts[CustomerImageStatus.PENDING] ?: 0
    val uploadingCount = statusCounts[CustomerImageStatus.UPLOADING] ?: 0
    val failedCount = statusCounts[CustomerImageStatus.FAILED] ?: 0
    val completedCount = statusCounts[CustomerImageStatus.COMPLETED] ?: 0

    if (pendingCount > 0 || uploadingCount > 0 || failedCount > 0) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Upload Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (completedCount > 0) {
                        StatusChip(
                            label = "Completed",
                            count = completedCount,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uploadingCount > 0) {
                        StatusChip(
                            label = "Uploading",
                            count = uploadingCount,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    if (pendingCount > 0) {
                        StatusChip(
                            label = "Pending",
                            count = pendingCount,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    if (failedCount > 0) {
                        StatusChip(
                            label = "Failed",
                            count = failedCount,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = "$label ($count)",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}