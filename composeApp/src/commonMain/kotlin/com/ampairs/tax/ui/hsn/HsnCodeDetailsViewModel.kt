package com.ampairs.tax.ui.hsn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.HsnCodeKey
import com.ampairs.tax.domain.TaxStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.mobilenativefoundation.store.store5.StoreReadRequest

class HsnCodeDetailsViewModel(
    private val hsnCodeId: String,
    private val taxStore: TaxStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HsnCodeDetailsUiState())
    val uiState: StateFlow<HsnCodeDetailsUiState> = _uiState.asStateFlow()

    init {
        loadHsnCodeDetails()
    }

    private fun loadHsnCodeDetails() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                taxStore.hsnCodeStore
                    .stream(StoreReadRequest.cached(
                        key = HsnCodeKey(hsnCodeId),
                        refresh = false
                    ))
                    .collect { response ->
                        when (response) {
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Data -> {
                                val hsnCode = response.value

                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    hsnCode = hsnCode,
                                    error = null
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Exception -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load HSN code: ${response.error.message}"
                                )
                            }
                            is org.mobilenativefoundation.store.store5.StoreReadResponse.Error.Message -> {
                                _uiState.value = _uiState.value.copy(
                                    isLoading = false,
                                    error = "Failed to load HSN code: ${response.message}"
                                )
                            }
                            else -> {
                                // Loading state - continue
                            }
                        }
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load HSN code: ${e.message}"
                )
            }
        }
    }

    fun refreshHsnCode() {
        loadHsnCodeDetails()
    }

    fun deleteHsnCode(onSuccess: () -> Unit) {
        _uiState.value = _uiState.value.copy(isDeleting = true, error = null)

        viewModelScope.launch {
            try {
                val result = taxStore.deleteHsnCode(hsnCodeId)

                if (result.isSuccess) {
                    _uiState.value = _uiState.value.copy(isDeleting = false, error = null)
                    onSuccess()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isDeleting = false,
                        error = "Failed to delete HSN code: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Failed to delete HSN code: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HsnCodeDetailsUiState(
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val error: String? = null,
    val hsnCode: HsnCode? = null
) {
    val hasData: Boolean get() = hsnCode != null
    val showContent: Boolean get() = !isLoading && hasData
    val showEmptyState: Boolean get() = !isLoading && !hasData && error == null
}