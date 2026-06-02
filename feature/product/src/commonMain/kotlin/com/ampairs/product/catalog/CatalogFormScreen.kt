package com.ampairs.product.catalog

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.product.generated.resources.Res
import ampairsapp.feature.product.generated.resources.prod_catalog_add_brand
import ampairsapp.feature.product.generated.resources.prod_catalog_add_category
import ampairsapp.feature.product.generated.resources.prod_catalog_add_group
import ampairsapp.feature.product.generated.resources.prod_catalog_add_sub_category
import ampairsapp.feature.product.generated.resources.prod_catalog_edit_brand
import ampairsapp.feature.product.generated.resources.prod_catalog_edit_category
import ampairsapp.feature.product.generated.resources.prod_catalog_edit_group
import ampairsapp.feature.product.generated.resources.prod_catalog_edit_sub_category
import ampairsapp.feature.product.generated.resources.prod_catalog_form_active_label
import ampairsapp.feature.product.generated.resources.prod_catalog_form_change_image
import ampairsapp.feature.product.generated.resources.prod_catalog_form_image_label
import ampairsapp.feature.product.generated.resources.prod_catalog_form_name_error
import ampairsapp.feature.product.generated.resources.prod_catalog_form_name_label
import ampairsapp.feature.product.generated.resources.prod_catalog_form_remove_image
import ampairsapp.feature.product.generated.resources.prod_catalog_form_save
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogFormScreen(
    catalogType: ProductCatalogType,
    itemId: String?,
    onSaveSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CatalogFormViewModel = assistedMetroViewModel<CatalogFormViewModel, CatalogFormViewModel.Factory>(
        key = "${catalogType.name}_${itemId ?: "new"}"
    ) { create(catalogType, itemId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CatalogFormEvent.SaveSuccess -> onSaveSuccess()
                is CatalogFormEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val title = stringResource(
        if (viewModel.isEditing) when (catalogType) {
            ProductCatalogType.BRANDS -> Res.string.prod_catalog_edit_brand
            ProductCatalogType.CATEGORIES -> Res.string.prod_catalog_edit_category
            ProductCatalogType.SUB_CATEGORIES -> Res.string.prod_catalog_edit_sub_category
            ProductCatalogType.GROUPS -> Res.string.prod_catalog_edit_group
        } else when (catalogType) {
            ProductCatalogType.BRANDS -> Res.string.prod_catalog_add_brand
            ProductCatalogType.CATEGORIES -> Res.string.prod_catalog_add_category
            ProductCatalogType.SUB_CATEGORIES -> Res.string.prod_catalog_add_sub_category
            ProductCatalogType.GROUPS -> Res.string.prod_catalog_add_group
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onSaveSuccess) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = { Text(stringResource(Res.string.prod_catalog_form_name_label)) },
                isError = state.nameError,
                supportingText = if (state.nameError) {
                    { Text(stringResource(Res.string.prod_catalog_form_name_error)) }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (viewModel.isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.prod_catalog_form_active_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Switch(
                        checked = state.active,
                        onCheckedChange = viewModel::onActiveToggle,
                    )
                }
            }

            CatalogImageSection(
                currentImageUrl = state.currentImageUrl,
                pendingImage = state.pendingImage,
                onPickImage = viewModel::pickImage,
                onRemoveImage = viewModel::removeImage,
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = viewModel::save,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(stringResource(Res.string.prod_catalog_form_save))
                }
            }
        }
    }
}

@Composable
private fun CatalogImageSection(
    currentImageUrl: String?,
    pendingImage: com.ampairs.file.api.FilePickerResult?,
    onPickImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    val context = LocalPlatformContext.current
    val hasImage = pendingImage != null || currentImageUrl != null

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(Res.string.prod_catalog_form_image_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (hasImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(8.dp)
                    ),
            ) {
                val imageModel = when {
                    pendingImage != null -> ImageRequest.Builder(context)
                        .data(pendingImage.imageData)
                        .build()
                    currentImageUrl != null -> ImageRequest.Builder(context)
                        .data(currentImageUrl)
                        .build()
                    else -> null
                }
                if (imageModel != null) {
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(28.dp),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(Res.string.prod_catalog_form_remove_image),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                )
                Text(stringResource(Res.string.prod_catalog_form_change_image))
            }
        } else {
            OutlinedButton(onClick = onPickImage, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                )
                Text(stringResource(Res.string.prod_catalog_form_image_label))
            }
        }
    }
}
