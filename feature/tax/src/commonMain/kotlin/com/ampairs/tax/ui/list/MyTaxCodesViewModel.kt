package com.ampairs.tax.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val taxRuleRepository: com.ampairs.tax.data.repository.TaxRuleRepository,
    private val taxComponentRepository: com.ampairs.tax.data.repository.TaxComponentRepository
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
            TaxCodeSortBy.LAST_USED -> filteredCodes.sortedByDescending { it.lastUsedAt ?: 0L }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadTaxCodes()
        // Auto-pull from server on open so subscribed/imported codes appear without a manual
        // Refresh. Tax is off the central-sync pattern, so it pulls its own cluster here.
        refreshFromServer()
    }

    /** Silent pull of codes + rules + components on screen open (errors don't surface as UI errors). */
    private fun refreshFromServer() {
        viewModelScope.launch {
            taxCodeRepository.syncWorkspaceTaxCodes()
            taxRuleRepository.syncTaxRules()
            taxComponentRepository.syncWorkspaceComponents()
        }
    }

    fun loadTaxCodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Codes are loaded reactively via Flow
            kotlinx.coroutines.delay(500) // Small delay to show loading state

            _uiState.update { it.copy(isLoading = false) }
        }
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

    fun syncTaxCodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, syncSuccessMessage = null) }

            // Sync tax codes from server
            val codesResult = taxCodeRepository.syncWorkspaceTaxCodes()

            // Sync tax rules from server
            val rulesResult = taxRuleRepository.syncTaxRules()

            // Sync workspace components from server (needed for tax calculations)
            val componentsResult = taxComponentRepository.syncWorkspaceComponents()

            // Combine results
            if (codesResult.isSuccess && rulesResult.isSuccess && componentsResult.isSuccess) {
                val codesCount = codesResult.getOrNull() ?: 0
                val rulesCount = rulesResult.getOrNull() ?: 0
                val componentsCount = componentsResult.getOrNull() ?: 0

                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncSuccessMessage = if (codesCount > 0 || rulesCount > 0 || componentsCount > 0) {
                            buildString {
                                if (codesCount > 0) {
                                    append("Synced $codesCount tax code${if (codesCount != 1) "s" else ""}")
                                }
                                if (rulesCount > 0) {
                                    if (codesCount > 0) append(", ")
                                    append("$rulesCount rule${if (rulesCount != 1) "s" else ""}")
                                }
                                if (componentsCount > 0) {
                                    if (codesCount > 0 || rulesCount > 0) append(", ")
                                    append("$componentsCount component${if (componentsCount != 1) "s" else ""}")
                                }
                            }
                        } else {
                            "Already up to date"
                        }
                    )
                }
            } else {
                val error = codesResult.exceptionOrNull() ?: rulesResult.exceptionOrNull() ?: componentsResult.exceptionOrNull()
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = error?.message ?: "Sync failed"
                    )
                }
            }
        }
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
