package com.ampairs.pricing.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.pricing.data.repository.PriceListRepository
import com.ampairs.pricing.domain.model.PriceList
import com.ampairs.pricing.domain.model.PriceListAggregate
import com.ampairs.pricing.domain.model.PriceListItem
import com.ampairs.pricing.domain.model.PriceListStatus
import com.ampairs.pricing.domain.model.PriceTier
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.round

/** A quantity slab in the editor; money is entered in major units (e.g. rupees) as text. */
data class TierFormState(
    val minQty: String = "",
    val unitPrice: String = "",
)

/** A price-list item in the editor. `uid` blank = newly added (gets a UID at save). */
data class ItemFormState(
    val uid: String = "",
    val productId: String = "",
    val variantSku: String = "",
    val unitPrice: String = "",
    val moq: String = "",
    val tiers: List<TierFormState> = emptyList(),
)

data class PriceListFormState(
    val uid: String = "",
    val name: String = "",
    val channel: SalesChannel = SalesChannel.RETAIL,
    val customerGroupId: String = "",
    val customerType: String = "",
    val brandId: String = "",
    val categoryId: String = "",
    val productGroupId: String = "",
    val geoZoneId: String = "",
    val currency: String = "INR",
    val priority: String = "0",
    val status: PriceListStatus = PriceListStatus.DRAFT,
    val active: Boolean = true,
    val items: List<ItemFormState> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@AssistedInject
class PriceListFormViewModel(
    private val repository: PriceListRepository,
    @Assisted private val priceListId: String?,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(priceListId: String?): PriceListFormViewModel
    }

    private val _formState = MutableStateFlow(PriceListFormState())
    val formState: StateFlow<PriceListFormState> = _formState.asStateFlow()

    init {
        if (priceListId != null) load(priceListId)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            _formState.update { it.copy(isLoading = true) }
            val aggregate = repository.getPriceList(id)
            if (aggregate == null) {
                _formState.update { it.copy(isLoading = false, error = "Price list not found") }
                return@launch
            }
            val h = aggregate.header
            _formState.update {
                it.copy(
                    uid = h.uid,
                    name = h.name,
                    channel = h.channel,
                    customerGroupId = h.customerGroupId ?: "",
                    customerType = h.customerType ?: "",
                    brandId = h.brandId ?: "",
                    categoryId = h.categoryId ?: "",
                    productGroupId = h.productGroupId ?: "",
                    geoZoneId = h.geoZoneId ?: "",
                    currency = h.currency,
                    priority = h.priority.toString(),
                    status = h.status,
                    active = h.active,
                    items = aggregate.items.map { it.toFormState() },
                    isLoading = false,
                )
            }
        }
    }

    // ── header field updates ──────────────────────────────────────────────────
    fun updateName(v: String) = _formState.update { it.copy(name = v, error = null) }
    fun updateChannel(v: SalesChannel) = _formState.update { it.copy(channel = v) }
    fun updateCustomerGroupId(v: String) = _formState.update { it.copy(customerGroupId = v) }
    fun updateCurrency(v: String) = _formState.update { it.copy(currency = v.uppercase(), error = null) }
    fun updatePriority(v: String) = _formState.update { it.copy(priority = v.filter { c -> c.isDigit() }, error = null) }
    fun updateStatus(v: PriceListStatus) = _formState.update { it.copy(status = v) }
    fun updateActive(v: Boolean) = _formState.update { it.copy(active = v) }

    // ── item updates ──────────────────────────────────────────────────────────
    fun addItem() = _formState.update { it.copy(items = it.items + ItemFormState(), error = null) }
    fun removeItem(index: Int) =
        _formState.update { st -> st.copy(items = st.items.filterIndexed { i, _ -> i != index }) }

    fun updateItemProductId(i: Int, v: String) = updateItemAt(i) { it.copy(productId = v) }
    fun updateItemVariant(i: Int, v: String) = updateItemAt(i) { it.copy(variantSku = v) }
    fun updateItemUnitPrice(i: Int, v: String) = updateItemAt(i) { it.copy(unitPrice = sanitizeDecimal(v)) }
    fun updateItemMoq(i: Int, v: String) = updateItemAt(i) { it.copy(moq = sanitizeDecimal(v)) }

    // ── tier updates ──────────────────────────────────────────────────────────
    fun addTier(itemIndex: Int) = updateItemAt(itemIndex) { it.copy(tiers = it.tiers + TierFormState()) }
    fun removeTier(itemIndex: Int, tierIndex: Int) =
        updateItemAt(itemIndex) { item -> item.copy(tiers = item.tiers.filterIndexed { i, _ -> i != tierIndex }) }

    fun updateTierMinQty(itemIndex: Int, tierIndex: Int, v: String) =
        updateTierAt(itemIndex, tierIndex) { it.copy(minQty = sanitizeDecimal(v)) }
    fun updateTierPrice(itemIndex: Int, tierIndex: Int, v: String) =
        updateTierAt(itemIndex, tierIndex) { it.copy(unitPrice = sanitizeDecimal(v)) }

    private fun updateItemAt(index: Int, transform: (ItemFormState) -> ItemFormState) =
        _formState.update { st ->
            st.copy(items = st.items.mapIndexed { i, item -> if (i == index) transform(item) else item })
        }

    private fun updateTierAt(itemIndex: Int, tierIndex: Int, transform: (TierFormState) -> TierFormState) =
        updateItemAt(itemIndex) { item ->
            item.copy(tiers = item.tiers.mapIndexed { i, t -> if (i == tierIndex) transform(t) else t })
        }

    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val s = _formState.value
            if (s.name.isBlank()) {
                _formState.update { it.copy(error = "Name is required") }
                return@launch
            }
            if (s.currency.length != 3) {
                _formState.update { it.copy(error = "Currency must be a 3-letter code") }
                return@launch
            }
            if (s.items.any { it.productId.isNotBlank() && it.unitPrice.isBlank() }) {
                _formState.update { it.copy(error = "Each item needs a unit price") }
                return@launch
            }
            _formState.update { it.copy(isLoading = true, error = null) }

            val header = PriceList(
                uid = if (priceListId != null) s.uid else UidGenerator.generateUid("PRC"),
                name = s.name.trim(),
                channel = s.channel,
                customerGroupId = s.customerGroupId.trim().ifBlank { null },
                customerType = s.customerType.trim().ifBlank { null },
                brandId = s.brandId.trim().ifBlank { null },
                categoryId = s.categoryId.trim().ifBlank { null },
                productGroupId = s.productGroupId.trim().ifBlank { null },
                geoZoneId = s.geoZoneId.trim().ifBlank { null },
                currency = s.currency,
                priority = s.priority.toIntOrNull() ?: 0,
                status = s.status,
                active = s.active,
            )
            val items = s.items
                .filter { it.productId.isNotBlank() }
                .map { it.toDomain(header.uid) }

            val result = repository.savePriceList(PriceListAggregate(header, items))
            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _formState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to save") }
            }
        }
    }

    private fun ItemFormState.toDomain(priceListUid: String): PriceListItem = PriceListItem(
        uid = uid.ifBlank { UidGenerator.generateUid("PLI") },
        priceListId = priceListUid,
        productId = productId.trim(),
        variantSku = variantSku.trim().ifBlank { null },
        unitPriceMinor = majorToMinor(unitPrice),
        moq = moq.toDoubleOrNull(),
        tiers = tiers
            .filter { it.minQty.isNotBlank() }
            .map { PriceTier(minQty = it.minQty.toDoubleOrNull() ?: 0.0, unitPriceMinor = majorToMinor(it.unitPrice)) }
            .sortedBy { it.minQty },
        active = true,
    )

    private fun PriceListItem.toFormState(): ItemFormState = ItemFormState(
        uid = uid,
        productId = productId,
        variantSku = variantSku ?: "",
        unitPrice = minorToMajor(unitPriceMinor),
        moq = moq?.let { formatDouble(it) } ?: "",
        tiers = tiers.map { TierFormState(minQty = formatDouble(it.minQty), unitPrice = minorToMajor(it.unitPriceMinor)) },
    )
}

// ── money/number helpers (major units <-> minor) ────────────────────────────────
private fun majorToMinor(text: String): Long = round((text.toDoubleOrNull() ?: 0.0) * 100).toLong()
private fun minorToMajor(minor: Long): String = formatDouble(minor / 100.0)
private fun formatDouble(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

/** Keep only digits and a single decimal point. */
private fun sanitizeDecimal(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    if (firstDot < 0) return filtered
    return filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
}
