package com.ampairs.tax.ui.hsn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.tax.domain.HsnCategory
import com.ampairs.tax.domain.HsnCode
import com.ampairs.tax.domain.TaxStore
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class HsnCodeFormViewModel(
    private val hsnCodeId: String?,
    private val taxStore: TaxStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(HsnCodeFormUiState())
    val uiState: StateFlow<HsnCodeFormUiState> = _uiState.asStateFlow()

    init {
        if (hsnCodeId != null) {
            loadHsnCode(hsnCodeId)
        }
    }

    private fun loadHsnCode(id: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                val hsnCode = taxStore.hsnCodeStore
                    .stream(org.mobilenativefoundation.store.store5.StoreReadRequest.cached(
                        key = com.ampairs.tax.domain.HsnCodeKey(id),
                        refresh = false
                    ))
                    .first { it is org.mobilenativefoundation.store.store5.StoreReadResponse.Data }
                    .let { (it as org.mobilenativefoundation.store.store5.StoreReadResponse.Data).value }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    hsnCode = hsnCode.hsnCode,
                    description = hsnCode.description,
                    chapter = hsnCode.chapter,
                    heading = hsnCode.heading,
                    category = hsnCode.category,
                    isActive = hsnCode.isActive,
                    originalHsnCode = hsnCode
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load HSN code: ${e.message}"
                )
            }
        }
    }

    fun updateHsnCode(hsnCode: String) {
        if (hsnCode.all { it.isDigit() } && hsnCode.length <= 8) {
            _uiState.value = _uiState.value.copy(
                hsnCode = hsnCode,
                error = null
            )
        }
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            error = null
        )
    }

    fun updateChapter(chapter: String) {
        _uiState.value = _uiState.value.copy(
            chapter = chapter,
            error = null
        )
    }

    fun updateHeading(heading: String) {
        _uiState.value = _uiState.value.copy(
            heading = heading,
            error = null
        )
    }

    fun updateCategory(category: HsnCategory) {
        _uiState.value = _uiState.value.copy(
            category = category,
            error = null
        )
    }

    fun updateIsActive(isActive: Boolean) {
        _uiState.value = _uiState.value.copy(
            isActive = isActive,
            error = null
        )
    }

    fun saveHsnCode(onSuccess: () -> Unit) {
        val currentState = _uiState.value

        if (!currentState.canSave) {
            _uiState.value = currentState.copy(
                error = "Please fill in all required fields with valid data"
            )
            return
        }

        _uiState.value = currentState.copy(isSaving = true, error = null)

        viewModelScope.launch {
            try {
                val hsnCodeToSave = if (hsnCodeId != null && currentState.originalHsnCode != null) {
                    // Update existing
                    currentState.originalHsnCode.copy(
                        hsnCode = currentState.hsnCode,
                        description = currentState.description,
                        chapter = currentState.chapter,
                        heading = currentState.heading,
                        category = currentState.category,
                        isActive = currentState.isActive
                    )
                } else {
                    // Create new
                    HsnCode(
                        id = Uuid.random().toString(),
                        hsnCode = currentState.hsnCode,
                        description = currentState.description,
                        chapter = currentState.chapter,
                        heading = currentState.heading,
                        category = currentState.category,
                        isActive = currentState.isActive
                    )
                }

                val result = if (hsnCodeId != null) {
                    taxStore.updateHsnCode(hsnCodeToSave)
                } else {
                    taxStore.createHsnCode(hsnCodeToSave)
                }

                if (result.isSuccess) {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        error = null
                    )
                    onSuccess()
                } else {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        error = "Failed to save HSN code: ${result.exceptionOrNull()?.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    isSaving = false,
                    error = "Failed to save HSN code: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class HsnCodeFormUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val hsnCode: String = "",
    val description: String = "",
    val chapter: String = "",
    val heading: String = "",
    val category: HsnCategory = HsnCategory.GENERAL,
    val isActive: Boolean = true,
    val originalHsnCode: HsnCode? = null
) {
    val canSave: Boolean
        get() = hsnCode.isNotBlank() &&
                description.isNotBlank() &&
                chapter.isNotBlank() &&
                heading.isNotBlank() &&
                hsnCode.matches(Regex("^\\d{4,8}$")) &&
                !isSaving

    val isValidHsnCode: Boolean
        get() = hsnCode.matches(Regex("^\\d{4,8}$"))

    val formattedHsnCode: String
        get() = when (hsnCode.length) {
            4 -> hsnCode
            6 -> "${hsnCode.take(4)} ${hsnCode.drop(4)}"
            8 -> "${hsnCode.take(4)} ${hsnCode.drop(4).take(2)} ${hsnCode.drop(6)}"
            else -> hsnCode
        }
}