package com.ampairs.aiops.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.aiops.db.dao.AiOpsDao
import com.ampairs.common.aiops.AiOpsUndo
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
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
    val pendingSuggestions: Int = 0,
)

/**
 * Read-only feed of what the AI Ops engine did, backed by the workspace audit DB (`AiOpsDao`).
 * WorkspaceScope because the audit tables live in the per-workspace consolidated DB. Reactive: an undo
 * re-emits the affected row with `revertedAt` set, so the list updates itself with no manual refresh.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class AiOpsActivityViewModel(
    private val dao: AiOpsDao,
    private val undo: AiOpsUndo,
) : ViewModel() {

    val uiState: StateFlow<AiOpsActivityUiState> =
        combine(
            dao.observeRecentDecisions(RECENT_LIMIT).map { rows -> rows.map { it.toActivityItem() } },
            dao.observeFindingsByStatus(PENDING_REVIEW).map { it.size },
        ) { items, pending -> AiOpsActivityUiState(items = items, pendingSuggestions = pending) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AiOpsActivityUiState())

    fun undo(decisionId: String) {
        viewModelScope.launch { runCatching { undo.undo(decisionId) } }
    }
}
