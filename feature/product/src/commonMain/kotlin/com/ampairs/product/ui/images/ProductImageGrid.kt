package com.ampairs.product.ui.images

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.product.domain.ProductImageStatus
import com.ampairs.product.domain.ProductUploadImageListItem
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_images_cd_add
import ampairsapp.feature.product.generated.resources.prod_images_cd_delete
import ampairsapp.feature.product.generated.resources.prod_images_cd_image
import ampairsapp.feature.product.generated.resources.prod_images_primary_badge
import ampairsapp.feature.product.generated.resources.prod_images_set_primary_btn
import ampairsapp.feature.product.generated.resources.prod_images_delete_btn
import ampairsapp.feature.product.generated.resources.prod_images_status_uploading
import ampairsapp.feature.product.generated.resources.prod_images_status_failed
import ampairsapp.feature.product.generated.resources.prod_images_status_pending
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductImageGrid(
    images: List<ProductUploadImageListItem>,
    onAddImage: (() -> Unit)?,
    onImageClick: (ProductUploadImageListItem) -> Unit,
    onDeleteImage: ((ProductUploadImageListItem) -> Unit)?,
    onSetPrimary: ((ProductUploadImageListItem) -> Unit)?,
    modifier: Modifier = Modifier,
    maxImages: Int = 10,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 400.dp),
        ) {
            if (images.size < maxImages && onAddImage != null) {
                item {
                    AddImageCard(
                        onClick = onAddImage,
                        modifier = Modifier.aspectRatio(1f),
                    )
                }
            }

            items(images, key = { it.uid }) { image ->
                ProductImageCard(
                    image = image,
                    onClick = { onImageClick(image) },
                    onDelete = if (onDeleteImage != null) { { onDeleteImage(image) } } else null,
                    onSetPrimary = if (onSetPrimary != null) { { onSetPrimary(image) } } else null,
                    modifier = Modifier.aspectRatio(1f),
                )
            }
        }
    }
}

@Composable
private fun AddImageCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline)
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = stringResource(Res.string.prod_images_cd_add),
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.prod_images_cd_add),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ProductImageCard(
    image: ProductUploadImageListItem,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    onSetPrimary: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val hasActions = onDelete != null || onSetPrimary != null
    var showActions by remember { mutableStateOf(false) }

    ElevatedCard(
        onClick = {
            if (hasActions && !showActions) {
                showActions = true
            } else {
                showActions = false
                onClick()
            }
        },
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (image.isPrimary)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Image content
            val imageModel = when {
                !image.localPath.isNullOrBlank() -> "file://${image.localPath}"
                !image.synced -> null
                !image.thumbnailUrl.isNullOrBlank() -> ApiUrlBuilder.buildCompleteUrl(image.thumbnailUrl!!)
                !image.imageUrl.isNullOrBlank() -> ApiUrlBuilder.buildCompleteUrl(image.imageUrl!!)
                else -> null
            }

            if (imageModel != null) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = stringResource(Res.string.prod_images_cd_image),
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Primary badge
            if (image.isPrimary) {
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Text(stringResource(Res.string.prod_images_primary_badge), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            // Upload status badge
            if (image.uploadStatus != ProductImageStatus.COMPLETED) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = when (image.uploadStatus) {
                        ProductImageStatus.UPLOADING -> MaterialTheme.colorScheme.secondary
                        ProductImageStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                ) {
                    Text(
                        text = when (image.uploadStatus) {
                            ProductImageStatus.UPLOADING -> stringResource(Res.string.prod_images_status_uploading)
                            ProductImageStatus.FAILED -> stringResource(Res.string.prod_images_status_failed)
                            else -> stringResource(Res.string.prod_images_status_pending)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (image.uploadStatus) {
                            ProductImageStatus.UPLOADING -> MaterialTheme.colorScheme.onSecondary
                            ProductImageStatus.FAILED -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onTertiary
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }

            // Actions overlay
            if (showActions && hasActions) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        if (onSetPrimary != null) {
                            IconButton(onClick = { onSetPrimary(); showActions = false }) {
                                Icon(
                                    if (image.isPrimary) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = stringResource(Res.string.prod_images_set_primary_btn),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = { onDelete(); showActions = false }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(Res.string.prod_images_cd_delete),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
