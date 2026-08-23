package com.ampairs.imagesearch.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.imagesearch.generated.resources.Res
import ampairsapp.feature.imagesearch.generated.resources.img_search_back_cd
import ampairsapp.feature.imagesearch.generated.resources.img_search_cancel
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_accept
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_body
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_cancel
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_title
import ampairsapp.feature.imagesearch.generated.resources.img_search_downloading
import ampairsapp.feature.imagesearch.generated.resources.img_search_empty
import ampairsapp.feature.imagesearch.generated.resources.img_search_hint
import ampairsapp.feature.imagesearch.generated.resources.img_search_preview_title
import ampairsapp.feature.imagesearch.generated.resources.img_search_result_cd
import ampairsapp.feature.imagesearch.generated.resources.img_search_search
import ampairsapp.feature.imagesearch.generated.resources.img_search_searching
import ampairsapp.feature.imagesearch.generated.resources.img_search_set_primary
import ampairsapp.feature.imagesearch.generated.resources.img_search_title
import ampairsapp.feature.imagesearch.generated.resources.img_search_use_image
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ampairs.imagesearch.domain.ImageResult
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource

/**
 * Internet image-search picker. The user sees a clean chip + grid UI; a hidden [ImageSearchWebView]
 * scrapes results behind the (opaque) grid. Picking an image downloads it and writes it into the
 * existing file pipeline via [ImageSearchViewModel], then pops back.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ImageSearchScreen(
    entityType: String,
    entityUid: String,
    keywords: List<String>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageSearchViewModel = assistedMetroViewModel<ImageSearchViewModel, ImageSearchViewModel.Factory>(
        key = entityUid,
    ) { create(entityType, entityUid, keywords) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffectSaved(viewModel, onNavigateBack)

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.img_search_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.img_search_back_cd),
                    )
                }
            },
        )

        // Keyword chips — toggle to refine the query.
        if (state.keywords.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.keywords.forEachIndexed { index, keyword ->
                    FilterChip(
                        selected = keyword.enabled,
                        onClick = { viewModel.toggleKeyword(index) },
                        label = { Text(keyword.value) },
                    )
                }
            }
        }

        // Editable query + submit.
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            label = { Text(stringResource(Res.string.img_search_hint)) },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = viewModel::submitSearch) {
                    Icon(Icons.Default.Search, contentDescription = stringResource(Res.string.img_search_search))
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch() }),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Clean results UI — this is what the user sees.
            when {
                state.results.isEmpty() && state.isSearching -> CenteredMessage {
                    CircularProgressIndicator()
                    Text(
                        text = stringResource(Res.string.img_search_searching),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                state.results.isEmpty() -> CenteredMessage {
                    Text(
                        text = stringResource(Res.string.img_search_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.results, key = { it.id }) { result ->
                        ResultThumb(result = result, onClick = { viewModel.select(result) })
                    }
                }
            }

            // Hidden scrape engine — kept in-composition (a detached WebView won't load lazy content)
            // but tiny + invisible + non-interactive so it neither shows nor swallows taps. Its JS
            // auto-scrolls to pull in lazy results regardless of the 1.dp viewport. NOTE: on Desktop
            // (JavaFX) a 1.dp panel may not run reliably — see module docs; bump size there if needed.
            ImageSearchWebView(
                url = state.searchUrl ?: "",
                onResults = viewModel::onResultsFromWeb,
                onError = viewModel::onWebError,
                modifier = Modifier.size(1.dp).alpha(0f),
            )
        }
    }

    if (state.showDisclaimer) {
        DisclaimerDialog(
            onAccept = viewModel::acceptDisclaimer,
            onCancel = onNavigateBack,
        )
    }

    state.selected?.let { selected ->
        UseImageDialog(
            result = selected,
            isDownloading = state.isDownloading,
            onConfirm = viewModel::useSelected,
            onDismiss = viewModel::clearSelection,
        )
    }
}

@Composable
private fun LaunchedEffectSaved(viewModel: ImageSearchViewModel, onNavigateBack: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ImageSearchEvent.Saved -> onNavigateBack()
            }
        }
    }
}

@Composable
private fun ResultThumb(result: ImageResult, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val model: Any = result.thumbnailBytes ?: result.thumbnailUrl
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current).data(model).build(),
        contentDescription = stringResource(Res.string.img_search_result_cd),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
    )
}

@Composable
private fun CenteredMessage(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) { content() }
    }
}

@Composable
private fun DisclaimerDialog(onAccept: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.img_search_disclaimer_title)) },
        text = { Text(stringResource(Res.string.img_search_disclaimer_body)) },
        confirmButton = {
            TextButton(onClick = onAccept) { Text(stringResource(Res.string.img_search_disclaimer_accept)) }
        },
        dismissButton = {
            TextButton(onClick = onCancel) { Text(stringResource(Res.string.img_search_disclaimer_cancel)) }
        },
    )
}

@Composable
private fun UseImageDialog(
    result: ImageResult,
    isDownloading: Boolean,
    onConfirm: (isPrimary: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var isPrimary by remember { mutableStateOf(false) }
    val model: Any = result.thumbnailBytes ?: result.thumbnailUrl
    AlertDialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        title = { Text(stringResource(Res.string.img_search_preview_title)) },
        text = {
            Column {
                AsyncImage(
                    model = ImageRequest.Builder(LocalPlatformContext.current).data(model).build(),
                    contentDescription = stringResource(Res.string.img_search_result_cd),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(8.dp)),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(stringResource(Res.string.img_search_set_primary))
                    Switch(checked = isPrimary, onCheckedChange = { isPrimary = it }, enabled = !isDownloading)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(isPrimary) }, enabled = !isDownloading) {
                if (isDownloading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.img_search_downloading),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    Text(stringResource(Res.string.img_search_use_image))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDownloading) {
                Text(stringResource(Res.string.img_search_cancel))
            }
        },
    )
}
