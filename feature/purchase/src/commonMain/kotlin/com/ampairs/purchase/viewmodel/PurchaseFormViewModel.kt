package com.ampairs.purchase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.supplier.data.SupplierDataService
import com.ampairs.supplier.domain.SupplierListItem
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.data.PurchaseCostResolver
import com.ampairs.product.domain.ProductSummary
import com.ampairs.purchase.db.PurchaseRepository
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity
import com.ampairs.purchase.domain.PurchaseStatus
import com.ampairs.purchase.util.PurchaseConstants
import com.ampairs.purchase.util.PurchaseLogger
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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * One editable line. Stores backend IDs as Strings (never product object references) per the form
 * rules; `name`/`code` are display-only snapshots. `price` is the buy-side cost per unit.
 */
data class PurchaseItemFormState(
    val id: String,
    val productId: String,
    val name: String,
    val code: String,
    val taxCode: String,
    val taxPercent: Double,
    val quantity: Double,
    val price: Double,
    val mrp: Double,
    val dp: Double,
    val unitId: String,
) {
    val lineTotal: Double get() = quantity * price
}

@OptIn(ExperimentalTime::class)
data class PurchaseFormState(
    val purchaseId: String,
    // The purchase voucher (document) date — the as-of instant the standard cost is resolved at.
    // Defaults to now for a new voucher; reloaded from the persisted purchase_date when editing.
    val purchaseDate: Instant = Clock.System.now(),
    val purchaseNumber: String = "",
    val supplierId: String = "",
    val supplierName: String = "",
    val supplierPhone: String? = null,
    val supplierGst: String = "",
    val supplierInvoiceNumber: String = "",
    val status: PurchaseStatus = PurchaseStatus.DRAFT,
    val items: List<PurchaseItemFormState> = emptyList(),
    // picker results
    val supplierResults: List<SupplierListItem> = emptyList(),
    val productResults: List<ProductSummary> = emptyList(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isEdit: Boolean = false,
) {
    val totalCost: Double get() = items.sumOf { it.lineTotal }
    val totalQuantity: Double get() = items.sumOf { it.quantity }
    val totalItems: Int get() = items.size
}

@OptIn(ExperimentalTime::class)
@AssistedInject
class PurchaseFormViewModel(
    @Assisted private val purchaseId: String?,
    private val repository: PurchaseRepository,
    private val supplierDataService: SupplierDataService,
    private val productDataService: ProductDataService,
    private val purchaseCostResolver: PurchaseCostResolver,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PurchaseFormState(
            // Generate the aggregate UID in the ViewModel (PUR prefix) — never in the repository.
            purchaseId = purchaseId ?: UidGenerator.generateUid(PurchaseConstants.UID_PREFIX),
            isEdit = purchaseId != null,
        )
    )
    val state: StateFlow<PurchaseFormState> = _state.asStateFlow()

    init {
        if (purchaseId != null) loadExisting(purchaseId) else loadNewNumberPreview()
    }

    private fun loadNewNumberPreview() {
        viewModelScope.launch {
            val preview = repository.nextNumberPreview()
            _state.update { it.copy(purchaseNumber = preview) }
        }
    }

    private fun loadExisting(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val parent = repository.getPurchase(id)
            val items = repository.getPurchaseItems(id)
            if (parent != null) {
                _state.update {
                    it.copy(
                        purchaseDate = DateTimeAdapter.fromDateTimeString(parent.purchase_date)
                            ?: it.purchaseDate,
                        purchaseNumber = parent.purchase_number,
                        supplierId = parent.supplier_id,
                        supplierName = parent.supplier_name,
                        supplierPhone = parent.supplier_phone,
                        supplierGst = parent.supplier_gst,
                        supplierInvoiceNumber = parent.supplier_invoice_number ?: "",
                        status = runCatching { PurchaseStatus.valueOf(parent.status.uppercase()) }
                            .getOrDefault(PurchaseStatus.DRAFT),
                        items = items.map { e ->
                            PurchaseItemFormState(
                                id = e.id,
                                productId = e.product_id,
                                name = e.description,
                                code = "",
                                taxCode = e.tax_code,
                                taxPercent = 0.0,
                                quantity = e.quantity,
                                price = e.price,
                                mrp = e.mrp,
                                dp = e.dp,
                                unitId = e.unit_id,
                            )
                        },
                        isLoading = false,
                    )
                }
            } else {
                _state.update { it.copy(isLoading = false) }
            }
        }
    }

    // ───────────────────────── supplier picker ─────────────────────────

    fun searchSuppliers(query: String) {
        viewModelScope.launch {
            val results = runCatching { supplierDataService.listSuppliers(query) }
                .onFailure { PurchaseLogger.w("PurchaseForm", "supplier search failed", it) }
                .getOrDefault(emptyList())
            _state.update { it.copy(supplierResults = results) }
        }
    }

    fun selectSupplier(item: SupplierListItem) {
        viewModelScope.launch {
            val full = runCatching { supplierDataService.getById(item.id) }.getOrNull()
            _state.update {
                it.copy(
                    supplierId = item.id,
                    supplierName = item.name,
                    supplierPhone = item.phone,
                    supplierGst = full?.gstNumber ?: item.gstin ?: "",
                    supplierResults = emptyList(),
                )
            }
        }
    }

    fun setSupplierInvoiceNumber(value: String) =
        _state.update { it.copy(supplierInvoiceNumber = value) }

    fun setStatus(status: PurchaseStatus) = _state.update { it.copy(status = status) }

    // ───────────────────────── line items ─────────────────────────

    fun searchProducts(term: String) {
        viewModelScope.launch {
            val results = runCatching { productDataService.searchSummaries(term) }
                .onFailure { PurchaseLogger.w("PurchaseForm", "product search failed", it) }
                .getOrDefault(emptyList())
            _state.update { it.copy(productResults = results) }
        }
    }

    /**
     * Add a line from a catalog product. The line rate is seeded from the effective-dated standard
     * purchase cost resolved as of the voucher date; when no standard cost applies it falls back to
     * DP (else selling price). Resolution is local/offline.
     */
    fun addProduct(product: ProductSummary) {
        viewModelScope.launch {
            val asOf = _state.value.purchaseDate
            val resolvedCost = runCatching {
                purchaseCostResolver.resolve(
                    productId = product.id,
                    variantSku = null,
                    asOfDate = asOf.toString(),
                )
            }.onFailure {
                PurchaseLogger.w("PurchaseForm", "standard cost resolve failed", it)
            }.getOrNull()
            val catalogFallback = if (product.dp > 0.0) product.dp else product.sellingPrice
            val line = PurchaseItemFormState(
                id = UidGenerator.generateUid(PurchaseConstants.ITEM_UID_PREFIX),
                productId = product.id,
                name = product.name,
                code = product.code,
                taxCode = product.taxCode,
                taxPercent = 0.0,
                quantity = 1.0,
                price = resolvedCost ?: catalogFallback,
                mrp = product.mrp,
                dp = product.dp,
                unitId = product.baseUnitId ?: "",
            )
            _state.update { it.copy(items = it.items + line, productResults = emptyList()) }
        }
    }

    fun updateQuantity(itemId: String, quantity: Double) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == itemId) it.copy(quantity = quantity) else it })
    }

    fun updatePrice(itemId: String, price: Double) = _state.update { s ->
        s.copy(items = s.items.map { if (it.id == itemId) it.copy(price = price) else it })
    }

    fun removeItem(itemId: String) =
        _state.update { s -> s.copy(items = s.items.filterNot { it.id == itemId }) }

    // ───────────────────────── save ─────────────────────────

    fun save(onSaved: (String) -> Unit) {
        val s = _state.value
        if (s.supplierId.isBlank() || s.items.isEmpty()) {
            PurchaseLogger.w("PurchaseForm", "save blocked: supplier or items missing")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            val parent = buildParent(s)
            val items = buildItems(s)
            val result = repository.savePurchase(parent, items)
            _state.update { it.copy(isSaving = false) }
            result.onSuccess { saved -> onSaved(saved.id) }
                .onFailure { PurchaseLogger.e("PurchaseForm", "save failed", it) }
        }
    }

    private fun buildParent(s: PurchaseFormState): PurchaseEntity = PurchaseEntity(
        seq_id = 0,
        id = s.purchaseId,
        purchase_number = s.purchaseNumber.takeIf { it.startsWith("PUR-") } ?: "",
        purchase_date = DateTimeAdapter.toDateTimeString(s.purchaseDate),
        status = s.status.name,
        supplier_id = s.supplierId,
        supplier_name = s.supplierName,
        supplier_gst = s.supplierGst,
        supplier_phone = s.supplierPhone,
        supplier_invoice_number = s.supplierInvoiceNumber.ifBlank { null },
        purchase_type = null,
        place_of_supply = null,
        total_cost = s.totalCost,
        total_tax = 0.0,
        total_items = s.totalItems.toLong(),
        total_quantity = s.totalQuantity,
        base_price = s.totalCost,
        active = 1,
        soft_deleted = 0,
        synced = 0,
    )

    private fun buildItems(s: PurchaseFormState): List<PurchaseItemEntity> =
        s.items.mapIndexed { index, line ->
            PurchaseItemEntity(
                seq_id = 0,
                id = line.id,
                description = line.name,
                product_id = line.productId,
                total_cost = line.lineTotal,
                base_price = line.lineTotal,
                product_price = line.price,
                total_tax = 0.0,
                purchase_id = s.purchaseId,
                tax_code = line.taxCode,
                quantity = line.quantity,
                item_no = index + 1,
                price = line.price,
                mrp = line.mrp,
                dp = line.dp,
                active = 1,
                soft_deleted = 0,
                unit_id = line.unitId,
                base_quantity = line.quantity,
            )
        }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(purchaseId: String?): PurchaseFormViewModel
    }
}
