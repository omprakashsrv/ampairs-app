package com.ampairs.printing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.model.Template
import com.ampairs.printing.data.repository.TemplateRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TemplateListUiState(
    val templates: List<Template> = emptyList(),
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class TemplateListViewModel(
    private val repository: TemplateRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateListUiState())
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { repository.seedDefaultsIfEmpty() }
        repository.observeAll()
            .onEach { templates -> _uiState.update { it.copy(templates = templates) } }
            .launchIn(viewModelScope)
    }

    /** Re-seed the built-in templates (overwrites by id) and flag them for backend push. */
    fun restoreDefaults() {
        viewModelScope.launch { repository.restoreDefaults() }
    }
}
