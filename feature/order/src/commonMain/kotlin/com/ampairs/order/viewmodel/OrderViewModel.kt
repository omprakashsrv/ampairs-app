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
import com.ampairs.invoice.editor.ComposerResultUi
import com.ampairs.invoice.editor.ComposerUiState
import com.ampairs.invoice.editor.DocBaseUnitChoice
import com.ampairs.invoice.editor.DocCustomerUi
import com.ampairs.invoice.editor.DocLineUi
import com.ampairs.invoice.editor.DocSyncUi
import com.ampairs.invoice.editor.DocUnitChoiceUi
import com.ampairs.invoice.editor.DocVariantChoiceUi
import com.ampairs.invoice.editor.entry.ComposerEngine
import com.ampairs.invoice.editor.entry.EntryMatcher
import com.ampairs.invoice.editor.entry.EntryPreview
import com.ampairs.invoice.editor.entry.EntryUnit
import com.ampairs.order.db.OrderRepository
import com.ampairs.order.domain.Discount
import com.ampairs.order.domain.Order
import com.ampairs.order.domain.OrderItem
import com.ampairs.order.domain.TaxInfo
import com.ampairs.order.domain.TaxSpec
import com.ampairs.order.domain.asDatabaseModel
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.product.data.ProductDataService
import com.ampairs.product.data.PriceResolver
import com.ampairs.product.data.PriceResolutionInput
import com.ampairs.product.domain.Constants
import com.ampairs.product.domain.ProductSummary
import com.ampairs.store.domain.StoreSettingsProvider
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import com.ampairs.sync.SyncStatus
import com.ampairs.unit.data.repository.UnitLookup
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
import com.ampairs.invoice.ui.TaxGroupUi
import com.ampairs.invoice.ui.TotalsUi
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ViewModel for the v2 "fast entry" order editor (spec 010). Lines are added through the
 * command-bar composer; every edit re-runs calc-core ([DocumentTotalsCalculator]) and republishes
 * [lineUis] + [totals]. The workspace price mode is observed from settings (no editor toggle) and
 * snapshotted onto the document at save.
 */
