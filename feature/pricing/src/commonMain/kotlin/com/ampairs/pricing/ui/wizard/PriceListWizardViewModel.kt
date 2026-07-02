package com.ampairs.pricing.ui.wizard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListAggregate
import com.ampairs.pricing.domain.model.PriceListStatus
import com.ampairs.pricing.domain.model.SalesChannel
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

/** Which single targeting dimension the user chose on the "Who gets this price?" step. */
enum class WizardScopeMode { EVERYONE, CUSTOMER_GROUP, CUSTOMER_TYPE, CUSTOMER }

/** Total number of wizard steps (0..5 inclusive). */
const val WIZARD_STEP_COUNT = 6

data class WizardUiState(
    val step: Int = 0,
    val name: String = "",
    val channel: SalesChannel = SalesChannel.WHOLESALE,
    val scopeMode: WizardScopeMode = WizardScopeMode.EVERYONE,
    val scopeValue: String = "",
    val brandId: String = "",
    val categoryId: String = "",
    val productGroupId: String = "",
    val geoZoneId: String = "",
    val geoZones: List<GeoZone> = emptyList(),
    val priority: String = "0",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the "Create Price List" wizard: a 6-step (0..5) header-only flow that writes a single
 * [PriceList] (no items) via [PriceListRepository.savePriceList]. The repository is local-only —
 * the row is persisted unsynced and flagged for the automatic background push.
 */
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
@Inject
class PriceListWizardViewModel(
    private val repository: PriceListRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WizardUiState())
    val uiState: StateFlow<WizardUiState> = _uiState.asStateFlow()

    init {
        repository.observeGeoZones()
            .onEach { zones -> _uiState.update { it.copy(geoZones = zones) } }
            .launchIn(viewModelScope)
    }

    // ── navigation ──────────────────────────────────────────────────────────────
    fun next() = _uiState.update { it.copy(step = (it.step + 1).coerceIn(0, WIZARD_STEP_COUNT - 1)) }
    fun back() = _uiState.update { it.copy(step = (it.step - 1).coerceIn(0, WIZARD_STEP_COUNT - 1)) }

    // ── field setters ───────────────────────────────────────────────────────────
    fun setName(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun setChannel(v: SalesChannel) = _uiState.update { it.copy(channel = v) }

    fun setScopeMode(v: WizardScopeMode) =
        _uiState.update { it.copy(scopeMode = v, scopeValue = if (v == WizardScopeMode.EVERYONE) "" else it.scopeValue) }

    fun setScopeValue(v: String) = _uiState.update { it.copy(scopeValue = v) }
    fun setBrandId(v: String) = _uiState.update { it.copy(brandId = v) }
    fun setCategoryId(v: String) = _uiState.update { it.copy(categoryId = v) }
    fun setProductGroupId(v: String) = _uiState.update { it.copy(productGroupId = v) }
    fun setGeoZoneId(v: String) = _uiState.update { it.copy(geoZoneId = v) }
    fun setPriority(v: String) = _uiState.update { it.copy(priority = v.filter { c -> c.isDigit() }) }

    /** Build the header-only price list and persist it; calls [onSaved] on success. */
    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.name.isBlank()) {
                _uiState.update { it.copy(error = "name_required") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }

            val scopeValue = s.scopeValue.trim().ifBlank { null }
            val header = PriceList(
                uid = UidGenerator.generateUid("PRC"),
                name = s.name.trim(),
                channel = s.channel,
                customerGroupId = if (s.scopeMode == WizardScopeMode.CUSTOMER_GROUP) scopeValue else null,
                customerType = if (s.scopeMode == WizardScopeMode.CUSTOMER_TYPE) scopeValue else null,
                customerId = if (s.scopeMode == WizardScopeMode.CUSTOMER) scopeValue else null,
                brandId = s.brandId.trim().ifBlank { null },
                categoryId = s.categoryId.trim().ifBlank { null },
                productGroupId = s.productGroupId.trim().ifBlank { null },
                geoZoneId = s.geoZoneId.trim().ifBlank { null },
                currency = "INR",
                priority = s.priority.toIntOrNull() ?: 0,
                status = PriceListStatus.ACTIVE,
                active = true,
            )

            val result = repository.savePriceList(PriceListAggregate(header, emptyList()))
            if (result.isSuccess) {
                _uiState.update { it.copy(isSaving = false) }
                onSaved()
            } else {
                _uiState.update {
                    it.copy(isSaving = false, error = result.exceptionOrNull()?.message ?: "save_failed")
                }
            }
        }
    }
}
