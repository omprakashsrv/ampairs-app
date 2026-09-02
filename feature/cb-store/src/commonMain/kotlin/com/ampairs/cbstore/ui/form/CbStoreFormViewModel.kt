package com.ampairs.cbstore.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.cbstore.data.repository.StoreRepository
import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.common.id_generator.UidGenerator
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STORE_UID_PREFIX = "STR"

data class CbStoreFormUiState(
    val code: String = "",
    val name: String = "",
    val city: String = "",
    val zonalOfficeId: String = "",
    val zoneOptions: List<ZonalOffice> = emptyList(),
    val isEdit: Boolean = false,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
) {
    val isValid: Boolean get() = code.isNotBlank() && name.isNotBlank() && city.isNotBlank() && zonalOfficeId.isNotBlank()
}

@AssistedInject
class CbStoreFormViewModel(
    @Assisted private val storeId: String?,
    private val repository: StoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CbStoreFormUiState(isEdit = storeId != null))
    val uiState: StateFlow<CbStoreFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val zones = repository.observeZonalOffices().first()
            _uiState.update { it.copy(zoneOptions = zones) }
            if (storeId != null) {
                repository.getStore(storeId)?.let { s ->
                    _uiState.update {
                        it.copy(code = s.code, name = s.name, city = s.city, zonalOfficeId = s.zonalOfficeId)
                    }
                }
            }
        }
    }

    fun onCode(v: String) = _uiState.update { it.copy(code = v) }
    fun onName(v: String) = _uiState.update { it.copy(name = v) }
    fun onCity(v: String) = _uiState.update { it.copy(city = v) }
    fun onZone(v: String) = _uiState.update { it.copy(zonalOfficeId = v) }

    fun save() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Code, name, city and zone are required") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            val uid = storeId ?: UidGenerator.generateUid(STORE_UID_PREFIX)
            val result = repository.saveStore(
                Store(
                    uid = uid,
                    code = state.code.trim(),
                    name = state.name.trim(),
                    city = state.city.trim(),
                    zonalOfficeId = state.zonalOfficeId,
                    active = true,
                ),
            )
            _uiState.update {
                if (result.isSuccess) it.copy(isSaving = false, saved = true)
                else it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "Failed to save outlet")
            }
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(storeId: String?): CbStoreFormViewModel
    }
}
