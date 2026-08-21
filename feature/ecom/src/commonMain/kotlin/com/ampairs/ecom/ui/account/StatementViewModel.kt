package com.ampairs.ecom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.BuyerOutstanding
import com.ampairs.ecom.api.model.BuyerStatement
import com.ampairs.ecom.data.repository.StatementRepository
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatementUiState(
    val outstanding: BuyerOutstanding? = null,
    val statement: BuyerStatement? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class StatementViewModel(
    private val repository: StatementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(StatementUiState())
    val state: StateFlow<StatementUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            val outstanding = repository.getOutstanding()
            val statement = repository.getStatement()
            val error = outstanding.exceptionOrNull() ?: statement.exceptionOrNull()
            _state.update {
                it.copy(
                    outstanding = outstanding.getOrNull(),
                    statement = statement.getOrNull(),
                    isLoading = false,
                    error = error?.message,
                )
            }
        }
    }
}
