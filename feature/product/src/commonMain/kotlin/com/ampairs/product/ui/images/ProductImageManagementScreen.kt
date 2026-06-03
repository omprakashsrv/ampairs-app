package com.ampairs.product.ui.images

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_images_count
import ampairsapp.feature.product.generated.resources.prod_images_dismiss
import ampairsapp.feature.product.generated.resources.prod_images_empty
import ampairsapp.feature.product.generated.resources.prod_images_empty_desc
import ampairsapp.feature.product.generated.resources.prod_images_empty_readonly
import ampairsapp.feature.product.generated.resources.prod_images_error
import ampairsapp.feature.product.generated.resources.prod_images_guideline_formats
import ampairsapp.feature.product.generated.resources.prod_images_guideline_max_count
import ampairsapp.feature.product.generated.resources.prod_images_guideline_primary
import ampairsapp.feature.product.generated.resources.prod_images_guideline_resolution
import ampairsapp.feature.product.generated.resources.prod_images_guideline_size
import ampairsapp.feature.product.generated.resources.prod_images_guidelines_title
import ampairsapp.feature.product.generated.resources.prod_images_loading
import ampairsapp.feature.product.generated.resources.prod_images_readonly_desc
import ampairsapp.feature.product.generated.resources.prod_images_retry
import ampairsapp.feature.product.generated.resources.prod_images_retry_sync_cd
import ampairsapp.feature.product.generated.resources.prod_images_title
import ampairsapp.feature.product.generated.resources.prod_images_upload_first
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProductImageManagementScreen(
    productId: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    viewModel: ProductImageViewModel,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ProductImageHeader(
            imageCount = uiState.images.size,
            onSync = viewModel::syncImages,
            isLoading = uiState.isLoading || uiState.isRefreshing,
            showSyncButton = uiState.syncError,
        )

        uiState.error?.let { errorMessage ->
            ProductImageErrorCard(
                error = errorMessage,
                onDismiss = viewModel::clearError,
                onRetry = viewModel::syncImages,
            )
        }

        when {
            uiState.isLoading && uiState.images.isEmpty() -> {
                ProductImageLoadingContent()
            }

            uiState.images.isEmpty() -> {
                ProductImageEmptyState(
                    onAddImage = if (readOnly) null else viewModel::pickSingleImage,
                )
            }

            else -> {
                ProductImageGrid(
                    images = uiState.images,
                    onAddImage = if (readOnly) null else viewModel::pickSingleImage,
                    onImageClick = { image -> viewModel.showImageViewer(image.uid) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (uiState.images.size < 10 && !readOnly) {
            ProductImageGuidelinesCard()
        }
    }

    if (uiState.showImageViewer && uiState.selectedImage != null) {
        ProductImageViewer(
            image = uiState.selectedImage,
            onDismiss = viewModel::hideImageViewer,
            onDelete = if (readOnly) null else { image -> viewModel.deleteImage(image.uid); viewModel.hideImageViewer() },
            onSetPrimary = if (readOnly) null else { image -> viewModel.setPrimaryImage(image.uid) },
        )
    }

    if (uiState.showUploadDialog && uiState.uploadData != null) {
        ProductImageUploadDialog(
            uploadData = uiState.uploadData,
            onDismiss = viewModel::hideUploadDialog,
            onUpload = viewModel::uploadImage,
            isUploading = uiState.isUploading,
        )
    }
}

@Composable
private fun ProductImageHeader(
    imageCount: Int,
    onSync: () -> Unit,
    isLoading: Boolean,
    showSyncButton: Boolean,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = stringResource(Res.string.prod_images_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = stringResource(Res.string.prod_images_count, imageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            if (showSyncButton) {
                IconButton(onClick = onSync, enabled = !isLoading) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = stringResource(Res.string.prod_images_retry_sync_cd),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductImageErrorCard(
    error: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                Text(
                    text = stringResource(Res.string.prod_images_error),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                ) {
                    Text(stringResource(Res.string.prod_images_dismiss))
                }
                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onErrorContainer),
                ) {
                    Text(stringResource(Res.string.prod_images_retry))
                }
            }
        }
    }
}

@Composable
private fun ProductImageLoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.prod_images_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProductImageEmptyState(
    onAddImage: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (onAddImage == null) stringResource(Res.string.prod_images_empty_readonly)
                       else stringResource(Res.string.prod_images_empty),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (onAddImage == null) stringResource(Res.string.prod_images_readonly_desc)
                       else stringResource(Res.string.prod_images_empty_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onAddImage != null) {
                Button(onClick = onAddImage, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.prod_images_upload_first))
                }
            }
        }
    }
}

@Composable
private fun ProductImageGuidelinesCard(modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(Res.string.prod_images_guidelines_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            listOf(
                stringResource(Res.string.prod_images_guideline_max_count),
                stringResource(Res.string.prod_images_guideline_formats),
                stringResource(Res.string.prod_images_guideline_size),
                stringResource(Res.string.prod_images_guideline_resolution),
                stringResource(Res.string.prod_images_guideline_primary),
            ).forEach { guideline ->
                Text(
                    text = "• $guideline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
