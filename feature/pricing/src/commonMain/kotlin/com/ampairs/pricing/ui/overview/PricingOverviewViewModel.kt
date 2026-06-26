package com.ampairs.pricing.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.PriceListStatus
import com.ampairs.pricing.domain.model.SalesChannel
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * At-a-glance counts for the Pricing & Offers overview dashboard. Reads the local price-list and
 * geo-zone read models reactively; nothing here talks to the network.
 */
data class PricingOverviewUiState(
    val isLoading: Boolean = true,
    val totalListCount: Int = 0,
    val activeListCount: Int = 0,
    val retailActiveCount: Int = 0,
    val wholesaleActiveCount: Int = 0,
    val geoZoneCount: Int = 0,
)

@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PricingOverviewViewModel(
    private val repository: PriceListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PricingOverviewUiState())
    val uiState: StateFlow<PricingOverviewUiState> = _uiState.asStateFlow()

    init {
        combine(repository.observePriceLists(), repository.observeGeoZones()) { lists, zones ->
            val active = lists.filter { it.status == PriceListStatus.ACTIVE }
            PricingOverviewUiState(
                isLoading = false,
                totalListCount = lists.size,
                activeListCount = active.size,
                retailActiveCount = active.count { it.channel == SalesChannel.RETAIL },
                wholesaleActiveCount = active.count { it.channel == SalesChannel.WHOLESALE },
                geoZoneCount = zones.size,
            )
        }
            .onEach { state -> _uiState.update { state } }
            .launchIn(viewModelScope)
    }
}
