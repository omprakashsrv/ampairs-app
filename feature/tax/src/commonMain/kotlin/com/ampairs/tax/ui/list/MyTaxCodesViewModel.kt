package com.ampairs.tax.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * My Tax Codes ViewModel
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class MyTaxCodesViewModel(
    private val taxCodeRepository: TaxCodeRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyTaxCodesUiState())
    val uiState: StateFlow<MyTaxCodesUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Reactive tax codes flow with filters
    @OptIn(FlowPreview::class)
    val taxCodes: StateFlow<List<TaxCode>> = combine(
        taxCodeRepository.observeWorkspaceTaxCodes(),
        _searchQuery,
        _uiState
    ) { codes, query, state ->
        var filteredCodes = codes

        // Apply search filter
        if (query.isNotBlank()) {
            filteredCodes = filteredCodes.filter { code ->
                code.code.contains(query, ignoreCase = true) ||
                    code.description.contains(query, ignoreCase = true) ||
                    code.shortDescription.contains(query, ignoreCase = true)
            }
        }

        // Apply favorites filter
        if (state.showFavoritesOnly) {
            filteredCodes = filteredCodes.filter { it.isFavorite }
        }

        // Apply sorting
        when (state.sortBy) {
            TaxCodeSortBy.CODE -> filteredCodes.sortedBy { it.code }
            TaxCodeSortBy.USAGE_COUNT -> filteredCodes.sortedByDescending { it.usageCount }
            TaxCodeSortBy.RECENTLY_ADDED -> filteredCodes.sortedByDescending { it.addedAt }
            TaxCodeSortBy.LAST_USED -> filteredCodes.sortedByDescending { it.lastUsedAt ?: kotlin.time.Instant.fromEpochMilliseconds(0) }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadTaxCodes()
        // Drive the spinner from central sync state, not from a coroutine's lifetime.
        syncService.observeEntity(SyncEntity.TAX)
            .onEach { state -> _uiState.update { it.copy(isSyncing = state?.status is SyncStatus.Syncing) } }
            .launchIn(viewModelScope)
        // Initial pull on open — TaxSyncDelegate pulls the codes/rules/components cluster.
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.TAX))
    }

    fun loadTaxCodes() {
        // Codes are loaded reactively via the DAO Flow (see init); nothing to wait on here.
        _uiState.update { it.copy(isLoading = false, errorMessage = null) }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearchMode() {
        _uiState.update { it.copy(isSearchMode = !it.isSearchMode) }
        if (!_uiState.value.isSearchMode) {
            _searchQuery.value = ""
        }
    }

    fun toggleFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun onSortByChange(sortBy: TaxCodeSortBy) {
        _uiState.update { it.copy(sortBy = sortBy) }
    }

    fun toggleFavorite(codeId: String) {
        viewModelScope.launch {
            val code = taxCodeRepository.getById(codeId)
            if (code != null) {
                taxCodeRepository.setFavorite(codeId, !code.isFavorite)
            }
        }
    }

    fun showUnsubscribeDialog(taxCode: TaxCode) {
        _uiState.update { it.copy(codeToUnsubscribe = taxCode) }
    }

    fun hideUnsubscribeDialog() {
        _uiState.update { it.copy(codeToUnsubscribe = null) }
    }

    fun unsubscribeFromCode() {
        val code = _uiState.value.codeToUnsubscribe ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isUnsubscribing = true) }

            val result = taxCodeRepository.unsubscribeFromTaxCode(code.id)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUnsubscribing = false,
                            codeToUnsubscribe = null,
                            syncSuccessMessage = "Unsubscribed from ${code.code}"
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUnsubscribing = false,
                            codeToUnsubscribe = null,
                            errorMessage = error.message ?: "Failed to unsubscribe"
                        )
                    }
                }
            )
        }
    }

    /** Manual refresh — full push + pull of the tax cluster via CentralSyncService. */
    fun syncTaxCodes() {
        syncService.emit(SyncEvent.TriggerFullSync(SyncEntity.TAX))
    }

    fun clearSyncMessage() {
        _uiState.update { it.copy(syncSuccessMessage = null) }
    }
}

/**
 * UI State for My Tax Codes
 */
data class MyTaxCodesUiState(
    val isLoading: Boolean = false,
    val isSearchMode: Boolean = false,
    val showFavoritesOnly: Boolean = false,
    val sortBy: TaxCodeSortBy = TaxCodeSortBy.USAGE_COUNT,
    val codeToUnsubscribe: TaxCode? = null,
    val isUnsubscribing: Boolean = false,
    val isSyncing: Boolean = false,
    val syncSuccessMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * Sort options for tax codes
 */
enum class TaxCodeSortBy {
    CODE,
    USAGE_COUNT,
    RECENTLY_ADDED,
    LAST_USED
}
