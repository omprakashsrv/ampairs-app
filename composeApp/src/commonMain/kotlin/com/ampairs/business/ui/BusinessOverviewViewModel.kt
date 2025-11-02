package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessOverview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Business Overview Screen.
 * Manages loading and displaying business overview dashboard.
 */
class BusinessOverviewViewModel(
    private val repository: BusinessRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessOverviewUiState())
    val uiState: StateFlow<BusinessOverviewUiState> = _uiState.asStateFlow()

    init {
        loadOverview()
    }

    fun loadOverview() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.fetchBusinessOverview()
                .onSuccess { overview ->
                    _uiState.value = _uiState.value.copy(
                        overview = overview,
                        isLoading = false,
                        error = null
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load business overview"
                    )
                }
        }
    }

    fun refresh() {
        loadOverview()
    }
}

data class BusinessOverviewUiState(
    val overview: BusinessOverview? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
