package com.ampairs.pricing.ui.geozone

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.GeoZoneMembers
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RangeFormState(val from: String = "", val to: String = "")

data class GeoZoneFormState(
    val uid: String = "",
    val name: String = "",
    val active: Boolean = true,
    /** Comma-separated pincodes / states for simple text entry. */
    val pincodesText: String = "",
    val statesText: String = "",
    val ranges: List<RangeFormState> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class GeoZoneFormViewModel(
    private val repository: PriceListRepository,
    @Assisted private val geoZoneId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(geoZoneId: String?): GeoZoneFormViewModel
    }

    private val _formState = MutableStateFlow(GeoZoneFormState())
    val formState: StateFlow<GeoZoneFormState> = _formState.asStateFlow()

    init {
        if (geoZoneId != null) load(geoZoneId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val zone = repository.getGeoZone(id)
            if (zone == null) {
                _formState.update { it.copy(isLoading = false, error = "Geo zone not found") }
                return@launch
            }
            _formState.update {
                it.copy(
                    uid = zone.uid,
                    name = zone.name,
                    active = zone.active,
                    pincodesText = zone.members.pincodes.joinToString(", "),
                    statesText = zone.members.states.joinToString(", "),
                    ranges = zone.members.pincodeRanges.map { r -> RangeFormState(r.from, r.to) },
                    isLoading = false,
                )
            }
        }
    }

    fun updateName(v: String) = _formState.update { it.copy(name = v, error = null) }
    fun updateActive(v: Boolean) = _formState.update { it.copy(active = v) }
    fun updatePincodes(v: String) = _formState.update { it.copy(pincodesText = v) }
    fun updateStates(v: String) = _formState.update { it.copy(statesText = v) }

    fun addRange() = _formState.update { it.copy(ranges = it.ranges + RangeFormState()) }
    fun removeRange(index: Int) = _formState.update { st -> st.copy(ranges = st.ranges.filterIndexed { i, _ -> i != index }) }
    fun updateRangeFrom(index: Int, v: String) = updateRangeAt(index) { it.copy(from = v.filter { c -> c.isDigit() }) }
    fun updateRangeTo(index: Int, v: String) = updateRangeAt(index) { it.copy(to = v.filter { c -> c.isDigit() }) }

    private fun updateRangeAt(index: Int, transform: (RangeFormState) -> RangeFormState) =
        _formState.update { st -> st.copy(ranges = st.ranges.mapIndexed { i, r -> if (i == index) transform(r) else r }) }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _formState.value
            if (s.name.isBlank()) {
                _formState.update { it.copy(error = "Name is required") }
                return@launch
            }
            _formState.update { it.copy(isLoading = true, error = null) }

            val members = GeoZoneMembers(
                pincodes = s.pincodesText.split(",").map { it.trim() }.filter { it.isNotBlank() },
                states = s.statesText.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() },
                pincodeRanges = s.ranges
                    .filter { it.from.isNotBlank() && it.to.isNotBlank() }
                    .map { GeoZoneMembers.PincodeRange(from = it.from.trim(), to = it.to.trim()) },
            )
            val zone = GeoZone(
                uid = if (geoZoneId != null) s.uid else UidGenerator.generateUid("GEZ"),
                name = s.name.trim(),
                members = members,
                active = s.active,
            )
            val result = repository.saveGeoZone(zone)
            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _formState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to save") }
            }
        }
    }
}
