package com.ampairs.pricing.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.pricing.data.repository.OfferRepository
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferConditionType
import com.ampairs.pricing.domain.model.OfferRewardType
import com.ampairs.pricing.domain.model.OfferStatus
import com.ampairs.pricing.domain.model.SalesChannel
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Total number of offer-builder steps (0..5 inclusive). */
const val OFFER_STEP_COUNT = 6

/**
 * Editable form state for the offer builder. Money fields are held as **major-unit text** (what the
 * user types) and converted to minor units (Long) only on save — consistent with the [Offer] model
 * where `cartMinMinor` / `rewardFlatMinor` / `rewardCapMinor` are minor-unit Longs.
 */
data class OfferBuilderUiState(
    val step: Int = 0,
    val name: String = "",
    val channel: SalesChannel = SalesChannel.RETAIL,
    val status: OfferStatus = OfferStatus.SCHEDULED,
    val priority: String = "0",
    val startsAt: String = "",
    val endsAt: String = "",
    // Targets — same dimensions as a price list.
    val customerGroupId: String = "",
    val customerType: String = "",
    val brandId: String = "",
    val categoryId: String = "",
    val geoZoneId: String = "",
    val geoZones: List<GeoZone> = emptyList(),
    // Trigger / condition.
    val conditionType: OfferConditionType = OfferConditionType.NONE,
    val cartMin: String = "",
    val quantityMin: String = "",
    val couponCode: String = "",
    val couponLimit: String = "",
    // Reward.
    val rewardType: OfferRewardType = OfferRewardType.PERCENT,
    val rewardPercent: String = "",
    val rewardFlat: String = "",
    val rewardCap: String = "",
    val bogoBuyQty: String = "",
    val bogoGetQty: String = "",
    // Limits.
    val stackable: Boolean = false,
    val exclusive: Boolean = false,
    val perCustomerLimit: String = "",
    val totalLimit: String = "",
    val isSaving: Boolean = false,
    val error: String? = null,
)

/**
 * Drives the 6-step (0..5) Offer Builder wizard. Loads geo zones for the targeting picker and (when
 * editing) prefills from an existing [Offer]. On save it builds a fully-formed [Offer] (UID set here)
 * and persists it via the local-only [OfferRepository] — there is no backend offer resource yet.
 */
