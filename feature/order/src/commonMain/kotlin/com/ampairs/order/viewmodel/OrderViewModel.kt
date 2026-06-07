package com.ampairs.order.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.customer.data.CustomerDataService
import com.ampairs.customer.domain.Customer
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.TaxInfo
import com.ampairs.order.domain.TaxSpec
import com.ampairs.order.domain.asDatabaseModel
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.domain.ProductSummary
import com.ampairs.unit.data.repository.UnitOption
import com.ampairs.unit.data.repository.UnitOptionsLookup
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.tax.calculation.document.DiscountInput
import com.ampairs.tax.calculation.document.DiscountKind
import com.ampairs.tax.calculation.document.DocumentCalcInput
import com.ampairs.tax.calculation.document.DocumentTotalsCalculator
import com.ampairs.tax.calculation.document.LineCalcInput
import com.ampairs.tax.calculation.document.OverallDiscountMode
import com.ampairs.tax.calculation.document.PriceMode
import com.ampairs.tax.calculation.document.ScenarioResolver
import com.ampairs.tax.calculation.document.TaxRateProvider
import com.ampairs.tax.calculation.document.TaxScenario
import com.ampairs.order.ui.TaxGroupUi
import com.ampairs.order.ui.TotalsUi
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.launch


@AssistedInject
class OrderViewModel(
    @Assisted fromCustomerId: String?, @Assisted toCustomerId: String?, @Assisted id: String?,
    val customerDataService: CustomerDataService,
    val orderRepository: OrderRepository,
    val productDataService: ProductDataService,
    val tokenRepository: TokenRepository,
    val taxRateProvider: TaxRateProvider,
    val unitOptionsLookup: UnitOptionsLookup,
) :
    ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(fromCustomerId: String?, toCustomerId: String?, id: String?): OrderViewModel
    }

    fun updateOrderItems(products: List<ProductSummary>) {
        orderItems.removeAll(orderItems.filter { orderItem ->
            !products.map { it.id }.contains(orderItem.product?.id)
        })
        products.forEach { product ->
            val item = orderItems.find { orderItem -> orderItem.product?.id == product.id }
            if (item != null) {
                item.quantity = product.quantity
            } else {
                orderItems.add(OrderItem(product))
            }
        }
        orderItems.removeAll(orderItems.filter { orderItem -> orderItem.quantity <= 0 })
        order.items = orderItems
        recalculate()
    }

    /** Recompute GST + discount totals through the shared calculator and push them into UI state. */
    fun recalculate() {
        viewModelScope.launch(DispatcherProvider.io) { computeTotals() }
    }

    /** Load the sellable unit choices for the given line's product (base unit first). */
    fun loadUnitOptions(item: OrderItem) {
        viewModelScope.launch(DispatcherProvider.io) {
            val productId = item.product?.id ?: item.productId
            if (productId.isNullOrBlank()) { unitOptions = emptyList(); return@launch }
            val baseUnitId = item.product?.baseUnitId ?: item.unitId.takeIf { it.isNotBlank() }
            unitOptions = unitOptionsLookup.unitsForProduct(productId, baseUnitId)
        }
    }

    /** Apply a unit choice to a line: rescales price + base quantity, then recomputes totals. */
    fun selectUnit(item: OrderItem, option: UnitOption) {
        item.selectUnit(option.unitId, option.shortName, option.multiplier)
        order.updateTotalCost()
        recalculate()
    }

    fun selectPriceMode(mode: PriceMode) { priceMode = mode; recalculate() }

    fun setOverallDiscount(kind: DiscountKind, amount: Double) {
        overallDiscountKind = kind
        overallDiscountAmount = amount
        recalculate()
    }

    fun selectOverallDiscountMode(mode: OverallDiscountMode) { overallDiscountMode = mode; recalculate() }

    /** Add a product from the picker: new line (qty 1) or increment the existing line, then recompute. */
    fun addProduct(productId: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val existing = orderItems.find { it.product?.id == productId }
            if (existing != null) {
                existing.quantity = existing.quantity + 1.0
            } else {
                val product = productDataService.getById(productId) ?: return@launch
                val item = OrderItem(product)
                if (item.quantity <= 0.0) item.quantity = 1.0
                orderItems.add(item)
            }
            order.items = orderItems
            computeTotals()
        }
    }

    fun saveOrder(onOrderSaved: (String) -> Unit) {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            computeTotals()
            val userId = tokenRepository.getCurrentUserId() ?: ""
            if (order.createdBy.isEmpty()) {
                order.createdBy = userId
            }
            order.updatedBy = userId
            val orderEntity = order.asDatabaseModel()
            orderRepository.saveOrder(orderEntity, orderItems.asDatabaseModel(orderEntity.id))
            onOrderSaved(orderEntity.id)
            savingOrder = false
        }
    }

    private suspend fun computeTotals() {
        val scenario = ScenarioResolver.resolveFromGstins(
            sellerGstin = order.fromCustomer?.gstNumber,
            buyerGstin = order.toCustomer?.gstNumber,
        )
        val taxCodes = orderItems.mapNotNull { it.product?.taxCode }
        val rates = taxRateProvider.resolveAll(taxCodes, scenario)

        val lines = orderItems.map { item ->
            LineCalcInput(
                id = item.id,
                taxCode = item.product?.taxCode ?: "",
                unitPrice = item.price,
                quantity = item.quantity,
                lineDiscount = item.discount.firstOrNull().toDiscountInput(),
            )
        }
        val overall = if (overallDiscountAmount > 0.0) DiscountInput(overallDiscountKind, overallDiscountAmount) else null
        val input = DocumentCalcInput(
            lines = lines,
            priceMode = priceMode,
            overallDiscount = overall,
            overallDiscountMode = overallDiscountMode,
            scenario = scenario,
            rates = rates,
        )
        val result = DocumentTotalsCalculator.calculate(input)
        val spec = if (scenario == TaxScenario.INTRA) TaxSpec.INTRA else TaxSpec.INTER

        val byId = result.lines.associateBy { it.id }
        orderItems.forEach { item ->
            val line = byId[item.id] ?: return@forEach
            item.basePrice = line.taxable
            item.totalTax = line.totalTax
            item.totalCost = line.lineTotal
            item.taxInfos = line.components.map { c ->
                TaxInfo(name = c.name, percentage = c.percentage, taxSpec = spec, value = c.amount)
            }
        }
        order.taxSpec = spec
        order.basePrice = result.taxableSubtotal
        order.totalTax = result.totalTax
        order.taxInfos = result.taxComponents.map { c ->
            TaxInfo(name = c.name, percentage = 0.0, taxSpec = spec, value = c.amount)
        }.toMutableList()
        order.totalItems = orderItems.size
        order.totalQuantity = orderItems.sumOf { it.quantity }
        order.totalCost = result.grandTotal

        // also persist the chosen modes onto the order for save round-trip
        order.discount = overall?.let { mutableListOf(com.ampairs.order.domain.Discount(if (it.kind == DiscountKind.PERCENT) it.amount else 0.0, if (it.kind == DiscountKind.FLAT) it.amount else 0.0)) }

        val groups = result.lines.flatMap { it.components }
            .groupBy { it.name to it.percentage }
            .map { (k, v) -> TaxGroupUi(name = k.first, percentage = k.second, amount = v.sumOf { c -> c.amount }) }
            .sortedWith(compareBy({ taxOrder(it.name) }, { it.percentage }))
        totals = TotalsUi(
            subtotalGross = result.lines.sumOf { it.gross },
            lineDiscountTotal = result.lineDiscountTotal,
            overallDiscountValue = result.overallDiscountValue,
            taxableSubtotal = result.taxableSubtotal,
            taxGroups = groups,
            totalTax = result.totalTax,
            grandTotal = result.grandTotal,
            scenario = scenario,
            priceMode = priceMode,
            overallDiscountMode = overallDiscountMode,
            itemCount = orderItems.size,
        )
    }

    private fun taxOrder(name: String): Int = when (name) {
        "CGST" -> 0; "SGST" -> 1; "IGST" -> 2; else -> 3
    }

    private fun com.ampairs.order.domain.Discount?.toDiscountInput(): DiscountInput? = when {
        this == null -> null
        percent > 0.0 -> DiscountInput(DiscountKind.PERCENT, percent)
        value > 0.0 -> DiscountInput(DiscountKind.FLAT, value)
        else -> null
    }

    var fromCustomer: Customer? = null
    var toCustomer: Customer? = null
    val orderItems = mutableStateListOf<OrderItem>()
    var selectedOrderItem by mutableStateOf<OrderItem?>(null)
    var savingOrder by mutableStateOf(false)
    var order = Order()

    // Document settings (spec 010 C1/C2) — observable so the totals panel reacts.
    var priceMode by mutableStateOf(PriceMode.TAX_EXCLUSIVE)
        private set
    var overallDiscountMode by mutableStateOf(OverallDiscountMode.POST_TAX_REDUCTION)
        private set
    var overallDiscountKind by mutableStateOf(DiscountKind.PERCENT)
        private set
    var overallDiscountAmount by mutableStateOf(0.0)
        private set
    var totals by mutableStateOf(TotalsUi())
        private set

    // Unit choices for the currently edited line (loaded on demand).
    var unitOptions by mutableStateOf<List<UnitOption>>(emptyList())
        private set

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            if (!id.isNullOrEmpty()) {
                order = orderRepository.getOrder(id)
                fromCustomer = order.fromCustomer
                toCustomer = order.toCustomer
                orderItems.addAll(order.items)
            } else {
                fromCustomer =
                    fromCustomerId?.let { customerDataService.getById(it) }
                toCustomer =
                    toCustomerId?.let { customerDataService.getById(it) }
                order.fromCustomer = fromCustomer
                order.toCustomer = toCustomer
            }
            computeTotals()
        }
    }
}
