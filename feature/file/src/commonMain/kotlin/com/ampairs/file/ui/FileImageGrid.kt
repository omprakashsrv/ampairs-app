package com.ampairs.file.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ampairs.file.api.FileItem

@Composable
fun FileImageGrid(
    images: List<FileItem>,
    onPickImage: () -> Unit,
    onSetPrimary: (String) -> Unit,
    onDeleteImage: (String) -> Unit,
    modifier: Modifier = Modifier,
    itemSize: Dp = 100.dp,
    maxImages: Int = 10,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(images, key = { it.uid }) { image ->
            Box {
                FileImageDisplay(
                    imageUrl = image.imageUrl,
                    localPath = image.localPath,
                    uploadStatus = image.uploadStatus,
                    size = itemSize,
                    onRemoveImage = { onDeleteImage(image.uid) },
                )
                if (!image.isPrimary) {
                    IconButton(
                        onClick = { onSetPrimary(image.uid) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .size(24.dp),
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Set as primary",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                } else {
                    Badge(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Primary image",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        }
        if (images.size < maxImages) {
            item {
                FileImageDisplay(
                    imageUrl = null,
                    localPath = null,
                    uploadStatus = com.ampairs.file.api.FileUploadStatus.PENDING,
                    size = itemSize,
                    onPickImage = onPickImage,
                )
            }
        }
    }
}
