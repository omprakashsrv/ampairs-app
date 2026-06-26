package com.ampairs.pricing.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.OfferRepository
import com.ampairs.pricing.domain.model.Offer
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OffersListUiState(
    val offers: List<Offer> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class OffersListViewModel(
    private val repository: OfferRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OffersListUiState())
    val uiState: StateFlow<OffersListUiState> = _uiState.asStateFlow()

    init {
        observeOffers()
    }

    fun deleteOffer(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            val result = repository.deleteOffer(id)
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message ?: "Failed to delete offer") }
            }
        }
    }

    private fun observeOffers() {
        repository.observeOffers()
            .onEach { offers -> _uiState.update { it.copy(offers = offers, isLoading = false, error = null) } }
            .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
            .launchIn(viewModelScope)
    }
}
