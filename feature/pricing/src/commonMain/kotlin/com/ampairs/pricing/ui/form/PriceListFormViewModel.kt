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

/** Header-level form state for a price list. Items are preserved across edits (edited elsewhere). */
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
    val itemCount: Int = 0,
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

    // Items aren't edited in this header form; preserve them so a header save doesn't wipe them.
    private var preservedItems: List<PriceListItem> = emptyList()

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
            preservedItems = aggregate.items
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
                    itemCount = aggregate.items.size,
                    isLoading = false,
                )
            }
        }
    }

    fun updateName(v: String) = _formState.update { it.copy(name = v, error = null) }
    fun updateChannel(v: SalesChannel) = _formState.update { it.copy(channel = v) }
    fun updateCustomerGroupId(v: String) = _formState.update { it.copy(customerGroupId = v) }
    fun updateCustomerType(v: String) = _formState.update { it.copy(customerType = v) }
    fun updateBrandId(v: String) = _formState.update { it.copy(brandId = v) }
    fun updateCategoryId(v: String) = _formState.update { it.copy(categoryId = v) }
    fun updateProductGroupId(v: String) = _formState.update { it.copy(productGroupId = v) }
    fun updateGeoZoneId(v: String) = _formState.update { it.copy(geoZoneId = v) }
    fun updateCurrency(v: String) = _formState.update { it.copy(currency = v.uppercase(), error = null) }
    fun updatePriority(v: String) = _formState.update { it.copy(priority = v.filter { c -> c.isDigit() }, error = null) }
    fun updateStatus(v: PriceListStatus) = _formState.update { it.copy(status = v) }
    fun updateActive(v: Boolean) = _formState.update { it.copy(active = v) }

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
            // Re-point preserved items at the (possibly new) list uid.
            val items = preservedItems.map { it.copy(priceListId = header.uid) }
            val result = repository.savePriceList(PriceListAggregate(header, items))
            if (result.isSuccess) {
                _formState.update { it.copy(isLoading = false) }
                onSuccess()
            } else {
                _formState.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message ?: "Failed to save") }
            }
        }
    }
}
