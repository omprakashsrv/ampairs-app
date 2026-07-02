package com.ampairs.pricing.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListItem
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import com.ampairs.common.di.WorkspaceScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One "Who gets this price?" targeting dimension that is actually set on the list. */
data class TargetRow(
    val label: String,
    val value: String,
)

data class PriceListDetailUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val header: PriceList? = null,
    val items: List<PriceListItem> = emptyList(),
    val geoZoneName: String? = null,
)

/**
 * Read model for the Price List Detail screen — loads the aggregate (header + items) and resolves
 * the geo-zone name for the plain-language targeting summary.
 */
@AssistedInject
class PriceListDetailViewModel(
    private val repository: PriceListRepository,
    @Assisted private val priceListId: String,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(priceListId: String): PriceListDetailViewModel
    }

    private val _uiState = MutableStateFlow(PriceListDetailUiState())
    val uiState: StateFlow<PriceListDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val aggregate = repository.getPriceList(priceListId)
            if (aggregate == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            val zoneName = aggregate.header.geoZoneId?.takeIf { it.isNotBlank() }
                ?.let { repository.getGeoZone(it)?.name }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    header = aggregate.header,
                    items = aggregate.items.filter { item -> item.active },
                    geoZoneName = zoneName,
                )
            }
        }
    }
}
