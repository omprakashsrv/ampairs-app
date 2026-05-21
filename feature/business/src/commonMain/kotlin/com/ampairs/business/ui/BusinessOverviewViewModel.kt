package com.ampairs.business.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.business.data.api.BusinessApi
import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.business.domain.BusinessOverview
import com.ampairs.common.ApiUrlBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * ViewModel for Business Overview Screen.
 * Manages loading and displaying business overview dashboard.
 */
@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class BusinessOverviewViewModel(
    private val repository: BusinessRepository,
    private val businessApi: BusinessApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(BusinessOverviewUiState())
    val uiState: StateFlow<BusinessOverviewUiState> = _uiState.asStateFlow()

    init {
        loadOverview()
    }

    @OptIn(ExperimentalTime::class)
    fun loadOverview() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            repository.fetchBusinessOverview()
                .onSuccess { overview ->
                    _uiState.value = _uiState.value.copy(
                        overview = overview,
                        isLoading = false,
                        error = null
                    )

                    // Fetch business details to get logo URL
                    businessApi.getBusiness()
                        .onSuccess { business ->
                            val hasLogo = !business.logoUrl.isNullOrBlank()
                            val cacheBuster = Clock.System.now().toEpochMilliseconds()
                            _uiState.value = _uiState.value.copy(
                                logoThumbnailUrl = if (hasLogo) ApiUrlBuilder.businessLogoThumbnailUrl() else null,
                                logoCacheBuster = cacheBuster
                            )
                        }
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load business overview"
                    )
                }
        }
    }

    fun refresh() {
        loadOverview()
    }
}

data class BusinessOverviewUiState(
    val overview: BusinessOverview? = null,
    val logoThumbnailUrl: String? = null,
    val logoCacheBuster: Long = 0L,
    val isLoading: Boolean = false,
    val error: String? = null
)
