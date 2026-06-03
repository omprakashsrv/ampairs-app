package com.ampairs.tax.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.data.repository.TaxCodeRepository
import com.ampairs.tax.domain.model.MasterTaxCode
import com.ampairs.tax.domain.model.TaxCodeType
import com.ampairs.tax.domain.model.TaxCode
import com.ampairs.workspace.context.WorkspaceContextManager
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Tax Code Search ViewModel
 *
 * Features:
 * - Search master tax codes (server-side with network)
 * - View workspace subscribed codes (offline)
 * - Subscribe/unsubscribe codes
 * - Favorite management
 * - Usage count tracking
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class TaxCodeSearchViewModel(
    private val taxCodeRepository: TaxCodeRepository,
    private val workspaceContext: WorkspaceContextManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaxCodeSearchUiState())
    val uiState: StateFlow<TaxCodeSearchUiState> = _uiState.asStateFlow()

    // Search query with debounce
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    private val debouncedSearchQuery = _searchQuery
        .debounce(300)
        .onEach { query ->
            if (query.isNotBlank() && _uiState.value.selectedTab == SearchTab.MASTER_CODES) {
                searchMasterCodes(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, "")

    // Workspace codes (reactive from database)
    val workspaceCodes: StateFlow<List<TaxCode>> = combine(
        taxCodeRepository.observeWorkspaceTaxCodes(),
        _searchQuery
    ) { codes, query ->
        if (query.isBlank()) {
            codes
        } else {
            codes.filter { code ->
                code.code.contains(query, ignoreCase = true) ||
                    code.description.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        // Start observing debounced search
        viewModelScope.launch {
            debouncedSearchQuery.collect { }
        }

        // Load workspace codes on init
        loadWorkspaceCodes()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onTabChange(tab: SearchTab) {
        _uiState.update { it.copy(selectedTab = tab) }
        if (tab == SearchTab.MASTER_CODES && _searchQuery.value.isNotBlank()) {
            searchMasterCodes(_searchQuery.value)
        }
    }

    fun onCodeTypeFilterChange(codeType: TaxCodeType?) {
        _uiState.update { it.copy(selectedCodeType = codeType) }
        if (_searchQuery.value.isNotBlank() && _uiState.value.selectedTab == SearchTab.MASTER_CODES) {
            searchMasterCodes(_searchQuery.value)
        }
    }

    fun onCategoryFilterChange(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
        if (_searchQuery.value.isNotBlank() && _uiState.value.selectedTab == SearchTab.MASTER_CODES) {
            searchMasterCodes(_searchQuery.value)
        }
    }

    fun searchMasterCodes(query: String) {
        if (query.isBlank()) {
            _uiState.update { it.copy(masterCodes = emptyList(), isSearching = false, searchError = null) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchError = null) }

            val result = taxCodeRepository.searchMasterTaxCodes(
                query = query,
                countryCode = workspaceContext.getCurrentCountryCode() ?: "IN",
                codeType = _uiState.value.selectedCodeType?.name,
                category = _uiState.value.selectedCategory,
                page = 0,
                size = 50
            )

            result.fold(
                onSuccess = { masterCodes ->
                    _uiState.update {
                        it.copy(
                            masterCodes = masterCodes,
                            isSearching = false,
                            searchError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            masterCodes = emptyList(),
                            isSearching = false,
                            searchError = error.message ?: "Search failed"
                        )
                    }
                }
            )
        }
    }

    fun subscribeToCode(masterTaxCode: MasterTaxCode, isFavorite: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubscribing = true, subscribeError = null) }

            val result = taxCodeRepository.subscribeToTaxCode(
                masterTaxCodeId = masterTaxCode.id,
                customTaxRuleId = null,
                isFavorite = isFavorite,
                notes = null
            )

            result.fold(
                onSuccess = { workspaceCode ->
                    _uiState.update {
                        it.copy(
                            isSubscribing = false,
                            subscribeError = null,
                            lastSubscribedCode = workspaceCode
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isSubscribing = false,
                            subscribeError = error.message ?: "Subscription failed"
                        )
                    }
                }
            )
        }
    }

    fun unsubscribeFromCode(workspaceCodeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnsubscribing = true, unsubscribeError = null) }

            val result = taxCodeRepository.unsubscribeFromTaxCode(workspaceCodeId)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isUnsubscribing = false,
                            unsubscribeError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isUnsubscribing = false,
                            unsubscribeError = error.message ?: "Unsubscribe failed"
                        )
                    }
                }
            )
        }
    }

    fun toggleFavorite(workspaceCodeId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            taxCodeRepository.setFavorite(workspaceCodeId, isFavorite)
        }
    }

    fun incrementUsageCount(workspaceCodeId: String) {
        viewModelScope.launch {
            taxCodeRepository.incrementUsageCount(workspaceCodeId)
        }
    }

    private fun loadWorkspaceCodes() {
        viewModelScope.launch {
            // WorkspaceCodes are observed via StateFlow, no manual loading needed
        }
    }

    fun clearSearchError() {
        _uiState.update { it.copy(searchError = null) }
    }

    fun clearSubscribeError() {
        _uiState.update { it.copy(subscribeError = null) }
    }

    fun clearUnsubscribeError() {
        _uiState.update { it.copy(unsubscribeError = null) }
    }
}

/**
 * UI State for Tax Code Search
 */
data class TaxCodeSearchUiState(
    val selectedTab: SearchTab = SearchTab.MY_CODES,
    val masterCodes: List<MasterTaxCode> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,

    val isSubscribing: Boolean = false,
    val subscribeError: String? = null,
    val lastSubscribedCode: TaxCode? = null,

    val isUnsubscribing: Boolean = false,
    val unsubscribeError: String? = null,

    // Filters
    val selectedCodeType: TaxCodeType? = null,
    val selectedCategory: String? = null
)

/**
 * Search Tabs
 */
enum class SearchTab {
    MY_CODES,       // Workspace subscribed codes (offline)
    MASTER_CODES    // Server master codes (requires network)
}
