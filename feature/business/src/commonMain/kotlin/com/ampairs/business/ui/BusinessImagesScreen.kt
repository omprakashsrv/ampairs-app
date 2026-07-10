package com.ampairs.business.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ampairs.business.domain.BusinessImage
import com.ampairs.business.domain.BusinessImageType
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.business.ui.BusinessImagesViewModel
import com.ampairs.business.ui.components.BusinessScreenContent
import dev.zacsweers.metrox.viewmodel.metroViewModel

/**
 * Business Images Screen.
 * Manages business logo and gallery images with upload, view, and delete functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessImagesScreen(
    modifier: Modifier = Modifier,
    viewModel: BusinessImagesViewModel = metroViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Upload dialog state
    var showUploadDialog by remember { mutableStateOf(false) }

    // Edit dialog state
    var editingImage by remember { mutableStateOf<BusinessImage?>(null) }

    // Handle success messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    // Handle error messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        BusinessScreenContent(
            modifier = Modifier.padding(paddingValues),
            maxContentWidth = 720.dp,
            verticalSpacing = 24.dp,
        ) {
            Text(
                text = "Business Images",
                style = MaterialTheme.typography.headlineMedium
            )

            when {
                uiState.isLoading && uiState.business == null -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    // Logo Section - append cache buster to force reload after upload
                    val logoUrlWithCacheBuster = uiState.logoThumbnailUrl?.let { url ->
                        if (uiState.logoCacheBuster > 0) "$url?t=${uiState.logoCacheBuster}" else url
                    }
                    BusinessLogoSection(
                        logoUrl = logoUrlWithCacheBuster,
                        hasLogo = uiState.logoUrl != null,
                        isUploading = uiState.isUploadingLogo,
                        isDeleting = uiState.isDeletingLogo,
                        onUploadClick = { viewModel.pickAndUploadLogo() },
                        onDeleteClick = { viewModel.deleteLogo() }
                    )

                    HorizontalDivider()

                    // Gallery Section
                    BusinessGallerySection(
                        images = uiState.images,
                        isUploading = uiState.isUploadingImage,
                        isDeleting = uiState.isDeletingImage,
                        onAddClick = { showUploadDialog = true },
                        onEditClick = { image -> editingImage = image },
                        onSetPrimaryClick = { viewModel.setImageAsPrimary(it) },
                        onDeleteClick = { viewModel.deleteImage(it) }
                    )

                    // Bottom spacing
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Upload Image Dialog
    if (showUploadDialog) {
        ImageUploadDialog(
            onDismiss = { showUploadDialog = false },
            onUpload = { imageType, title, description ->
                showUploadDialog = false
                viewModel.pickAndUploadImage(imageType, title, description)
            }
        )
    }

    // Edit Image Dialog
    editingImage?.let { image ->
        ImageEditDialog(
            image = image,
            onDismiss = { editingImage = null },
            onSave = { title, description, imageType ->
                viewModel.updateImageMetadata(image.uid, title, description, imageType)
                editingImage = null
            }
        )
    }
}

/**
 * Business Logo Section
 */
@Composable
private fun BusinessLogoSection(
    logoUrl: String?,
    hasLogo: Boolean,
    isUploading: Boolean,
    isDeleting: Boolean,
    onUploadClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Business Logo",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Logo preview
            Box(
                modifier = Modifier
                    .size(128.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (hasLogo && logoUrl != null) {
                    AsyncImage(
                        model = logoUrl,
                        contentDescription = "Business Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Loading overlay
                if (isUploading || isDeleting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onUploadClick,
                    enabled = !isUploading && !isDeleting
                ) {
                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (hasLogo) "Change" else "Upload")
                }

                if (hasLogo) {
                    OutlinedButton(
                        onClick = onDeleteClick,
                        enabled = !isUploading && !isDeleting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Remove")
                    }
                }
            }

            // Helper text
            Text(
                text = "Max 10MB • JPEG, PNG, or WebP • 512x512 recommended",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Business Gallery Section
 */
@Composable
private fun BusinessGallerySection(
    images: List<BusinessImage>,
    isUploading: Boolean,
    isDeleting: Boolean,
    onAddClick: () -> Unit,
    onEditClick: (BusinessImage) -> Unit,
    onSetPrimaryClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Gallery Images",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "${images.size}/20 images",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalButton(
                onClick = onAddClick,
                enabled = !isUploading && images.size < 20
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Image")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (images.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No images yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onAddClick) {
                        Text("Add First Image")
                    }
                }
            }
        } else {
            // Image grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                userScrollEnabled = false
            ) {
                items(images, key = { it.uid }) { image ->
                    BusinessImageItem(
                        image = image,
                        isDeleting = isDeleting,
                        onEditClick = { onEditClick(image) },
                        onSetPrimaryClick = { onSetPrimaryClick(image.uid) },
                        onDeleteClick = { onDeleteClick(image.uid) }
                    )
                }
            }
        }
    }
}

/**
 * Individual gallery image item
 */
@Composable
private fun BusinessImageItem(
    image: BusinessImage,
    isDeleting: Boolean,
    onEditClick: () -> Unit,
    onSetPrimaryClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Image
        AsyncImage(
            model = ApiUrlBuilder.businessImageThumbnailUrl(image.uid),
            contentDescription = image.altText ?: image.title,
            modifier = Modifier
                .fillMaxSize()
                .clickable { showMenu = true },
            contentScale = ContentScale.Crop
        )

        // Type badge (show image type)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(4.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
        ) {
            Text(
                text = image.imageType.replace("_", " ").lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Primary badge
        if (image.isPrimary) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = "Primary",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Menu button
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(32.dp)
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }

        // Dropdown menu
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Edit") },
                onClick = {
                    showMenu = false
                    onEditClick()
                },
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            if (!image.isPrimary) {
                DropdownMenuItem(
                    text = { Text("Set as Primary") },
                    onClick = {
                        showMenu = false
                        onSetPrimaryClick()
                    },
                    leadingIcon = { Icon(Icons.Default.Star, null) }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                enabled = !isDeleting
            )
        }
    }
}

/**
 * Dialog for uploading a new image with type selection
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageUploadDialog(
    onDismiss: () -> Unit,
    onUpload: (BusinessImageType, String?, String?) -> Unit
) {
    var selectedType by remember { mutableStateOf(BusinessImageType.GALLERY) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Image") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Image Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BusinessImageType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(type.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() })
                                },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Title (optional)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description (optional)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onUpload(
                        selectedType,
                        title.takeIf { it.isNotBlank() },
                        description.takeIf { it.isNotBlank() }
                    )
                }
            ) {
                Text("Select Image")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for editing image metadata
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageEditDialog(
    image: BusinessImage,
    onDismiss: () -> Unit,
    onSave: (String?, String?, String?) -> Unit
) {
    val currentType = try {
        BusinessImageType.valueOf(image.imageType)
    } catch (_: Exception) {
        BusinessImageType.GALLERY
    }

    var selectedType by remember { mutableStateOf(currentType) }
    var title by remember { mutableStateOf(image.title ?: "") }
    var description by remember { mutableStateOf(image.description ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Image") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Image Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name.replace("_", " ").lowercase()
                            .replaceFirstChar { it.uppercase() },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Image Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        BusinessImageType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = {
                                    Text(type.name.replace("_", " ").lowercase()
                                        .replaceFirstChar { it.uppercase() })
                                },
                                onClick = {
                                    selectedType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        title.takeIf { it.isNotBlank() },
                        description.takeIf { it.isNotBlank() },
                        selectedType.name
                    )
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
