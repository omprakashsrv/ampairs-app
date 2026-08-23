package com.ampairs.imagesearch.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.file.api.FileEntityType
import com.ampairs.file.api.FileRepository
import com.ampairs.imagesearch.domain.ImageResult
import com.ampairs.imagesearch.domain.SearchKeyword
import com.ampairs.imagesearch.domain.toQuery
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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImageSearchUiState(
    val keywords: List<SearchKeyword> = emptyList(),
    val query: String = "",
    /** Drives the hidden WebView; non-null starts a load + scrape. */
    val searchUrl: String? = null,
    val isSearching: Boolean = false,
    val results: List<ImageResult> = emptyList(),
    val selected: ImageResult? = null,
    val isDownloading: Boolean = false,
    /** Copyright disclaimer must be accepted before the first search runs. */
    val showDisclaimer: Boolean = true,
    val error: String? = null,
)

sealed interface ImageSearchEvent {
    /** Image saved into the file pipeline — the screen should pop back. */
    data object Saved : ImageSearchEvent
}

@AssistedInject
class ImageSearchViewModel(
    @Assisted private val entityTypeName: String,
    @Assisted private val entityUid: String,
    @Assisted keywords: List<String>,
    private val fileRepository: FileRepository,
    private val downloader: ImageDownloader,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(entityTypeName: String, entityUid: String, keywords: List<String>): ImageSearchViewModel
    }

    private val entityType: FileEntityType =
        FileEntityType.entries.firstOrNull { it.name == entityTypeName } ?: FileEntityType.PRODUCT

    private val _uiState = MutableStateFlow(
        ImageSearchUiState(
            keywords = keywords.filter { it.isNotBlank() }.map { SearchKeyword(label = it, value = it) },
        ).let { it.copy(query = it.keywords.toQuery()) }
    )
    val uiState: StateFlow<ImageSearchUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ImageSearchEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<ImageSearchEvent> = _events.asSharedFlow()

    companion object {
        private const val TAG = "ImageSearchViewModel"
        private const val MAX_RESULTS = 120
    }

    /** User accepted the copyright disclaimer → run the first search. */
    fun acceptDisclaimer() {
        _uiState.update { it.copy(showDisclaimer = false) }
        runSearch()
    }

    fun toggleKeyword(index: Int) {
        _uiState.update { state ->
            val updated = state.keywords.mapIndexed { i, k -> if (i == index) k.copy(enabled = !k.enabled) else k }
            state.copy(keywords = updated, query = updated.toQuery())
        }
        runSearch()
    }

    fun updateQuery(text: String) {
        _uiState.update { it.copy(query = text) }
    }

    /** Explicit submit (keyboard action or search button). */
    fun submitSearch() = runSearch()

    private fun runSearch() {
        val query = _uiState.value.query.trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(searchUrl = null, results = emptyList(), isSearching = false) }
            return
        }
        _uiState.update {
            it.copy(
                searchUrl = ImageScraperJs.searchUrl(query),
                results = emptyList(),
                selected = null,
                isSearching = true,
                error = null,
            )
        }
    }

    /** Bridge callback from the hidden WebView (raw JSON batch). */
    fun onResultsFromWeb(payload: String) {
        val parsed = ImageResultParser.parse(payload)
        if (parsed.isEmpty()) return
        _uiState.update { state ->
            val merged = (state.results + parsed).distinctBy { it.id }.take(MAX_RESULTS)
            state.copy(results = merged, isSearching = false)
        }
    }

    fun onWebError(message: String) {
        ImageSearchLogger.w(TAG, "WebView error: $message")
        _uiState.update { it.copy(isSearching = false) }
    }

    fun select(result: ImageResult) {
        _uiState.update { it.copy(selected = result) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selected = null) }
    }

    /** Download the chosen image and hand it to the existing file pipeline. */
    fun useSelected(isPrimary: Boolean) {
        val result = _uiState.value.selected ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDownloading = true, error = null) }
            downloader.download(result).fold(
                onSuccess = { pickerResult ->
                    fileRepository.saveLocally(
                        entityType = entityType,
                        entityUid = entityUid,
                        result = pickerResult,
                        isPrimary = isPrimary,
                    ).fold(
                        onSuccess = {
                            syncService.markPendingPush(SyncEntity.FILE)
                            _uiState.update { it.copy(isDownloading = false, selected = null) }
                            _events.tryEmit(ImageSearchEvent.Saved)
                        },
                        onFailure = { error ->
                            ImageSearchLogger.e(TAG, "Failed to save downloaded image", error)
                            _uiState.update { it.copy(isDownloading = false, error = "Failed to save image: ${error.message}") }
                        },
                    )
                },
                onFailure = { error ->
                    ImageSearchLogger.e(TAG, "Failed to download image", error)
                    _uiState.update { it.copy(isDownloading = false, error = "Failed to download image: ${error.message}") }
                },
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
