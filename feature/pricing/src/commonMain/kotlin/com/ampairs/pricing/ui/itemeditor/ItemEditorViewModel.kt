package com.ampairs.pricing.ui.itemeditor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceTier
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
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
import kotlin.math.round

/** One editable quantity slab; money is entered in major units (e.g. rupees) as text. */
data class TierRow(
    val minQty: String = "",
    val unitPrice: String = "",
)

/**
 * Editor state for a single price-list item plus its quantity-tier ladder. Money fields hold major
 * units as text (minor units are derived at save). The visual ladder, "valid ladder" indicator and
 * the inline tester all read from this state.
 */
data class ItemEditorUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val searchResults: List<ProductSummary> = emptyList(),
    val selectedProduct: ProductSummary? = null,
    val variantSku: String = "",
    val unitPrice: String = "",
    val moq: String = "",
    val tiers: List<TierRow> = emptyList(),
    val testerQuantity: Double = 10.0,
) {
    /** Tiers as parsed (minQty, unitPriceMajor) pairs, keeping only rows with a positive minQty. */
    val parsedTiers: List<Pair<Double, Double>>
        get() = tiers.mapNotNull { row ->
            val q = row.minQty.toDoubleOrNull() ?: return@mapNotNull null
            val p = row.unitPrice.toDoubleOrNull() ?: 0.0
            if (q > 0) q to p else null
        }

    /** A valid ladder: every minQty > 0 and strictly ascending across the entered rows. */
    val isLadderValid: Boolean
        get() {
            val rows = tiers.filter { it.minQty.isNotBlank() }
            if (rows.isEmpty()) return false
            val qtys = rows.map { it.minQty.toDoubleOrNull() }
            if (qtys.any { it == null || it <= 0.0 }) return false
            val nonNull = qtys.filterNotNull()
            return nonNull.zipWithNext().all { (a, b) -> a < b }
        }

    /** Effective unit price (major) at [testerQuantity]: highest tier whose minQty <= qty, else base. */
    val testerUnitPrice: Double
        get() {
            val base = unitPrice.toDoubleOrNull() ?: 0.0
            val applicable = parsedTiers.filter { it.first <= testerQuantity }.maxByOrNull { it.first }
            return applicable?.second ?: base
        }

    /** The minQty of the applied tier at [testerQuantity], or null when the base price applies. */
    val testerAppliedTierMinQty: Double?
        get() = parsedTiers.filter { it.first <= testerQuantity }.maxByOrNull { it.first }?.first
}

@AssistedInject
class ItemEditorViewModel(
    private val repository: PriceListRepository,
    private val productDataService: ProductDataService,
    @Assisted private val priceListId: String,
    @Assisted private val itemId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(priceListId: String, itemId: String?): ItemEditorViewModel
    }

    private val _uiState = MutableStateFlow(ItemEditorUiState())
    val uiState: StateFlow<ItemEditorUiState> = _uiState.asStateFlow()

    init {
        search("")
        if (itemId != null) loadExisting(itemId)
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val aggregate = repository.getPriceList(priceListId)
            val item = aggregate?.items?.firstOrNull { it.uid == id }
            if (item == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }
            val product = productDataService.getById(item.productId)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    selectedProduct = product,
                    variantSku = item.variantSku ?: "",
                    unitPrice = minorToMajor(item.unitPriceMinor),
                    moq = item.moq?.let(::formatDouble) ?: "",
                    tiers = item.tiers
                        .sortedBy { t -> t.minQty }
                        .map { t -> TierRow(minQty = formatDouble(t.minQty), unitPrice = minorToMajor(t.unitPriceMinor)) },
                )
            }
        }
    }

    // ── product picker ──────────────────────────────────────────────────────────
    fun onQueryChange(query: String) {
        _uiState.update { it.copy(query = query) }
        search(query)
    }

    fun selectProduct(product: ProductSummary) {
        _uiState.update {
            it.copy(
                selectedProduct = product,
                searchResults = emptyList(),
                query = "",
                // Seed the base price from the catalog selling price when empty.
                unitPrice = if (it.unitPrice.isBlank()) formatDouble(product.sellingPrice) else it.unitPrice,
            )
        }
    }

    fun clearProduct() {
        _uiState.update { it.copy(selectedProduct = null, query = "") }
        search("")
    }

    private fun search(term: String) {
        viewModelScope.launch {
            val results = productDataService.searchSummaries(term, limit = 25)
            _uiState.update { it.copy(searchResults = results) }
        }
    }

    // ── field updates ───────────────────────────────────────────────────────────
    fun updateVariantSku(v: String) = _uiState.update { it.copy(variantSku = v) }
    fun updateUnitPrice(v: String) = _uiState.update { it.copy(unitPrice = sanitizeDecimal(v)) }
    fun updateMoq(v: String) = _uiState.update { it.copy(moq = sanitizeDecimal(v)) }

    // ── tier updates ────────────────────────────────────────────────────────────
    fun addTier() = _uiState.update { it.copy(tiers = it.tiers + TierRow()) }

    fun removeTier(index: Int) =
        _uiState.update { st -> st.copy(tiers = st.tiers.filterIndexed { i, _ -> i != index }) }

    fun updateTierMinQty(index: Int, v: String) = updateTierAt(index) { it.copy(minQty = sanitizeDecimal(v)) }
    fun updateTierPrice(index: Int, v: String) = updateTierAt(index) { it.copy(unitPrice = sanitizeDecimal(v)) }

    private fun updateTierAt(index: Int, transform: (TierRow) -> TierRow) =
        _uiState.update { st -> st.copy(tiers = st.tiers.mapIndexed { i, t -> if (i == index) transform(t) else t }) }

    fun setTesterQuantity(quantity: Double) {
        val clamped = if (quantity < 1.0) 1.0 else quantity
        _uiState.update { it.copy(testerQuantity = clamped) }
    }

    fun incrementTesterQuantity() = setTesterQuantity(_uiState.value.testerQuantity + 1)
    fun decrementTesterQuantity() = setTesterQuantity(_uiState.value.testerQuantity - 1)

    // ── save ────────────────────────────────────────────────────────────────────
    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val s = _uiState.value
            val product = s.selectedProduct ?: return@launch
            val aggregate = repository.getPriceList(priceListId) ?: return@launch

            val uid = itemId ?: UidGenerator.generateUid("PLI")
            val item = PriceListItem(
                uid = uid,
                priceListId = priceListId,
                productId = product.id,
                variantSku = s.variantSku.trim().ifBlank { null },
                unitPriceMinor = round((s.unitPrice.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                moq = s.moq.toDoubleOrNull(),
                tiers = s.tiers
                    .filter { it.minQty.isNotBlank() }
                    .map { row ->
                        PriceTier(
                            minQty = row.minQty.toDoubleOrNull() ?: 0.0,
                            unitPriceMinor = round((row.unitPrice.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                        )
                    }
                    .sortedBy { it.minQty },
                active = true,
            )

            val others = aggregate.items.filter { it.uid != uid }
            val newItems = others + item
            repository.savePriceList(aggregate.copy(items = newItems))
            onSaved()
        }
    }
}

// ── money/number helpers (major units <-> minor) ────────────────────────────────
private fun minorToMajor(minor: Long): String = formatDouble(minor / 100.0)
private fun formatDouble(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

/** Keep only digits and a single decimal point. */
private fun sanitizeDecimal(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot < 0) return filtered
    return filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
}
