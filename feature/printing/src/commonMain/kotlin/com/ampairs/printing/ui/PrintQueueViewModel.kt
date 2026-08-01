package com.ampairs.printing.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.printing.core.spool.PrintJob
import com.ampairs.printing.data.repository.PrintJobRepository
import com.ampairs.printing.service.PrintCoordinator
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

data class PrintQueueUiState(
    val jobs: List<PrintJob> = emptyList(),
    val message: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PrintQueueViewModel(
    private val jobRepository: PrintJobRepository,
    private val coordinator: PrintCoordinator,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrintQueueUiState())
    val uiState: StateFlow<PrintQueueUiState> = _uiState.asStateFlow()

    init {
        jobRepository.observeJobs()
            .onEach { jobs -> _uiState.update { it.copy(jobs = jobs) } }
            .launchIn(viewModelScope)
    }

    /** Re-send a failed / dead / unconfirmed job on its original printer. */
    fun retry(jobId: String) {
        viewModelScope.launch {
            val result = coordinator.retry(jobId)
            _uiState.update { it.copy(message = if (result.isSuccess) "Retried" else result.exceptionOrNull()?.message) }
        }
    }

    /** Mark a sent-but-unconfirmed job as printed (user confirmed paper came out). */
    fun markPrinted(jobId: String) {
        viewModelScope.launch { coordinator.markPrinted(jobId) }
    }

    /** Remove a job from the queue (device-local spool). */
    fun delete(jobId: String) {
        viewModelScope.launch { jobRepository.delete(jobId) }
    }

    /** Clear all jobs from the queue. */
    fun clearAll() {
        viewModelScope.launch { jobRepository.deleteAll() }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
