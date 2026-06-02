package com.ampairs.product.ui.images

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ampairs.file.api.FileItem
import com.ampairs.file.api.FileUploadStatus
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_images_cd_add
import ampairsapp.feature.product.generated.resources.prod_images_cd_image
import ampairsapp.feature.product.generated.resources.prod_images_primary_badge
import ampairsapp.feature.product.generated.resources.prod_images_status_uploading
import ampairsapp.feature.product.generated.resources.prod_images_status_failed
import ampairsapp.feature.product.generated.resources.prod_images_status_pending
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductImageGrid(
    images: List<FileItem>,
    onAddImage: (() -> Unit)?,
    onImageClick: (FileItem) -> Unit,
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
    image: FileItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (image.isPrimary)
                MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageModel = when {
                !image.localPath.isNullOrBlank() -> "file://${image.localPath}"
                !image.synced -> null
                image.thumbnailUrl.isNotBlank() -> image.thumbnailUrl
                image.imageUrl.isNotBlank() -> image.imageUrl
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

            if (image.uploadStatus != FileUploadStatus.COMPLETED) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = when (image.uploadStatus) {
                        FileUploadStatus.UPLOADING -> MaterialTheme.colorScheme.secondary
                        FileUploadStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.tertiary
                    },
                ) {
                    Text(
                        text = when (image.uploadStatus) {
                            FileUploadStatus.UPLOADING -> stringResource(Res.string.prod_images_status_uploading)
                            FileUploadStatus.FAILED -> stringResource(Res.string.prod_images_status_failed)
                            else -> stringResource(Res.string.prod_images_status_pending)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = when (image.uploadStatus) {
                            FileUploadStatus.UPLOADING -> MaterialTheme.colorScheme.onSecondary
                            FileUploadStatus.FAILED -> MaterialTheme.colorScheme.onError
                            else -> MaterialTheme.colorScheme.onTertiary
                        },
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
