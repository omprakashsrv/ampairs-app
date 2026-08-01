package com.ampairs.ecom.ui.management

import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_access_mode
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_access_public
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_access_public_desc
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_access_restricted
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_access_restricted_desc
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_banner
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_branding_after_create
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_create
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_create_intro
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_description
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_intro
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_logo
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_manage_users
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_name
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_preview
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_publish
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_retry
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_save
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_section_access
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_section_branding
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_section_details
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_slug
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_slug_helper
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_status
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_status_draft
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_status_published
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_status_unknown
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_status_unpublished
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_title
import ampairsapp.feature.ecom.generated.resources.storefront_mgmt_unpublish
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ampairs.ecom.api.model.StorefrontAccessMode
import com.ampairs.ecom.api.model.StorefrontStatus
import com.ampairs.file.api.FileUploadStatus
import com.ampairs.file.ui.FileImageDisplay
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Merchant-side storefront configuration screen. Creates the workspace's storefront if none
 * exists, otherwise edits branding + access mode and publishes/unpublishes it. The "Preview store"
 * action opens the live customer storefront for this slug via [onPreview].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorefrontManagementScreen(
    onPreview: (slug: String) -> Unit,
    onManageUsers: () -> Unit,
    onBack: () -> Unit,
    viewModel: StorefrontManagementViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is StorefrontManagementEvent.Message -> snackbar.showSnackbar(event.text)
                is StorefrontManagementEvent.OpenPreview -> onPreview(event.slug)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.storefront_mgmt_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.loadError != null -> LoadError(message = state.loadError!!, onRetry = viewModel::load)
                else -> StorefrontForm(state = state, viewModel = viewModel, onManageUsers = onManageUsers)
            }
        }
    }
}

@Composable
private fun LoadError(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text(stringResource(Res.string.storefront_mgmt_retry)) }
    }
}

@Composable
private fun StorefrontForm(
    state: StorefrontManagementUiState,
    viewModel: StorefrontManagementViewModel,
    onManageUsers: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(if (state.exists) Res.string.storefront_mgmt_intro else Res.string.storefront_mgmt_create_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Status (only once a storefront exists)
        if (state.exists) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(Res.string.storefront_mgmt_status),
                    style = MaterialTheme.typography.labelLarge,
                )
                AssistChip(onClick = {}, label = { Text(stringResource(state.status.labelRes())) })
            }
        }

        SectionLabel(Res.string.storefront_mgmt_section_details)

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(Res.string.storefront_mgmt_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.slug,
            onValueChange = viewModel::onSlugChange,
            label = { Text(stringResource(Res.string.storefront_mgmt_slug)) },
            singleLine = true,
            enabled = state.slugEditable,
            supportingText = { Text(stringResource(Res.string.storefront_mgmt_slug_helper)) },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = viewModel::onDescriptionChange,
            label = { Text(stringResource(Res.string.storefront_mgmt_description)) },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionLabel(Res.string.storefront_mgmt_section_branding)

        if (state.exists) {
            // Logo
            Text(stringResource(Res.string.storefront_mgmt_logo), style = MaterialTheme.typography.bodyMedium)
            FileImageDisplay(
                imageUrl = state.logoFile?.imageUrl,
                localPath = state.logoFile?.localPath,
                uploadStatus = state.logoFile?.uploadStatus ?: FileUploadStatus.PENDING,
                size = 110.dp,
                onPickImage = viewModel::pickLogo,
                onRemoveImage = state.logoFile?.let { { viewModel.removeLogo() } },
            )

            // Banner
            Text(stringResource(Res.string.storefront_mgmt_banner), style = MaterialTheme.typography.bodyMedium)
            FileImageDisplay(
                imageUrl = state.bannerFile?.imageUrl,
                localPath = state.bannerFile?.localPath,
                uploadStatus = state.bannerFile?.uploadStatus ?: FileUploadStatus.PENDING,
                size = 110.dp,
                onPickImage = viewModel::pickBanner,
                onRemoveImage = state.bannerFile?.let { { viewModel.removeBanner() } },
            )
        } else {
            Text(
                text = stringResource(Res.string.storefront_mgmt_branding_after_create),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Access mode is only meaningful once the storefront exists (update-only field server-side).
        if (state.exists) {
            SectionLabel(Res.string.storefront_mgmt_section_access)
            Text(
                text = stringResource(Res.string.storefront_mgmt_access_mode),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.accessMode == StorefrontAccessMode.PUBLIC,
                    onClick = { viewModel.onAccessModeChange(StorefrontAccessMode.PUBLIC) },
                    label = { Text(stringResource(Res.string.storefront_mgmt_access_public)) },
                )
                FilterChip(
                    selected = state.accessMode == StorefrontAccessMode.RESTRICTED,
                    onClick = { viewModel.onAccessModeChange(StorefrontAccessMode.RESTRICTED) },
                    label = { Text(stringResource(Res.string.storefront_mgmt_access_restricted)) },
                )
            }
            Text(
                text = stringResource(
                    if (state.accessMode == StorefrontAccessMode.PUBLIC)
                        Res.string.storefront_mgmt_access_public_desc
                    else
                        Res.string.storefront_mgmt_access_restricted_desc
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = viewModel::save,
            enabled = !state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(if (state.exists) Res.string.storefront_mgmt_save else Res.string.storefront_mgmt_create))
        }

        if (state.exists) {
            OutlinedButton(
                onClick = { if (state.isPublished) viewModel.unpublish() else viewModel.publish() },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(if (state.isPublished) Res.string.storefront_mgmt_unpublish else Res.string.storefront_mgmt_publish))
            }

            if (state.canPreview) {
                TextButton(
                    onClick = viewModel::preview,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.storefront_mgmt_preview))
                }
            }

            TextButton(
                onClick = onManageUsers,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(Res.string.storefront_mgmt_manage_users))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SectionLabel(res: StringResource) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun StorefrontStatus.labelRes(): StringResource = when (this) {
    StorefrontStatus.PUBLISHED -> Res.string.storefront_mgmt_status_published
    StorefrontStatus.DRAFT -> Res.string.storefront_mgmt_status_draft
    StorefrontStatus.UNPUBLISHED -> Res.string.storefront_mgmt_status_unpublished
    StorefrontStatus.UNKNOWN -> Res.string.storefront_mgmt_status_unknown
}
