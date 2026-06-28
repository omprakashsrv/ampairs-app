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
    /** Committed chips. */
    val pincodes: List<String> = emptyList(),
    val states: List<String> = emptyList(),
    val ranges: List<RangeFormState> = emptyList(),
    /** Draft inputs for the chip "Add" rows. */
    val pincodeDraft: String = "",
    val stateDraft: String = "",
    /** "Test a pincode" box. */
    val testPincode: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    /** Live membership built from the committed chips/ranges. */
    val members: GeoZoneMembers
        get() = GeoZoneMembers(
            pincodes = pincodes,
            states = states,
            pincodeRanges = ranges
                .filter { it.from.isNotBlank() && it.to.isNotBlank() }
                .map { GeoZoneMembers.PincodeRange(from = it.from.trim(), to = it.to.trim()) },
        )

    /** True only when a pincode has been typed and it falls inside the zone. */
    val testInZone: Boolean
        get() = testPincode.isNotBlank() && members.contains(testPincode.trim())

    /**
     * Approximate member count: exact pincodes + states + the sum of each numeric range's size.
     * A `from..to` range of equal endpoints counts as one; malformed ranges count as zero.
     */
    val memberCount: Int
        get() {
            val rangeTotal = members.pincodeRanges.sumOf { r ->
                val from = r.from.toLongOrNull()
                val to = r.to.toLongOrNull()
                if (from != null && to != null && to >= from) (to - from + 1) else 0L
            }
            return pincodes.size + states.size + rangeTotal.toInt()
        }
}

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
                _formState.update { it.copy(isLoading = false, error = NOT_FOUND) }
                return@launch
            }
            _formState.update {
                it.copy(
                    uid = zone.uid,
                    name = zone.name,
                    active = zone.active,
                    pincodes = zone.members.pincodes,
                    states = zone.members.states,
                    ranges = zone.members.pincodeRanges.map { r -> RangeFormState(r.from, r.to) },
                    isLoading = false,
                )
            }
        }
    }

    fun updateName(v: String) = _formState.update { it.copy(name = v, error = null) }
    fun updateActive(v: Boolean) = _formState.update { it.copy(active = v) }

    // --- Pincode chips --------------------------------------------------------------------

    fun updatePincodeDraft(v: String) =
        _formState.update { it.copy(pincodeDraft = v.filter { c -> c.isDigit() }) }

    /** Commit the draft (or a pasted, comma/space-separated batch) into pincode chips. */
    fun addPincode() = _formState.update { st ->
        val tokens = st.pincodeDraft.split(',', ' ', '\n', '\t')
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@update st.copy(pincodeDraft = "")
        val merged = (st.pincodes + tokens).distinct()
        st.copy(pincodes = merged, pincodeDraft = "", error = null)
    }

    fun removePincode(pincode: String) =
        _formState.update { it.copy(pincodes = it.pincodes.filterNot { p -> p == pincode }) }

    // --- State chips ----------------------------------------------------------------------

    fun updateStateDraft(v: String) = _formState.update { it.copy(stateDraft = v) }

    fun addState() = _formState.update { st ->
        val tokens = st.stateDraft.split(',', '\n', '\t')
            .map { it.trim().uppercase() }
            .filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@update st.copy(stateDraft = "")
        val merged = (st.states + tokens).distinct()
        st.copy(states = merged, stateDraft = "", error = null)
    }

    fun removeState(state: String) =
        _formState.update { it.copy(states = it.states.filterNot { s -> s == state }) }

    // --- Ranges ---------------------------------------------------------------------------

    fun addRange() = _formState.update { it.copy(ranges = it.ranges + RangeFormState()) }
    fun removeRange(index: Int) = _formState.update { st -> st.copy(ranges = st.ranges.filterIndexed { i, _ -> i != index }) }
    fun updateRangeFrom(index: Int, v: String) = updateRangeAt(index) { it.copy(from = v.filter { c -> c.isDigit() }) }
    fun updateRangeTo(index: Int, v: String) = updateRangeAt(index) { it.copy(to = v.filter { c -> c.isDigit() }) }

    private fun updateRangeAt(index: Int, transform: (RangeFormState) -> RangeFormState) =
        _formState.update { st -> st.copy(ranges = st.ranges.mapIndexed { i, r -> if (i == index) transform(r) else r }) }

    // --- Test box -------------------------------------------------------------------------

    fun updateTestPincode(v: String) =
        _formState.update { it.copy(testPincode = v.filter { c -> c.isDigit() }) }

    // --- Save -----------------------------------------------------------------------------

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            // Fold any uncommitted drafts into the chips so nothing is lost on save.
            addPincode()
            addState()
            val s = _formState.value
            if (s.name.isBlank()) {
                _formState.update { it.copy(error = NAME_REQUIRED) }
                return@launch
            }
            _formState.update { it.copy(isLoading = true, error = null) }

            val zone = GeoZone(
                uid = if (geoZoneId != null) s.uid else UidGenerator.generateUid("GEZ"),
                name = s.name.trim(),
                members = s.members,
                active = s.active,
            )
            val result = repository.saveGeoZone(zone)
            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _formState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: SAVE_FAILED) }
            }
        }
    }

    private companion object {
        /** Stable error markers; the screen maps these to localized strings. */
        const val NOT_FOUND = "not_found"
        const val NAME_REQUIRED = "name_required"
        const val SAVE_FAILED = "save_failed"
    }
}
