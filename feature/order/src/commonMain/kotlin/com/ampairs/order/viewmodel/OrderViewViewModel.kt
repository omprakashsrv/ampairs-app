package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.Discount as InvoiceDiscount
import com.ampairs.invoice.domain.TaxInfo as InvoiceTaxInfo
import com.ampairs.invoice.domain.TaxSpec as InvoiceTaxSpec
import com.ampairs.invoice.editor.DocSyncUi
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.TaxInfo as OrderTaxInfo
import com.ampairs.order.domain.TaxSpec as OrderTaxSpec
import kotlinx.coroutines.Dispatchers
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.launch

@AssistedInject
class OrderViewViewModel(
    @Assisted val orderId: String,
    val orderRepository: OrderRepository,
    val invoiceRepository: InvoiceRepository,
    private val syncService: CentralSyncService,
) : ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(orderId: String): OrderViewViewModel
    }

    var order by mutableStateOf(Order())
        private set
    var savingOrder by mutableStateOf(false)
        private set

    /** Live document sync chip: this row's synced flag + the ORDER entity's sync activity. */
    var syncUi by mutableStateOf(DocSyncUi.NONE)
        private set

    init {
        viewModelScope.launch {
            order = orderRepository.getOrder(orderId)
            refreshSyncFlag()
        }
        syncService.observeEntity(SyncEntity.ORDER)
            .onEach { state ->
                when (state?.status) {
                    is SyncStatus.Syncing -> syncUi = DocSyncUi.SYNCING
                    is SyncStatus.Failed -> {
                        refreshSyncFlag()
                        if (syncUi == DocSyncUi.OFFLINE) syncUi = DocSyncUi.FAILED
                    }
                    else -> refreshSyncFlag()
                }
            }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshSyncFlag() {
        if (orderId.isBlank()) return
        syncUi = if (orderRepository.isOrderSynced(orderId)) DocSyncUi.SYNCED else DocSyncUi.OFFLINE
    }

    fun retrySync() {
        syncService.emit(SyncEvent.TriggerPush(SyncEntity.ORDER))
    }

    fun saveOrder() {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            order.let { orderRepository.saveOrder(it) }
            order = orderRepository.getOrder(orderId)
            viewModelScope.launch(Dispatchers.Main) {
                savingOrder = false
            }
        }
    }

    /**
     * Order → Invoice conversion (spec 010 US3), client-side and offline-first:
     * build an invoice from the order's lines/tax/discounts, let InvoiceRepository assign the
     * sequential number, cross-link orderRefId ↔ invoiceRefId, and flag both PENDING_PUSH.
     * Idempotent: if the order is already converted, it's left untouched.
     */
    fun createInvoice() {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            val current = order
            if (current.invoiceRefId.isNullOrBlank()) {
                val invoice = current.toInvoice()
                invoiceRepository.saveInvoice(invoice)        // saves + numbers + marks INVOICE pending
                current.invoiceRefId = invoice.id
                orderRepository.saveOrder(current)            // local-only + marks ORDER pending
            }
            order = orderRepository.getOrder(orderId)
            viewModelScope.launch(Dispatchers.Main) {
                savingOrder = false
            }
        }
    }

    private fun Order.toInvoice(): Invoice {
        val src = this
        return Invoice().apply {
            orderRefId = src.id
            fromCustomer = src.fromCustomer
            toCustomer = src.toCustomer
            // toCustomer setter derives taxSpec with legacy-inverted logic — override it explicitly.
            taxSpec = src.taxSpec.toInvoiceSpec()
            basePrice = src.basePrice
            totalTax = src.totalTax
            discount = src.discount?.map { InvoiceDiscount(it.percent, it.value) }?.toMutableList()
            taxInfos = src.taxInfos?.map { it.toInvoiceTaxInfo() }?.toMutableList()
            items = src.items.map { it.toInvoiceItem() }.toMutableList()  // setter recomputes totals
            totalCost = src.totalCost                                      // keep the order grand total
            totalItems = src.totalItems
            totalQuantity = src.totalQuantity
        }
    }

    private fun OrderItem.toInvoiceItem(): InvoiceItem = InvoiceItem(this.product).apply {
        quantity = this@toInvoiceItem.quantity
        price = this@toInvoiceItem.price
        mrp = this@toInvoiceItem.mrp
        dp = this@toInvoiceItem.dp
        productPrice = this@toInvoiceItem.productPrice
        basePrice = this@toInvoiceItem.basePrice
        totalTax = this@toInvoiceItem.totalTax
        totalCost = this@toInvoiceItem.totalCost
        taxSpec = this@toInvoiceItem.taxSpec.toInvoiceSpec()
        taxInfos = this@toInvoiceItem.taxInfos.map { it.toInvoiceTaxInfo() }
        discountPercent = this@toInvoiceItem.discountPercent
        this@toInvoiceItem.discount.forEach { discount.add(InvoiceDiscount(it.percent, it.value)) }
        unitId = this@toInvoiceItem.unitId
        unitMultiplier = this@toInvoiceItem.unitMultiplier
        baseQuantity = this@toInvoiceItem.baseQuantity
        variantSku = this@toInvoiceItem.variantSku
    }

    private fun OrderTaxInfo.toInvoiceTaxInfo(): InvoiceTaxInfo = InvoiceTaxInfo(
        id = id,
        name = name,
        percentage = percentage,
        formattedName = formattedName,
        taxSpec = taxSpec.toInvoiceSpec(),
        value = value,
    )

    private fun OrderTaxSpec.toInvoiceSpec(): InvoiceTaxSpec =
        if (this == OrderTaxSpec.INTRA) InvoiceTaxSpec.INTRA else InvoiceTaxSpec.INTER
}