@AssistedInject
class OrderViewModel(
    @Assisted customerId: String?, @Assisted id: String?,
    val customerDataService: CustomerDataService,
    val orderRepository: OrderRepository,
    val productDataService: ProductDataService,
    val priceResolver: PriceResolver,
    val tokenRepository: TokenRepository,
    val taxRateProvider: TaxRateProvider,
    val unitOptionsLookup: UnitOptionsLookup,
    val unitLookup: UnitLookup,
    val storeSettings: StoreSettingsProvider,
    val syncService: CentralSyncService,
) :
    ViewModel() {

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(customerId: String?, id: String?): OrderViewModel
    }

    // ───────────────────────── core document state ─────────────────────────

    var customer: Customer? = null
    val orderItems = mutableStateListOf<OrderItem>()
    var savingOrder by mutableStateOf(false)

    /** Number shown in the editor header: existing order's number, or the next this device would assign. */
    var numberPreview by mutableStateOf<String?>(null)
    var order = Order()

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
    var showDiscount by mutableStateOf(true)
        private set

    /** Computed line UI rows for the shared editor (rebuilt by every recalculation). */
    var lineUis by mutableStateOf<List<DocLineUi>>(emptyList())
        private set
    var customerUi by mutableStateOf<DocCustomerUi?>(null)
        private set
    var dateLabel by mutableStateOf("")
        private set
    var syncUi by mutableStateOf(DocSyncUi.NONE)
        private set

    // Supply-origin (seller) state is owned by the tax module, not the order/invoice flow, and is
    // not stored on the document. Until the tax module supplies it, the editor treats the scenario
    // as intra-state by default. The buyer's GSTIN (place of supply) still drives ScenarioResolver.
    val sellerStateCode: Int? = null

    private var customerWalkIn = false
    private var savedOnce = false
    private var ratePercents: Map<String, Double?> = emptyMap()
    private val engine by lazy { ComposerEngine(productDataService, unitOptionsLookup, taxRateProvider) }

    // ───────────────────────── composer (command bar) ─────────────────────────

    var composer by mutableStateOf(ComposerUiState())
        private set
    private val recents = ArrayDeque<String>()
    private var composerJob: Job? = null
    private var flashJob: Job? = null
    private var lastComputation: ComposerEngine.Computation? = null

    fun composerQueryChanged(query: String) {
        composer = composer.copy(query = query, flash = null)
        composerJob?.cancel()
        if (query.isBlank()) {
            lastComputation = null
            composer = composer.copy(results = emptyList(), highlight = 0, noMatch = false, preview = null, typedWords = "")
            return
        }
        composerJob = viewModelScope.launch(DispatcherProvider.io) {
            val computation = engine.compute(query, totals.scenario, recents.toList())
            val results = computation.previews.map { it.toResultUi(computation.ratePercents) }
            lastComputation = computation
            if (composer.query == query) {
                composer = composer.copy(
                    results = results,
                    highlight = 0,
                    noMatch = computation.noMatch,
                    typedWords = computation.parsed.words.joinToString(" "),
                    preview = results.firstOrNull(),
                )
            }
        }
    }

    fun composerMoveHighlight(delta: Int) {
        if (composer.results.isEmpty()) return
        val next = (composer.highlight + delta).coerceIn(0, composer.results.lastIndex)
        composer = composer.copy(highlight = next, preview = composer.results[next])
    }

    /** Commit the highlighted result. Returns false when there is no match (caller opens create). */
    fun composerCommitHighlighted(): Boolean {
        val computation = lastComputation ?: return true
        if (computation.noMatch) return false
        if (composer.results.isEmpty()) return true
        composerCommitAt(composer.highlight)
        return true
    }

    fun composerCommitAt(index: Int) {
        val computation = lastComputation ?: return
        val preview = computation.previews.getOrNull(index) ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            commitPreview(preview)
        }
    }

    /**
     * One line per product: if the product is already on the order, increment that line's quantity
     * instead of appending a duplicate. Matches on the line's product id — including lines reloaded
     * from Room, where the catalog product isn't attached (product == null) but productId is set.
     */
    private suspend fun commitPreview(preview: EntryPreview) {
        val mergeable = orderItems.find { item ->
            (item.product?.id ?: item.productId) == preview.product.id
        }
        if (mergeable != null) {
            mergeable.quantity = EntryMatcher.clampToDecimals(mergeable.quantity + preview.quantity, preview.unit.decimalPlaces)
        } else {
            val item = OrderItem(preview.product)
            item.quantity = preview.quantity
            item.selectUnit(preview.unit.unitId, preview.unit.name, preview.unit.multiplier)
            if (preview.priceOverridden) {
                item.price = preview.unitPrice
                item.priceOverridden = true
            }
            if (preview.discountPercent > 0.0) {
                item.discountPercent = preview.discountPercent
                item.discount.add(Discount(preview.discountPercent, 0.0))
            }
            orderItems.add(item)
        }
        recents.remove(preview.product.id)
        recents.addFirst(preview.product.id)
        while (recents.size > 5) recents.removeLast()

        order.items = orderItems
        composer = ComposerUiState(flash = "${preview.product.name} · ${trimQty(preview.quantity)} ${preview.unit.name}")
        lastComputation = null
        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(1400)
            composer = composer.copy(flash = null)
        }
        computeTotals()
    }

    private fun trimQty(q: Double): String = if (q % 1.0 == 0.0) "${q.toInt()}" else "$q"

    private fun EntryPreview.toResultUi(rates: Map<String, Double?>): ComposerResultUi = ComposerResultUi(
        productId = product.id,
        name = product.name,
        hsn = product.taxCode,
        gstRatePercent = rates[product.taxCode],
        barcode = product.code.takeIf { it.length >= 8 && it.all(Char::isDigit) },
        quantity = quantity,
        unitName = unit.name,
        unitPrice = unitPrice,
        priceOverridden = priceOverridden,
        discountPercent = discountPercent,
        amount = amount,
    )

    // ───────────────────────── line intents ─────────────────────────

    fun setLineQuantity(lineId: String, quantity: Double) {
        val item = orderItems.find { it.id == lineId } ?: return
        item.quantity = quantity.coerceAtLeast(0.0)
        order.updateTotalCost()
        recalculate()
    }

    fun setLineUnitPrice(lineId: String, price: Double) {
        val item = orderItems.find { it.id == lineId } ?: return
        item.price = price.coerceAtLeast(0.0)
        item.priceOverridden = true
        item.updateTotal()
        order.updateTotalCost()
        recalculate()
    }

    fun setLineDiscount(lineId: String, kind: DiscountKind, amount: Double) {
        val item = orderItems.find { it.id == lineId } ?: return
        item.discount.clear()
        if (amount > 0.0) {
            when (kind) {
                DiscountKind.PERCENT -> { item.discountPercent = amount; item.discount.add(Discount(amount, 0.0)) }
                DiscountKind.FLAT -> { item.discountPercent = 0.0; item.discount.add(Discount(0.0, amount)) }
            }
        } else {
            item.discountPercent = 0.0
        }
        // keep the chosen kind visible in the UI even at zero
        lineDiscountKinds[lineId] = kind
        recalculate()
    }

    fun removeLine(lineId: String) {
        orderItems.removeAll { it.id == lineId }
        lineDiscountKinds.remove(lineId)
        order.items = orderItems
        recalculate()
    }

    private val lineDiscountKinds = mutableMapOf<String, DiscountKind>()

    // ───────────────────────── unit / variant / product pickers ─────────────────────────

    var unitChoices by mutableStateOf<List<DocUnitChoiceUi>>(emptyList())
        private set
    var variantChoices by mutableStateOf<List<DocVariantChoiceUi>>(emptyList())
        private set

    fun loadUnitChoicesFor(lineId: String) {
        val item = orderItems.find { it.id == lineId } ?: return
        val product = item.product ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            unitChoices = engine.unitsFor(product).map { u ->
                DocUnitChoiceUi(
                    unitId = u.unitId,
                    name = u.name,
                    multiplier = u.multiplier,
                    priceAtUnit = item.productPrice * u.multiplier,
                    decimalPlaces = u.decimalPlaces,
                    isBase = u.isBase,
                )
            }
        }
    }

    fun loadVariantChoicesFor(lineId: String) {
        val item = orderItems.find { it.id == lineId } ?: return
        val productId = item.product?.id ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val basePrice = item.product?.sellingPrice ?: 0.0
            variantChoices = productDataService.variantsForProduct(productId).map { v ->
                DocVariantChoiceUi(sku = v.sku, label = v.label, price = v.sellingPrice ?: basePrice)
            }
        }
    }

    fun selectUnitFor(lineId: String, unitId: String) {
        val item = orderItems.find { it.id == lineId } ?: return
        val product = item.product ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val unit = engine.unitsFor(product).find { it.unitId == unitId } ?: return@launch
            item.selectUnit(unit.unitId, unit.name, unit.multiplier)
            item.quantity = EntryMatcher.clampToDecimals(item.quantity, unit.decimalPlaces)
            order.updateTotalCost()
            computeTotals()
        }
    }

    fun selectVariantFor(lineId: String, sku: String) {
        val item = orderItems.find { it.id == lineId } ?: return
        val productId = item.product?.id ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val variant = productDataService.variantsForProduct(productId).find { it.sku == sku } ?: return@launch
            item.selectVariant(variant.sku, variant.sellingPrice)
            order.updateTotalCost()
            computeTotals()
        }
    }

    fun changeLineProduct(lineId: String, productId: String) {
        val item = orderItems.find { it.id == lineId } ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val product = productDataService.getById(productId) ?: return@launch
            // Resolve the line price through the PriceResolver seam (spec 009), supplying the order's
            // customer + channel context. Walk-in => RETAIL, a named customer => WHOLESALE. Falls back
            // to product.sellingPrice when no price list matches.
            val orderCustomer = order.customer
            val resolved = priceResolver.resolve(
                PriceResolutionInput(
                    productId = product.id,
                    variantSku = null,
                    quantity = item.quantity,
                    fallbackUnitPrice = product.sellingPrice,
                    channel = if (customerWalkIn || orderCustomer == null) "RETAIL" else "WHOLESALE",
                    customerId = orderCustomer?.uid,
                    customerGroupId = orderCustomer?.customerGroup,
                    customerType = orderCustomer?.customerType,
                    pincode = orderCustomer?.pincode,
                    // Resolve the price as of the order's document date (effective-dated pricing).
                    asOfDate = order.orderDate.toString(),
                ),
            )
            item.product = product
            item.productId = product.id
            item.description = product.name + " " + product.code
            item.productPrice = resolved.unitPrice
            item.priceOverridden = false
            item.variantSku = null
            // Capture the resolution snapshot so it persists to Room and pushes verbatim on /sync.
            item.resolvedUnitPriceMinor = resolved.resolvedUnitPriceMinor
            item.currency = resolved.currency
            item.priceSource = resolved.priceSource
            item.matchedPriceListUid = resolved.matchedPriceListUid
            item.appliedTierMinQty = resolved.appliedTierMinQty
            item.belowMoq = resolved.belowMoq
            val base = engine.unitsFor(product).firstOrNull()
            if (base != null) {
                item.selectUnit(base.unitId, base.name, base.multiplier)
            } else {
                item.price = resolved.unitPrice
                item.updateTotal()
            }
            order.updateTotalCost()
            computeTotals()
        }
    }

    // product picker (change-product flow)
    var productResults by mutableStateOf<List<ProductSummary>>(emptyList())
        private set
    var productRatePercents by mutableStateOf<Map<String, Double?>>(emptyMap())
        private set

    fun searchProducts(query: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val results = productDataService.searchSummaries(query, 50)
            productResults = results
            productRatePercents = engine.ratePercentsFor(results.map { it.taxCode }, totals.scenario)
        }
    }

    // ───────────────────────── inline create product ─────────────────────────

    var baseUnits by mutableStateOf<List<DocBaseUnitChoice>>(emptyList())
        private set
    var createHsnRatePercent by mutableStateOf<Double?>(null)
        private set

    fun resolveCreateHsn(code: String) {
        if (code.isBlank()) { createHsnRatePercent = null; return }
        viewModelScope.launch(DispatcherProvider.io) {
            createHsnRatePercent = engine.ratePercentsFor(listOf(code), totals.scenario)[code]
        }
    }

    /** Inline-create (4 fields) and land as a line. UID minted here; saves offline (PENDING_PUSH). */
    fun createProductInline(name: String, price: Double, taxCode: String, baseUnitId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch(DispatcherProvider.io) {
            val uid = UidGenerator.generateUid(Constants.PRODUCT_PREFIX)
            val summary = productDataService.quickCreate(
                id = uid, name = name, code = "",
                sellingPrice = price, mrp = price, taxCode = taxCode, baseUnitId = baseUnitId,
            ) ?: return@launch
            engine.invalidate()
            val item = OrderItem(summary)
            item.quantity = 1.0
            val base = engine.unitsFor(summary).firstOrNull()
            if (base != null) item.selectUnit(base.unitId, base.name, base.multiplier)
            orderItems.add(item)
            recents.addFirst(summary.id)
            while (recents.size > 5) recents.removeLast()
            order.items = orderItems
            computeTotals()
        }
    }

    // ───────────────────────── customer selection ─────────────────────────

    var customerName by mutableStateOf("")
        private set
    var customerResults by mutableStateOf<List<com.ampairs.customer.domain.CustomerListItem>>(emptyList())
        private set

    fun searchCustomers(query: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            customerResults = customerDataService.listCustomers(query)
        }
    }

    fun selectCustomer(customerId: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val selected = customerDataService.getById(customerId) ?: return@launch
            customer = selected
            customerWalkIn = false
            order.customer = selected
            customerName = selected.name
            computeTotals()
        }
    }

    /** Walk-in buyer (spec 010 v2): no customer record; no GSTIN ⇒ intra vs the seller state. */
    fun useWalkInCustomer(name: String, phone: String, gstin: String) {
        val walkIn = Customer(
            uid = "",
            name = name.ifBlank { "Walk-in" },
            phone = phone.trim().ifBlank { null },
            gstNumber = gstin.trim().ifBlank { null },
        )
        customer = walkIn
        customerWalkIn = true
        order.customer = walkIn
        customerName = walkIn.name
        recalculate()
    }

    // ───────────────────────── totals / recalculation ─────────────────────────

    fun recalculate() {
        viewModelScope.launch(DispatcherProvider.io) { computeTotals() }
    }

    fun setOverallDiscount(kind: DiscountKind, amount: Double) {
        overallDiscountKind = kind
        overallDiscountAmount = amount
        recalculate()
    }

    fun selectOverallDiscountMode(mode: OverallDiscountMode) { overallDiscountMode = mode; recalculate() }

    fun selectPriceMode(mode: PriceMode) { priceMode = mode; recalculate() }

    private suspend fun computeTotals() {
        // IGST vs CGST+SGST is purely the place-of-supply diff: seller (origin) state vs buyer
        // (destination) state — both snapshotted on the document. Fall back to the GSTIN state only
        // when an explicit place-of-supply is absent. Unknown → intra-state default.
        val supplierState = order.sellerPlaceOfSupply?.toIntOrNull()
            ?: ScenarioResolver.stateCodeFromGstin(order.sellerGst)
        val posState = order.placeOfSupply?.toIntOrNull()
            ?: ScenarioResolver.stateCodeFromGstin(order.customer?.gstNumber)
        val scenario = ScenarioResolver.resolve(supplierState, posState)
        val taxCodes = orderItems.mapNotNull { it.product?.taxCode }
        val rates = taxRateProvider.resolveAll(taxCodes, scenario)
        ratePercents = rates.mapValues { (_, v) -> v.totalRate }

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
            // keep the resolved discount value on the item for the save round-trip
            val disc = item.discount.firstOrNull()
            if (disc != null && line.lineDiscountValue > 0.0) {
                item.discount[0] = Discount(disc.percent, line.lineDiscountValue)
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
        order.discount = overall?.let {
            mutableListOf(
                Discount(
                    if (it.kind == DiscountKind.PERCENT) it.amount else 0.0,
                    result.overallDiscountValue,
                )
            )
        }

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
        publishLineUis(scenario)
        publishCustomerUi(scenario)
    }

    private suspend fun publishLineUis(scenario: TaxScenario) {
        lineUis = orderItems.map { item ->
            val units = item.product?.let { engine.unitsFor(it) } ?: emptyList()
            val selected = units.find { it.unitId == item.unitId || it.name == item.unitName }
            val base = units.firstOrNull()
            val disc = item.discount.firstOrNull()
            val kind = lineDiscountKinds[item.id] ?: when {
                disc == null -> DiscountKind.PERCENT
                disc.percent > 0.0 -> DiscountKind.PERCENT
                else -> DiscountKind.FLAT
            }
            DocLineUi(
                id = item.id,
                name = item.product?.name ?: item.description,
                hsn = item.product?.taxCode ?: "",
                gstRatePercent = ratePercents[item.product?.taxCode],
                hasVariants = item.product?.hasVariants == true,
                variantSku = item.variantSku,
                unitName = item.unitName.ifBlank { selected?.name ?: base?.name ?: "" },
                unitMultiplier = item.unitMultiplier,
                baseUnitName = base?.name ?: "",
                decimalPlaces = selected?.decimalPlaces ?: base?.decimalPlaces ?: 0,
                quantity = item.quantity,
                baseQuantity = item.baseQuantity,
                unitPrice = item.price,
                priceOverridden = item.priceOverridden,
                catalogUnitPrice = item.productPrice * item.unitMultiplier,
                discountKind = kind,
                discountAmount = when {
                    disc == null -> 0.0
                    disc.percent > 0.0 -> disc.percent
                    else -> disc.value
                },
                taxable = item.basePrice,
                totalTax = item.totalTax,
                lineTotal = item.totalCost,
                belowMoq = item.belowMoq,
            )
        }
    }

    private fun publishCustomerUi(scenario: TaxScenario) {
        customerUi = customer?.let {
            val stateCode = ScenarioResolver.stateCodeFromGstin(it.gstNumber)
            DocCustomerUi(
                id = it.uid,
                name = it.name,
                initials = it.name.split(' ').filter { w -> w.isNotBlank() }.take(2)
                    .joinToString("") { w -> w.first().uppercase() }.ifBlank { "?" },
                stateLabel = it.state?.takeIf { s -> s.isNotBlank() }?.let { s -> stateCode?.let { c -> "$s ($c)" } ?: s }
                    ?: stateCode?.toString(),
                intra = scenario == TaxScenario.INTRA,
                walkIn = customerWalkIn,
            )
        }
    }

    private fun taxOrder(name: String): Int = when (name) {
        "CGST" -> 0; "SGST" -> 1; "IGST" -> 2; else -> 3
    }

    private fun Discount?.toDiscountInput(): DiscountInput? = when {
        this == null -> null
        percent > 0.0 -> DiscountInput(DiscountKind.PERCENT, percent)
        value > 0.0 -> DiscountInput(DiscountKind.FLAT, value)
        else -> null
    }

    // ───────────────────────── save / sync ─────────────────────────

    fun saveOrder(onOrderSaved: (String) -> Unit) {
        savingOrder = true
        viewModelScope.launch(DispatcherProvider.io) {
            computeTotals()
            val userId = tokenRepository.getCurrentUserId() ?: ""
            if (order.createdBy.isEmpty()) {
                order.createdBy = userId
            }
            order.updatedBy = userId
            // Default place of supply to the buyer's GSTIN state, else the buyer's address state.
            if (order.placeOfSupply.isNullOrBlank()) {
                order.placeOfSupply = ScenarioResolver.stateCodeFromGstin(order.customer?.gstNumber)?.toString()
                    ?: order.customer?.state
            }
            // Seller (origin) place of supply: from the snapshotted seller GSTIN (stamped from the
            // tax registration). Stored so the IGST decision is a pure compare on re-edit.
            if (order.sellerPlaceOfSupply.isNullOrBlank()) {
                order.sellerPlaceOfSupply = ScenarioResolver.stateCodeFromGstin(order.sellerGst)?.toString()
            }
            // snapshot the active modes onto the document (spec 010 C1/C2)
            val orderEntity = order.asDatabaseModel().copy(
                price_mode = priceMode.name,
                overall_discount_mode = overallDiscountMode.name,
            )
            orderRepository.saveOrder(orderEntity, orderItems.asDatabaseModel(orderEntity.id))
            savedOnce = true
            syncUi = DocSyncUi.OFFLINE
            onOrderSaved(orderEntity.id)
            savingOrder = false
        }
    }

    fun retrySync() {
        syncService.emit(SyncEvent.TriggerPush(SyncEntity.ORDER))
    }

    // ───────────────────────── legacy API (kept for other call sites) ─────────────────────────

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

    /** Add a product from the picker: new line (qty 1) or increment the existing line. */
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

    var unitOptions by mutableStateOf<List<UnitOption>>(emptyList())
        private set
    var selectedOrderItem by mutableStateOf<OrderItem?>(null)

    fun loadUnitOptions(item: OrderItem) {
        viewModelScope.launch(DispatcherProvider.io) {
            val productId = item.product?.id ?: item.productId
            if (productId.isNullOrBlank()) { unitOptions = emptyList(); return@launch }
            val baseUnitId = item.product?.baseUnitId ?: item.unitId.takeIf { it.isNotBlank() }
            unitOptions = unitOptionsLookup.unitsForProduct(productId, baseUnitId)
        }
    }

    fun selectUnit(item: OrderItem, option: UnitOption) {
        item.selectUnit(option.unitId, option.shortName, option.multiplier)
        order.updateTotalCost()
        recalculate()
    }

    fun createAndAddProduct(name: String, code: String, price: Double, mrp: Double, taxCode: String) {
        if (name.isBlank()) return
        viewModelScope.launch(DispatcherProvider.io) {
            val uid = UidGenerator.generateUid(Constants.PRODUCT_PREFIX)
            val summary = productDataService.quickCreate(
                id = uid, name = name, code = code,
                sellingPrice = price, mrp = mrp, taxCode = taxCode, baseUnitId = null,
            ) ?: return@launch
            val item = OrderItem(summary)
            if (item.quantity <= 0.0) item.quantity = 1.0
            orderItems.add(item)
            order.items = orderItems
            computeTotals()
        }
    }

    // ───────────────────────── init ─────────────────────────

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            // One-shot defaults before the first totals computation.
            showDiscount = storeSettings.getBoolean("order", "show_discount_options", default = true)
            if (!id.isNullOrEmpty()) {
                order = orderRepository.getOrder(id)
                customer = order.customer
                orderItems.addAll(order.items)
                // unitName is transient (display-only, not persisted) — restore it from the unit
                // catalog so re-opened lines show their unit even when the catalog product isn't
                // attached (publishLineUis otherwise resolves the name only via the product's units).
                orderItems.forEach { item ->
                    if (item.unitName.isBlank() && item.unitId.isNotBlank()) {
                        unitLookup.getUnitById(item.unitId)?.let { u -> item.unitName = u.shortName.ifBlank { u.name } }
                    }
                }
            } else {
                customer = customerId?.let { customerDataService.getById(it) }
                order.customer = customer
            }
            customerName = customer?.name ?: ""
            numberPreview = order.orderNumber?.takeIf { it.isNotBlank() }
                ?: orderRepository.nextNumberPreview()
            dateLabel = formatDocDate()
            baseUnits = unitLookup.getActiveUnits().map { DocBaseUnitChoice(it.uid, it.shortName.ifBlank { it.name }) }
            computeTotals()
        }
        // Workspace price mode is observed (spec 010 v2): changing the setting recomputes open drafts.
        storeSettings.observeBoolean("common", "prices_include_tax", default = false)
            .onEach { inclusive ->
                val mode = if (inclusive) PriceMode.TAX_INCLUSIVE else PriceMode.TAX_EXCLUSIVE
                if (mode != priceMode) { priceMode = mode; recalculate() }
            }
            .launchIn(viewModelScope)
        // Default overall-discount mode is a workspace setting too (FR-B10).
        storeSettings.observeString("common", "overall_discount_mode", default = null)
            .onEach { value ->
                val mode = when (value) {
                    "PRE_TAX_APPORTIONED" -> OverallDiscountMode.PRE_TAX_APPORTIONED
                    "POST_TAX_REDUCTION" -> OverallDiscountMode.POST_TAX_REDUCTION
                    else -> null
                }
                if (mode != null && mode != overallDiscountMode) { overallDiscountMode = mode; recalculate() }
            }
            .launchIn(viewModelScope)
        // Sync chip — only meaningful after this editor saved at least once.
        syncService.observeEntity(SyncEntity.ORDER)
            .onEach { state ->
                if (!savedOnce) return@onEach
                syncUi = when (state?.status) {
                    is SyncStatus.PendingPush, is SyncStatus.PendingPull -> DocSyncUi.OFFLINE
                    is SyncStatus.Syncing -> DocSyncUi.SYNCING
                    is SyncStatus.Success -> DocSyncUi.SYNCED
                    is SyncStatus.Failed -> DocSyncUi.FAILED
                    else -> syncUi
                }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    private fun formatDocDate(): String {
        val date = order.orderDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "${date.day.toString().padStart(2, '0')} $month ${date.year}"
    }
}
