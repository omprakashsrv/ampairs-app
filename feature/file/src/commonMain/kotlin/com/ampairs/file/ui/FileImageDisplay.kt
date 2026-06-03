package com.ampairs.file.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ampairs.file.api.FileUploadStatus

@Composable
fun FileImageDisplay(
    imageUrl: String?,
    localPath: String?,
    uploadStatus: FileUploadStatus,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    onPickImage: (() -> Unit)? = null,
    onRemoveImage: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(8.dp)
    val hasImage = !localPath.isNullOrBlank() || !imageUrl.isNullOrBlank()
    val imageSource: String? = when {
        !localPath.isNullOrBlank() && uploadStatus != FileUploadStatus.COMPLETED -> "file://$localPath"
        !imageUrl.isNullOrBlank() -> imageUrl
        else -> null
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .then(if (onPickImage != null && !hasImage) Modifier.clickable { onPickImage() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (imageSource != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(imageSource)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (uploadStatus == FileUploadStatus.UPLOADING) {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
            if (onRemoveImage != null) {
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove image",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        } else {
            Icon(
                Icons.Default.AddPhotoAlternate,
                contentDescription = "Add image",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }
    }
}
