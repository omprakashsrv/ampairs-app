package com.ampairs.imagesearch.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.imagesearch.generated.resources.Res
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_empty
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_matching
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_no_results
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_save
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_saving
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_searching
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_close
import ampairsapp.feature.imagesearch.generated.resources.img_bulk_title
import ampairsapp.feature.imagesearch.generated.resources.img_search_back_cd
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_accept
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_body
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_cancel
import ampairsapp.feature.imagesearch.generated.resources.img_search_disclaimer_title
import ampairsapp.feature.imagesearch.generated.resources.img_search_result_cd
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ampairs.imagesearch.domain.ImageResult
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import com.ampairs.imagesearch.BulkTarget
import org.jetbrains.compose.resources.stringResource

/**
 * Bulk auto-match screen. Scrapes candidate images for each target (sequentially, through one hidden
 * WebView, with a progress bar) and lets the user confirm/change one per row before saving all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkImageMatchScreen(
    entityType: String,
    targets: List<BulkTarget>,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BulkImageMatchViewModel = assistedMetroViewModel<BulkImageMatchViewModel, BulkImageMatchViewModel.Factory>(
        key = entityType + ":" + targets.size,
    ) { create(entityType, targets) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Local UI state: candidate being previewed full-size (double-click / long-press).
    var previewCandidate by remember { mutableStateOf<ImageResult?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is BulkImageMatchEvent.Finished -> onNavigateBack()
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(Res.string.img_bulk_title)) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.img_search_back_cd),
                    )
                }
            },
            actions = {
                TextButton(
                    onClick = viewModel::saveSelected,
                    enabled = !state.isProcessing && !state.isSaving && state.selectedCount > 0,
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.img_bulk_save, state.selectedCount))
                    }
                }
            },
        )

        if (state.isProcessing) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = stringResource(Res.string.img_bulk_matching, state.doneCount + 1, state.rows.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { if (state.rows.isEmpty()) 0f else state.doneCount.toFloat() / state.rows.size },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (state.rows.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(Res.string.img_bulk_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(state.rows, key = { it.entityUid }) { row ->
                        BulkRow(
                            row = row,
                            onSelect = { candidateIndex ->
                                viewModel.selectCandidate(state.rows.indexOf(row), candidateIndex)
                            },
                            onPreview = { candidate -> previewCandidate = candidate },
                        )
                        HorizontalDivider()
                    }
                }
            }

            // Hidden scrape engine (tiny + invisible + non-interactive); see ImageSearchWebView docs.
            ImageSearchWebView(
                url = state.currentSearchUrl ?: "",
                onResults = viewModel::onResultsFromWeb,
                onError = viewModel::onWebError,
                modifier = Modifier.size(1.dp).alpha(0f),
            )
        }
    }

    if (state.showDisclaimer) {
        DisclaimerDialogBulk(onAccept = viewModel::acceptDisclaimer, onCancel = onNavigateBack)
    }

    previewCandidate?.let { candidate ->
        CandidatePreviewDialog(candidate = candidate, onDismiss = { previewCandidate = null })
    }
}

@Composable
private fun BulkRow(row: BulkMatchRow, onSelect: (Int) -> Unit, onPreview: (ImageResult) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = row.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            when (row.status) {
                BulkMatchStatus.SEARCHING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        Text(
                            text = stringResource(Res.string.img_bulk_searching),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
                BulkMatchStatus.NO_RESULTS -> Text(
                    text = stringResource(Res.string.img_bulk_no_results),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Unit
            }
        }

        if (row.candidates.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                itemsIndexed(row.candidates) { index, candidate ->
                    CandidateThumb(
                        candidate = candidate,
                        selected = row.selectedIndex == index,
                        onClick = { onSelect(index) },
                        onPreview = { onPreview(candidate) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CandidateThumb(
    candidate: ImageResult,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: () -> Unit,
) {
    val model: Any = candidate.thumbnailBytes ?: candidate.thumbnailUrl
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    AsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current).data(model).build(),
        contentDescription = stringResource(Res.string.img_search_result_cd),
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .size(84.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(if (selected) 3.dp else 1.dp, borderColor, RoundedCornerShape(8.dp))
            // Single tap picks; double-click (desktop) or long-press (mobile) previews full-size.
            .combinedClickable(
                onClick = onClick,
                onDoubleClick = onPreview,
                onLongClick = onPreview,
            ),
    )
}

/** Full-size preview of a candidate image (prefers the full-res URL, falls back to the thumbnail). */
@Composable
private fun CandidatePreviewDialog(candidate: ImageResult, onDismiss: () -> Unit) {
    val model: Any = candidate.fullResUrl.ifBlank { null }
        ?: candidate.thumbnailBytes
        ?: candidate.thumbnailUrl
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current).data(model).build(),
                contentDescription = stringResource(Res.string.img_search_result_cd),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(Res.string.img_bulk_close),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun DisclaimerDialogBulk(onAccept: () -> Unit, onCancel: () -> Unit) {
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
