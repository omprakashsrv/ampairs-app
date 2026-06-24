package com.ampairs.agent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.agent.llm.ModelCatalog
import com.ampairs.agent.llm.ModelDescriptor
import com.ampairs.agent.llm.ModelInstallStatus
import com.ampairs.agent.llm.ModelManager
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A catalog model paired with its current install state, for the model-management list. */
data class ModelRow(
    val model: ModelDescriptor,
    val status: ModelInstallStatus,
)

data class ModelManagerUiState(
    val rows: List<ModelRow> = emptyList(),
)

/**
 * Drives the on-device model-management screen (T032): lists [ModelCatalog] entries with live
 * download/install state from [ModelManager] and exposes download / cancel / delete intents.
 */
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class ModelManagerViewModel(
    private val modelManager: ModelManager,
) : ViewModel() {

    val uiState: StateFlow<ModelManagerUiState> =
        modelManager.statuses
            .map { statuses -> ModelManagerUiState(rowsFrom(statuses)) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ModelManagerUiState(rowsFrom(modelManager.statuses.value)),
            )

    init {
        viewModelScope.launch { modelManager.refresh() }
    }

    fun onDownload(model: ModelDescriptor) = modelManager.download(model)

    fun onCancel(modelId: String) = modelManager.cancel(modelId)

    fun onDelete(model: ModelDescriptor) {
        viewModelScope.launch { modelManager.delete(model) }
    }

    private fun rowsFrom(statuses: Map<String, ModelInstallStatus>): List<ModelRow> =
        ModelCatalog.all.map { ModelRow(it, statuses[it.id] ?: ModelInstallStatus.NotInstalled) }
}