@AssistedInject
class OfferBuilderViewModel(
    private val offerRepository: OfferRepository,
    private val priceListRepository: PriceListRepository,
    @Assisted private val offerId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(offerId: String?): OfferBuilderViewModel
    }

    private val _uiState = MutableStateFlow(OfferBuilderUiState())
    val uiState: StateFlow<OfferBuilderUiState> = _uiState.asStateFlow()

    init {
        priceListRepository.observeGeoZones()
            .onEach { zones -> _uiState.update { it.copy(geoZones = zones) } }
            .launchIn(viewModelScope)

        if (offerId != null) loadExisting(offerId)
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            val o = offerRepository.getOffer(id) ?: return@launch
            _uiState.update {
                it.copy(
                    name = o.name,
                    channel = o.channel,
                    status = o.status,
                    priority = o.priority.toString(),
                    startsAt = o.startsAt.orEmpty(),
                    endsAt = o.endsAt.orEmpty(),
                    customerGroupId = o.customerGroupId.orEmpty(),
                    customerType = o.customerType.orEmpty(),
                    brandId = o.brandId.orEmpty(),
                    categoryId = o.categoryId.orEmpty(),
                    geoZoneId = o.geoZoneId.orEmpty(),
                    conditionType = o.conditionType,
                    cartMin = o.cartMinMinor.toMajorText(),
                    quantityMin = o.quantityMin?.let { q -> formatQty(q) } ?: "",
                    couponCode = o.couponCode.orEmpty(),
                    couponLimit = o.couponLimit?.toString() ?: "",
                    rewardType = o.rewardType,
                    rewardPercent = o.rewardPercent?.let { p -> formatQty(p) } ?: "",
                    rewardFlat = o.rewardFlatMinor.toMajorText(),
                    rewardCap = o.rewardCapMinor.toMajorText(),
                    bogoBuyQty = o.bogoBuyQty?.toString() ?: "",
                    bogoGetQty = o.bogoGetQty?.toString() ?: "",
                    stackable = o.stackable,
                    exclusive = o.exclusive,
                    perCustomerLimit = o.perCustomerLimit?.toString() ?: "",
                    totalLimit = o.totalLimit?.toString() ?: "",
                )
            }
        }
    }

    // ── navigation ──────────────────────────────────────────────────────────────
    fun next() = _uiState.update { it.copy(step = (it.step + 1).coerceIn(0, OFFER_STEP_COUNT - 1)) }
    fun back() = _uiState.update { it.copy(step = (it.step - 1).coerceIn(0, OFFER_STEP_COUNT - 1)) }

    // ── step 0 basics ─────────────────────────────────────────────────────────────
    fun setName(v: String) = _uiState.update { it.copy(name = v, error = null) }
    fun setChannel(v: SalesChannel) = _uiState.update { it.copy(channel = v) }
    fun setStatus(v: OfferStatus) = _uiState.update { it.copy(status = v) }
    fun setPriority(v: String) = _uiState.update { it.copy(priority = v.filter { c -> c.isDigit() }) }

    // ── step 1 targets ────────────────────────────────────────────────────────────
    fun setCustomerGroupId(v: String) = _uiState.update { it.copy(customerGroupId = v) }
    fun setCustomerType(v: String) = _uiState.update { it.copy(customerType = v) }
    fun setBrandId(v: String) = _uiState.update { it.copy(brandId = v) }
    fun setCategoryId(v: String) = _uiState.update { it.copy(categoryId = v) }
    fun setGeoZoneId(v: String) = _uiState.update { it.copy(geoZoneId = v) }

    // ── step 2 trigger ────────────────────────────────────────────────────────────
    fun setConditionType(v: OfferConditionType) = _uiState.update { it.copy(conditionType = v) }
    fun setCartMin(v: String) = _uiState.update { it.copy(cartMin = v.moneyFilter()) }
    fun setQuantityMin(v: String) = _uiState.update { it.copy(quantityMin = v.decimalFilter()) }
    fun setCouponCode(v: String) = _uiState.update { it.copy(couponCode = v) }
    fun setCouponLimit(v: String) = _uiState.update { it.copy(couponLimit = v.filter { c -> c.isDigit() }) }

    // ── step 3 reward ─────────────────────────────────────────────────────────────
    fun setRewardType(v: OfferRewardType) = _uiState.update { it.copy(rewardType = v) }
    fun setRewardPercent(v: String) = _uiState.update { it.copy(rewardPercent = v.decimalFilter()) }
    fun setRewardFlat(v: String) = _uiState.update { it.copy(rewardFlat = v.moneyFilter()) }
    fun setRewardCap(v: String) = _uiState.update { it.copy(rewardCap = v.moneyFilter()) }
    fun setBogoBuyQty(v: String) = _uiState.update { it.copy(bogoBuyQty = v.filter { c -> c.isDigit() }) }
    fun setBogoGetQty(v: String) = _uiState.update { it.copy(bogoGetQty = v.filter { c -> c.isDigit() }) }

    // ── step 4 limits ─────────────────────────────────────────────────────────────
    fun toggleStackable() = _uiState.update { it.copy(stackable = !it.stackable) }
    fun toggleExclusive() = _uiState.update { it.copy(exclusive = !it.exclusive) }
    fun setPerCustomerLimit(v: String) = _uiState.update { it.copy(perCustomerLimit = v.filter { c -> c.isDigit() }) }
    fun setTotalLimit(v: String) = _uiState.update { it.copy(totalLimit = v.filter { c -> c.isDigit() }) }

    /** Build the offer and persist it; calls [onSaved] on success. Validates name non-blank. */
    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            if (s.name.isBlank()) {
                _uiState.update { it.copy(error = "name_required") }
                return@launch
            }
            _uiState.update { it.copy(isSaving = true, error = null) }

            val offer = Offer(
                uid = offerId ?: UidGenerator.generateUid("OFR"),
                name = s.name.trim(),
                channel = s.channel,
                status = s.status,
                priority = s.priority.toIntOrNull() ?: 0,
                startsAt = s.startsAt.trim().ifBlank { null },
                endsAt = s.endsAt.trim().ifBlank { null },
                customerGroupId = s.customerGroupId.trim().ifBlank { null },
                customerType = s.customerType.trim().ifBlank { null },
                brandId = s.brandId.trim().ifBlank { null },
                categoryId = s.categoryId.trim().ifBlank { null },
                geoZoneId = s.geoZoneId.trim().ifBlank { null },
                conditionType = s.conditionType,
                cartMinMinor = s.cartMin.toMinorOrNull(),
                quantityMin = s.quantityMin.toDoubleOrNull(),
                couponCode = s.couponCode.trim().ifBlank { null },
                couponLimit = s.couponLimit.toIntOrNull(),
                rewardType = s.rewardType,
                rewardPercent = s.rewardPercent.toDoubleOrNull(),
                rewardFlatMinor = s.rewardFlat.toMinorOrNull(),
                rewardCapMinor = s.rewardCap.toMinorOrNull(),
                bogoBuyQty = s.bogoBuyQty.toIntOrNull(),
                bogoGetQty = s.bogoGetQty.toIntOrNull(),
                stackable = s.stackable,
                exclusive = s.exclusive,
                perCustomerLimit = s.perCustomerLimit.toIntOrNull(),
                totalLimit = s.totalLimit.toIntOrNull(),
                active = true,
            )

            val result = offerRepository.saveOffer(offer)
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

// ── pure money/number helpers (no IO, testable) ─────────────────────────────────────

/** Keep digits + a single decimal point — for major-unit money text. */
private fun String.moneyFilter(): String = filterDecimal()

/** Keep digits + a single decimal point — for quantity/percent text. */
private fun String.decimalFilter(): String = filterDecimal()

private fun String.filterDecimal(): String {
    val sb = StringBuilder()
    var seenDot = false
    for (c in this) {
        when {
            c.isDigit() -> sb.append(c)
            c == '.' && !seenDot -> {
                seenDot = true
                sb.append(c)
            }
        }
    }
    return sb.toString()
}

/** Major-unit text → minor-unit Long (paise/cents), rounding to 2 dp. Null when blank/invalid. */
private fun String.toMinorOrNull(): Long? {
    val major = trim().toDoubleOrNull() ?: return null
    return (major * 100.0).roundToLong()
}

/** Minor-unit Long → major-unit display text ("" when null). */
private fun Long?.toMajorText(): String {
    val v = this ?: return ""
    val major = v / 100.0
    return formatQty(major)
}

/** Render a Double without a trailing ".0" so whole numbers read cleanly in inputs. */
private fun formatQty(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}

private fun Double.roundToLong(): Long {
    val floor = this.toLong()
    val frac = this - floor
    return if (frac >= 0.5) floor + 1 else if (frac <= -0.5) floor - 1 else floor
}
