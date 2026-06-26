package com.ampairs.pricing.ui.offers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.repository.OfferRepository
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferRewardType
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

/** One hard-coded demo cart line, with money already projected into major units for display. */
data class PreviewCartLine(
    val name: String,
    val qty: Double,
    val unitPrice: Double,
    val lineTotal: Double,
)

/**
 * Read model for the Offer Preview Simulator — a what-if surface that applies the offer's reward to a
 * fixed, hard-coded sample cart and shows the discount math (subtotal − discount = final), plus a
 * "cap reached" note when a percentage discount is clamped by the reward cap.
 *
 * The sample cart is intentionally fake (a couple of fixed demo lines): this previews the offer's
 * effect, it is not connected to any real cart. Offer money is stored in minor units (paise/cents);
 * the UI state exposes major-unit Doubles so the screen can format them with formatMoney(...).
 */
data class OfferPreviewUiState(
    val isLoading: Boolean = true,
    val notFound: Boolean = false,
    val offerName: String = "",
    val rewardSummary: String = "",
    val cartLines: List<PreviewCartLine> = emptyList(),
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val finalTotal: Double = 0.0,
    val capHit: Boolean = false,
)

@AssistedInject
class OfferPreviewViewModel(
    private val offerRepository: OfferRepository,
    @Assisted private val offerId: String,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(offerId: String): OfferPreviewViewModel
    }

    private val _uiState = MutableStateFlow(OfferPreviewUiState())
    val uiState: StateFlow<OfferPreviewUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val offer = offerRepository.getOffer(offerId)
            if (offer == null) {
                _uiState.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            _uiState.update { buildState(offer) }
        }
    }

    private fun buildState(offer: Offer): OfferPreviewUiState {
        // Hard-coded demo cart (minor units). This is a what-if simulator, not a real cart.
        val demoLines = listOf(
            DemoLine(name = SAMPLE_LINE_1_NAME, qty = 12.0, unitPriceMinor = 45_000L),
            DemoLine(name = SAMPLE_LINE_2_NAME, qty = 6.0, unitPriceMinor = 38_000L),
        )

        val subtotalMinor = demoLines.sumOf { (it.unitPriceMinor.toDouble() * it.qty).toLong() }

        // Discount from the offer's reward.
        val (discountMinor, capHit) = when (offer.rewardType) {
            OfferRewardType.PERCENT -> {
                val pct = offer.rewardPercent ?: 0.0
                val raw = (subtotalMinor.toDouble() * pct / 100.0).toLong()
                val cap = offer.rewardCapMinor
                if (cap != null && raw > cap) cap to true else raw to false
            }
            OfferRewardType.FLAT -> (offer.rewardFlatMinor ?: 0L) to false
            else -> 0L to false
        }
        val clampedDiscountMinor = discountMinor.coerceIn(0L, subtotalMinor)
        val finalMinor = subtotalMinor - clampedDiscountMinor

        return OfferPreviewUiState(
            isLoading = false,
            notFound = false,
            offerName = offer.name,
            rewardSummary = rewardSummary(offer),
            cartLines = demoLines.map { line ->
                PreviewCartLine(
                    name = line.name,
                    qty = line.qty,
                    unitPrice = line.unitPriceMinor.toMajor(),
                    lineTotal = (line.unitPriceMinor.toDouble() * line.qty).toLong().toMajor(),
                )
            },
            subtotal = subtotalMinor.toMajor(),
            discount = clampedDiscountMinor.toMajor(),
            finalTotal = finalMinor.toMajor(),
            capHit = capHit,
        )
    }

    /** A short, non-localized reward descriptor. The screen renders it inside a localized label. */
    private fun rewardSummary(offer: Offer): String = when (offer.rewardType) {
        OfferRewardType.PERCENT -> "${trimNum(offer.rewardPercent ?: 0.0)}%"
        OfferRewardType.FLAT -> trimNum((offer.rewardFlatMinor ?: 0L).toMajor())
        OfferRewardType.BOGO -> "${offer.bogoBuyQty ?: 0} + ${offer.bogoGetQty ?: 0}"
        OfferRewardType.FREE_SHIPPING -> "Free shipping"
        OfferRewardType.FREE_GIFT -> "Free gift"
        OfferRewardType.TIERED -> "Tiered"
    }

    private data class DemoLine(val name: String, val qty: Double, val unitPriceMinor: Long)

    private companion object {
        const val SAMPLE_LINE_1_NAME = "Basmati Rice 5kg"
        const val SAMPLE_LINE_2_NAME = "Sunflower Oil 1L"

        fun Long.toMajor(): Double = this / 100.0

        fun trimNum(value: Double): String =
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }
}
