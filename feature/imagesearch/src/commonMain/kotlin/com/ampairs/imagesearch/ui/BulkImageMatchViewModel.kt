package com.ampairs.imagesearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.config.AppPreferencesDataStore
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileRepository
import com.ampairs.imagesearch.BulkTarget
import com.ampairs.imagesearch.domain.ImageResult
import com.ampairs.imagesearch.download.ImageDownloader
import com.ampairs.imagesearch.scrape.ImageResultParser
import com.ampairs.imagesearch.scrape.ImageScraperJs
import com.ampairs.imagesearch.util.ImageSearchLogger
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BulkMatchStatus { PENDING, SEARCHING, DONE, NO_RESULTS }

data class BulkMatchRow(
    val entityUid: String,
    val name: String,
    val keywords: List<String>,
    val status: BulkMatchStatus = BulkMatchStatus.PENDING,
    val candidates: List<ImageResult> = emptyList(),
    /** Index into [candidates] the user picked; null = skip this row. Pre-set to 0 (top match). */
    val selectedIndex: Int? = null,
)

data class BulkImageMatchUiState(
    val rows: List<BulkMatchRow> = emptyList(),
    /** Row currently being scraped; drives the single hidden WebView via [currentSearchUrl]. */
    val currentIndex: Int = -1,
    val currentSearchUrl: String? = null,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val savedCount: Int = 0,
    val showDisclaimer: Boolean = false,
    val error: String? = null,
) {
    val selectedCount: Int get() = rows.count { it.selectedIndex != null && it.candidates.isNotEmpty() }
    val doneCount: Int get() = rows.count { it.status == BulkMatchStatus.DONE || it.status == BulkMatchStatus.NO_RESULTS }
}

sealed interface BulkImageMatchEvent {
    /** All selected images saved — the screen should pop back. */
    data class Finished(val savedCount: Int) : BulkImageMatchEvent
}

@AssistedInject
class BulkImageMatchViewModel(
    @Assisted private val entityTypeName: String,
    @Assisted private val targets: List<BulkTarget>,
    private val fileRepository: FileRepository,
    private val downloader: ImageDownloader,
    private val syncService: CentralSyncService,
    private val appPreferences: AppPreferencesDataStore,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(entityTypeName: String, targets: List<BulkTarget>): BulkImageMatchViewModel
    }

    private val entityType: FileEntityType =
        FileEntityType.entries.firstOrNull { it.name == entityTypeName } ?: FileEntityType.PRODUCT

    private val _uiState = MutableStateFlow(
        BulkImageMatchUiState(
            rows = targets.map { BulkMatchRow(entityUid = it.entityUid, name = it.name, keywords = it.keywords) },
        )
    )
    val uiState: StateFlow<BulkImageMatchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BulkImageMatchEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BulkImageMatchEvent> = _events.asSharedFlow()

    companion object {
        private const val TAG = "BulkImageMatchViewModel"
        private const val MAX_CANDIDATES = 5
        /** How long to let the WebView scrape each row before finalizing and moving on. */
        private const val SEARCH_WINDOW_MS = 5_000L
    }

    init {
        viewModelScope.launch {
            val consented = appPreferences.getImageSearchConsent().first()
            if (consented) startProcessing() else _uiState.update { it.copy(showDisclaimer = true) }
        }
    }

    fun acceptDisclaimer() {
        _uiState.update { it.copy(showDisclaimer = false) }
        viewModelScope.launch { appPreferences.setImageSearchConsent(true) }
        startProcessing()
    }

    /** Sequentially scrape each row through the single hidden WebView (bounded per-row window). */
    private fun startProcessing() {
        if (_uiState.value.isProcessing) return
        _uiState.update { it.copy(isProcessing = true, error = null) }
        viewModelScope.launch {
            val rows = _uiState.value.rows
            for (i in rows.indices) {
                val query = rows[i].keywords.filter { it.isNotBlank() }.joinToString(" ").ifBlank { rows[i].name }
                _uiState.update { state ->
                    state.copy(
                        currentIndex = i,
                        currentSearchUrl = ImageScraperJs.searchUrl(query),
                        rows = state.rows.mapIndexed { idx, r -> if (idx == i) r.copy(status = BulkMatchStatus.SEARCHING) else r },
                    )
                }
                delay(SEARCH_WINDOW_MS) // onResultsFromWeb fills candidates for row i during this window
                _uiState.update { state ->
                    state.copy(
                        rows = state.rows.mapIndexed { idx, r ->
                            if (idx != i) r else r.copy(
                                status = if (r.candidates.isEmpty()) BulkMatchStatus.NO_RESULTS else BulkMatchStatus.DONE,
                                selectedIndex = if (r.candidates.isEmpty()) null else 0,
                            )
                        },
                    )
                }
            }
            _uiState.update { it.copy(isProcessing = false, currentIndex = -1, currentSearchUrl = null) }
        }
    }

    /** Bridge callback — append scraped candidates to the row currently being searched. */
    fun onResultsFromWeb(payload: String) {
        val parsed = ImageResultParser.parse(payload)
        if (parsed.isEmpty()) return
        _uiState.update { state ->
            val i = state.currentIndex
            if (i < 0 || i >= state.rows.size) return@update state
            val row = state.rows[i]
            if (row.status != BulkMatchStatus.SEARCHING) return@update state
            val merged = (row.candidates + parsed).distinctBy { it.id }.take(MAX_CANDIDATES)
            state.copy(rows = state.rows.mapIndexed { idx, r -> if (idx == i) r.copy(candidates = merged) else r })
        }
    }

    fun onWebError(message: String) {
        ImageSearchLogger.w(TAG, "WebView error: $message")
    }

    fun selectCandidate(rowIndex: Int, candidateIndex: Int) {
        _uiState.update { state ->
            state.copy(rows = state.rows.mapIndexed { idx, r ->
                if (idx == rowIndex) r.copy(selectedIndex = if (r.selectedIndex == candidateIndex) null else candidateIndex) else r
            })
        }
    }

    /** Download + save the selected image for every row that has one (as the primary image). */
    fun saveSelected() {
        if (_uiState.value.isSaving || _uiState.value.isProcessing) return
        _uiState.update { it.copy(isSaving = true, error = null) }
        viewModelScope.launch {
            var saved = 0
            _uiState.value.rows.forEach { row ->
                val idx = row.selectedIndex ?: return@forEach
                val candidate = row.candidates.getOrNull(idx) ?: return@forEach
                val download = downloader.download(candidate)
                val pickerResult = download.getOrNull()
                if (pickerResult == null) {
                    ImageSearchLogger.w(TAG, "Download failed for ${row.entityUid}", download.exceptionOrNull())
                    return@forEach
                }
                fileRepository.saveLocally(
                    entityType = entityType,
                    entityUid = row.entityUid,
                    result = pickerResult,
                    isPrimary = true,
                ).onSuccess { saved++ }
                    .onFailure { ImageSearchLogger.e(TAG, "Save failed for ${row.entityUid}", it) }
            }
            if (saved > 0) syncService.markPendingPush(SyncEntity.FILE)
            _uiState.update { it.copy(isSaving = false, savedCount = saved) }
            _events.tryEmit(BulkImageMatchEvent.Finished(saved))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
