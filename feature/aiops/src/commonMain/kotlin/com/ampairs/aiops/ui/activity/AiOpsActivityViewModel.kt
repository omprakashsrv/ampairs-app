package com.ampairs.aiops.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.aiops.db.dao.AiOpsDao
import com.ampairs.common.aiops.AiOpsReview
import com.ampairs.common.aiops.AiOpsRunner
import com.ampairs.common.aiops.AiOpsUndo
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The status the runner writes for a suggestion the user should review. */
private const val PENDING_REVIEW = "PENDING_REVIEW"
private const val RECENT_LIMIT = 100

data class AiOpsActivityUiState(
    val items: List<AiOpsActivityItem> = emptyList(),
    val suggestions: List<AiOpsSuggestionItem> = emptyList(),
    val isScanning: Boolean = false,
)

/**
 * Read/act feed of the AI Ops engine, backed by the workspace audit DB (`AiOpsDao`). WorkspaceScope
 * because the audit tables live in the per-workspace consolidated DB. Reactive: an undo/accept/dismiss
 * re-emits the affected rows, so the lists update themselves with no manual refresh.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class AiOpsActivityViewModel(
    private val dao: AiOpsDao,
    private val undo: AiOpsUndo,
    private val review: AiOpsReview,
    private val runner: AiOpsRunner,
) : ViewModel() {

    private val _isScanning = MutableStateFlow(false)

    val uiState: StateFlow<AiOpsActivityUiState> =
        combine(
            dao.observeRecentDecisions(RECENT_LIMIT).map { rows -> rows.map { it.toActivityItem() } },
            dao.observeFindingsByStatus(PENDING_REVIEW).map { rows -> rows.map { it.toSuggestionItem() } },
            _isScanning,
        ) { items, suggestions, scanning ->
            AiOpsActivityUiState(items = items, suggestions = suggestions, isScanning = scanning)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiOpsActivityUiState())

    /** Run every capability across the workspace to surface pre-existing issues. */
    fun scan() {
        if (_isScanning.value) return
        viewModelScope.launch {
            _isScanning.value = true
            try {
                runCatching { runner.scanWorkspace() }
            } finally {
                _isScanning.value = false
            }
        }
    }

    fun undo(decisionId: String) {
        viewModelScope.launch { runCatching { undo.undo(decisionId) } }
    }

    fun acceptSuggestion(findingId: String) {
        viewModelScope.launch { runCatching { review.accept(findingId) } }
    }

    fun dismissSuggestion(findingId: String) {
        viewModelScope.launch { runCatching { review.dismiss(findingId) } }
    }
}
