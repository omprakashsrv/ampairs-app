package com.ampairs.customer.ui.components.images

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CustomerImageManagementScreen(
    customerId: String,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    viewModel: CustomerImageViewModel = koinViewModel { parametersOf(customerId) }
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        CustomerImageHeader(
            imageCount = uiState.images.size,
            onSync = viewModel::syncImages,
            isLoading = uiState.isLoading,
            showSyncButton = uiState.syncError
        )

        // Error Display
        uiState.error?.let { errorMessage ->
            ErrorCard(
                error = errorMessage,
                onDismiss = viewModel::clearError,
                onRetry = viewModel::retryLastOperation
            )
        }

        // Main Content
        when {
            uiState.isLoading && uiState.images.isEmpty() -> {
                LoadingContent()
            }

            uiState.images.isEmpty() -> {
                EmptyStateContent(
                    onAddImage = if (readOnly) null else viewModel::pickSingleImage
                )
            }

            else -> {
                CustomerImageGrid(
                    images = uiState.images,
                    onAddImage = if (readOnly) null else viewModel::pickSingleImage,
                    onImageClick = { image ->
                        viewModel.showImageViewer(image.uid)
                    },
                    onDeleteImage = if (readOnly) null else { image ->
                        viewModel.deleteImage(image.uid)
                    },
                    onSetPrimary = if (readOnly) null else { image ->
                        viewModel.setPrimaryImage(image.uid)
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Upload Instructions (only show if not read-only)
        if (uiState.images.size < 10 && !readOnly) {
            UploadInstructionsCard()
        }
    }

    // Image Viewer Dialog
    if (uiState.showImageViewer && uiState.selectedImage != null) {
        CustomerImageViewer(
            image = uiState.selectedImage,
            onDismiss = viewModel::hideImageViewer,
            onDelete = { image ->
                viewModel.deleteImage(image.uid)
                viewModel.hideImageViewer()
            },
            onSetPrimary = { image ->
                viewModel.setPrimaryImage(image.uid)
            }
        )
    }

    // Upload Dialog
    if (uiState.showUploadDialog && uiState.uploadData != null) {
        CustomerImageUploadDialog(
            uploadData = uiState.uploadData,
            onDismiss = viewModel::hideUploadDialog,
            onUpload = viewModel::uploadImage,
            isUploading = uiState.isUploading
        )
    }
}

@Composable
private fun CustomerImageHeader(
    imageCount: Int,
    onSync: () -> Unit,
    isLoading: Boolean,
    showSyncButton: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Customer Images",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$imageCount images uploaded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // Only show sync button on error
            if (showSyncButton) {
                IconButton(
                    onClick = onSync,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry sync",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Error,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Dismiss")
                }

                TextButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("Retry")
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading images...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyStateContent(
    onAddImage: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CloudUpload,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (onAddImage == null) "No images" else "No images yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = if (onAddImage == null)
                    "Customer images are configured as read-only"
                else
                    "Upload customer images to help identify and personalize their profile",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (onAddImage != null) {
                Button(
                    onClick = onAddImage,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload First Image")
                }
            }
        }
    }
}

@Composable
private fun UploadInstructionsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Upload Guidelines",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            val guidelines = listOf(
                "Maximum 10 images per customer",
                "Supported formats: JPEG, PNG, WebP",
                "Maximum file size: 10MB per image",
                "Recommended resolution: 800x800 pixels",
                "Set one image as primary for customer lists"
            )

            guidelines.forEach { guideline ->
                Text(
                    text = "• $guideline",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}