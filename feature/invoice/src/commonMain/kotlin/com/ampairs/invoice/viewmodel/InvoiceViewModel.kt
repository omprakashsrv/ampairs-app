package com.ampairs.invoice.viewmodel

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
import com.ampairs.invoice.db.InvoiceRepository
import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.TaxInfo
import com.ampairs.invoice.domain.TaxSpec
import com.ampairs.invoice.domain.asDatabaseModel
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
 * ViewModel for the v2 "fast entry" invoice editor (spec 010) — the mirror of
 * [com.ampairs.order.viewmodel.OrderViewModel] plus client-side invoice numbering: the header
 * previews the next number in the active series and the repository assigns it at save (C4/C5).
 */
@AssistedInject
class InvoiceViewModel(
    @Assisted customerId: String?,
    @Assisted id: String?,
    val customerDataService: CustomerDataService,
    val invoiceRepository: InvoiceRepository,
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
        fun create(customerId: String?, id: String?): InvoiceViewModel
    }

    // ───────────────────────── core document state ─────────────────────────

    var customer: Customer? = null
    val invoiceItems = mutableStateListOf<InvoiceItem>()
    var savingInvoice by mutableStateOf(false)
    var invoice = Invoice()

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

    var lineUis by mutableStateOf<List<DocLineUi>>(emptyList())
        private set
    var customerUi by mutableStateOf<DocCustomerUi?>(null)
        private set
    var dateLabel by mutableStateOf("")
        private set
    var syncUi by mutableStateOf(DocSyncUi.NONE)
        private set

    /** "Number on save" preview, e.g. `MH27/26-27/0043` (null once the invoice is numbered). */
    var numberPreview by mutableStateOf<String?>(null)
        private set
    private var series: String = ""

    // Supply-origin (seller) state is owned by the tax module, not stored on the invoice. Until the
    // tax module supplies it, the editor treats the scenario as intra-state by default; the buyer's
    // GSTIN (place of supply) still drives ScenarioResolver.
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

    private suspend fun commitPreview(preview: EntryPreview) {
        val mergeable = invoiceItems.find { item ->
            item.product?.id == preview.product.id &&
                item.unitId == preview.unit.unitId &&
                !item.priceOverridden && !preview.priceOverridden &&
                item.discount.isEmpty() && preview.discountPercent == 0.0
        }
        if (mergeable != null) {
            mergeable.quantity = EntryMatcher.clampToDecimals(mergeable.quantity + preview.quantity, preview.unit.decimalPlaces)
        } else {
            val item = InvoiceItem(preview.product)
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
            invoiceItems.add(item)
        }
        recents.remove(preview.product.id)
        recents.addFirst(preview.product.id)
        while (recents.size > 5) recents.removeLast()

        invoice.items = invoiceItems
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
        val item = invoiceItems.find { it.id == lineId } ?: return
        item.quantity = quantity.coerceAtLeast(0.0)
        invoice.updateTotalCost()
        recalculate()
    }

    fun setLineUnitPrice(lineId: String, price: Double) {
        val item = invoiceItems.find { it.id == lineId } ?: return
        item.price = price.coerceAtLeast(0.0)
        item.priceOverridden = true
        item.updateTotal()
        invoice.updateTotalCost()
        recalculate()
    }

    fun setLineDiscount(lineId: String, kind: DiscountKind, amount: Double) {
        val item = invoiceItems.find { it.id == lineId } ?: return
        item.discount.clear()
        if (amount > 0.0) {
            when (kind) {
                DiscountKind.PERCENT -> { item.discountPercent = amount; item.discount.add(Discount(amount, 0.0)) }
                DiscountKind.FLAT -> { item.discountPercent = 0.0; item.discount.add(Discount(0.0, amount)) }
            }
        } else {
            item.discountPercent = 0.0
        }
        lineDiscountKinds[lineId] = kind
        recalculate()
    }

    fun removeLine(lineId: String) {
        invoiceItems.removeAll { it.id == lineId }
        lineDiscountKinds.remove(lineId)
        invoice.items = invoiceItems
        recalculate()
    }

    private val lineDiscountKinds = mutableMapOf<String, DiscountKind>()

    // ───────────────────────── unit / variant / product pickers ─────────────────────────

    var unitChoices by mutableStateOf<List<DocUnitChoiceUi>>(emptyList())
        private set
    var variantChoices by mutableStateOf<List<DocVariantChoiceUi>>(emptyList())
        private set

    fun loadUnitChoicesFor(lineId: String) {
        val item = invoiceItems.find { it.id == lineId } ?: return
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
        val item = invoiceItems.find { it.id == lineId } ?: return
        val productId = item.product?.id ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val basePrice = item.product?.sellingPrice ?: 0.0
            variantChoices = productDataService.variantsForProduct(productId).map { v ->
                DocVariantChoiceUi(sku = v.sku, label = v.label, price = v.sellingPrice ?: basePrice)
            }
        }
    }

    fun selectUnitFor(lineId: String, unitId: String) {
        val item = invoiceItems.find { it.id == lineId } ?: return
        val product = item.product ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val unit = engine.unitsFor(product).find { it.unitId == unitId } ?: return@launch
            item.selectUnit(unit.unitId, unit.name, unit.multiplier)
            item.quantity = EntryMatcher.clampToDecimals(item.quantity, unit.decimalPlaces)
            invoice.updateTotalCost()
            computeTotals()
        }
    }

    fun selectVariantFor(lineId: String, sku: String) {
        val item = invoiceItems.find { it.id == lineId } ?: return
        val productId = item.product?.id ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val variant = productDataService.variantsForProduct(productId).find { it.sku == sku } ?: return@launch
            item.selectVariant(variant.sku, variant.sellingPrice)
            invoice.updateTotalCost()
            computeTotals()
        }
    }

    fun changeLineProduct(lineId: String, productId: String) {
        val item = invoiceItems.find { it.id == lineId } ?: return
        viewModelScope.launch(DispatcherProvider.io) {
            val product = productDataService.getById(productId) ?: return@launch
            // Resolve the line price through the PriceResolver seam (spec 009) with the invoice's
            // customer + channel context. Walk-in => RETAIL, named customer => WHOLESALE. Falls back
            // to product.sellingPrice when no price list matches.
            val invoiceCustomer = invoice.customer
            val resolved = priceResolver.resolve(
                PriceResolutionInput(
                    productId = product.id,
                    variantSku = null,
                    quantity = item.quantity,
                    fallbackUnitPrice = product.sellingPrice,
                    channel = if (customerWalkIn || invoiceCustomer == null) "RETAIL" else "WHOLESALE",
                    customerId = invoiceCustomer?.uid,
                    customerGroupId = invoiceCustomer?.customerGroup,
                    customerType = invoiceCustomer?.customerType,
                    pincode = invoiceCustomer?.pincode,
                    // Resolve the price as of the invoice's document date (effective-dated pricing).
                    asOfDate = invoice.invoiceDate.toString(),
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
            invoice.updateTotalCost()
            computeTotals()
        }
    }

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

    fun createProductInline(name: String, price: Double, taxCode: String, baseUnitId: String?) {
        if (name.isBlank()) return
        viewModelScope.launch(DispatcherProvider.io) {
            val uid = UidGenerator.generateUid(Constants.PRODUCT_PREFIX)
            val summary = productDataService.quickCreate(
                id = uid, name = name, code = "",
                sellingPrice = price, mrp = price, taxCode = taxCode, baseUnitId = baseUnitId,
            ) ?: return@launch
            engine.invalidate()
            val item = InvoiceItem(summary)
            item.quantity = 1.0
            val base = engine.unitsFor(summary).firstOrNull()
            if (base != null) item.selectUnit(base.unitId, base.name, base.multiplier)
            invoiceItems.add(item)
            recents.addFirst(summary.id)
            while (recents.size > 5) recents.removeLast()
            invoice.items = invoiceItems
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
            invoice.customer = selected
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
        invoice.customer = walkIn
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
        val supplierState = invoice.sellerPlaceOfSupply?.toIntOrNull()
            ?: ScenarioResolver.stateCodeFromGstin(invoice.sellerGst)
        val posState = invoice.placeOfSupply?.toIntOrNull()
            ?: ScenarioResolver.stateCodeFromGstin(invoice.customer?.gstNumber)
        val scenario = ScenarioResolver.resolve(supplierState, posState)
        val taxCodes = invoiceItems.mapNotNull { it.product?.taxCode }
        val rates = taxRateProvider.resolveAll(taxCodes, scenario)
        ratePercents = rates.mapValues { (_, v) -> v.totalRate }

        val lines = invoiceItems.map { item ->
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
        invoiceItems.forEach { item ->
            val line = byId[item.id] ?: return@forEach
            item.basePrice = line.taxable
            item.totalTax = line.totalTax
            item.totalCost = line.lineTotal
            item.taxInfos = line.components.map { c ->
                TaxInfo(name = c.name, percentage = c.percentage, taxSpec = spec, value = c.amount)
            }
            val disc = item.discount.firstOrNull()
            if (disc != null && line.lineDiscountValue > 0.0) {
                item.discount[0] = Discount(disc.percent, line.lineDiscountValue)
            }
        }
        invoice.taxSpec = spec
        invoice.basePrice = result.taxableSubtotal
        invoice.totalTax = result.totalTax
        invoice.taxInfos = result.taxComponents.map { c ->
            TaxInfo(name = c.name, percentage = 0.0, taxSpec = spec, value = c.amount)
        }.toMutableList()
        invoice.totalItems = invoiceItems.size
        invoice.totalQuantity = invoiceItems.sumOf { it.quantity }
        invoice.totalCost = result.grandTotal
        invoice.discount = overall?.let {
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
            itemCount = invoiceItems.size,
        )
        publishLineUis()
        publishCustomerUi(scenario)
    }

    private suspend fun publishLineUis() {
        lineUis = invoiceItems.map { item ->
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

    // ───────────────────────── numbering / save / sync ─────────────────────────

    private suspend fun refreshNumberPreview() {
        if (!invoice.invoiceNumber.isNullOrBlank()) {
            numberPreview = invoice.invoiceNumber
            return
        }
        val prefix = storeSettings.getString("invoice", "series_prefix", default = null)?.ifBlank { null }
        val fy = storeSettings.getString("invoice", "financial_year", default = null)?.ifBlank { null }
        series = when {
            prefix != null && fy != null -> "$prefix/$fy"
            prefix != null -> prefix
            else -> ""
        }
        numberPreview = invoiceRepository.nextNumberPreview(series)
    }

    fun saveInvoice(onInvoiceSaved: (String) -> Unit) {
        savingInvoice = true
        viewModelScope.launch(DispatcherProvider.io) {
            computeTotals()
            val userId = tokenRepository.getCurrentUserId() ?: ""
            if (invoice.createdBy.isEmpty()) {
                invoice.createdBy = userId
            }
            invoice.updatedBy = userId
            // Default place of supply to the buyer's GSTIN state, else the buyer's address state.
            if (invoice.placeOfSupply.isNullOrBlank()) {
                invoice.placeOfSupply = ScenarioResolver.stateCodeFromGstin(invoice.customer?.gstNumber)?.toString()
                    ?: invoice.customer?.state
            }
            // Seller (origin) place of supply: from the snapshotted seller GSTIN (stamped from the
            // tax registration). Stored so the IGST decision is a pure compare on re-edit.
            if (invoice.sellerPlaceOfSupply.isNullOrBlank()) {
                invoice.sellerPlaceOfSupply = ScenarioResolver.stateCodeFromGstin(invoice.sellerGst)?.toString()
            }
            // snapshot the active modes + numbering series onto the document (spec 010 C1/C2/C4)
            val invoiceEntity = invoice.asDatabaseModel().copy(
                price_mode = priceMode.name,
                overall_discount_mode = overallDiscountMode.name,
                series = series.ifBlank { "INV" },
            )
            invoiceRepository.saveInvoice(
                invoiceEntity,
                invoiceItems.asDatabaseModel(invoiceEntity.id)
            )
            savedOnce = true
            syncUi = DocSyncUi.OFFLINE
            onInvoiceSaved(invoiceEntity.id)
            savingInvoice = false
        }
    }

    fun retrySync() {
        syncService.emit(SyncEvent.TriggerPush(SyncEntity.INVOICE))
    }

    // ───────────────────────── legacy API (kept for other call sites) ─────────────────────────

    fun updateInvoiceItems(products: List<ProductSummary>) {
        invoiceItems.removeAll(invoiceItems.filter { invoiceItem ->
            !products.map { it.id }.contains(invoiceItem.product?.id)
        })
        products.forEach { product ->
            val item = invoiceItems.find { invoiceItem -> invoiceItem.product?.id == product.id }
            if (item != null) {
                item.quantity = product.quantity
            } else {
                invoiceItems.add(InvoiceItem(product))
            }
        }
        invoiceItems.removeAll(invoiceItems.filter { invoiceItem -> invoiceItem.quantity <= 0 })
        invoice.items = invoiceItems
        recalculate()
    }

    fun addProduct(productId: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            val existing = invoiceItems.find { it.product?.id == productId }
            if (existing != null) {
                existing.quantity = existing.quantity + 1.0
            } else {
                val product = productDataService.getById(productId) ?: return@launch
                val item = InvoiceItem(product)
                if (item.quantity <= 0.0) item.quantity = 1.0
                invoiceItems.add(item)
            }
            invoice.items = invoiceItems
            computeTotals()
        }
    }

    var unitOptions by mutableStateOf<List<UnitOption>>(emptyList())
        private set
    var selectedInvoiceItem by mutableStateOf<InvoiceItem?>(null)

    fun loadUnitOptions(item: InvoiceItem) {
        viewModelScope.launch(DispatcherProvider.io) {
            val productId = item.product?.id ?: item.productId
            if (productId.isNullOrBlank()) { unitOptions = emptyList(); return@launch }
            val baseUnitId = item.product?.baseUnitId ?: item.unitId.takeIf { it.isNotBlank() }
            unitOptions = unitOptionsLookup.unitsForProduct(productId, baseUnitId)
        }
    }

    fun selectUnit(item: InvoiceItem, option: UnitOption) {
        item.selectUnit(option.unitId, option.shortName, option.multiplier)
        invoice.updateTotalCost()
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
            val item = InvoiceItem(summary)
            if (item.quantity <= 0.0) item.quantity = 1.0
            invoiceItems.add(item)
            invoice.items = invoiceItems
            computeTotals()
        }
    }

    // ───────────────────────── init ─────────────────────────

    init {
        viewModelScope.launch(DispatcherProvider.io) {
            showDiscount = storeSettings.getBoolean("invoice", "show_discount_options", default = true)
            if (!id.isNullOrEmpty()) {
                invoice = invoiceRepository.getInvoice(id)
                customer = invoice.customer
                invoiceItems.addAll(invoice.items)
                // unitName is transient (display-only, not persisted) — restore it from the unit
                // catalog so re-opened lines show their unit even when the catalog product isn't
                // attached (publishLineUis otherwise resolves the name only via the product's units).
                invoiceItems.forEach { item ->
                    if (item.unitName.isBlank() && item.unitId.isNotBlank()) {
                        unitLookup.getUnitById(item.unitId)?.let { u -> item.unitName = u.shortName.ifBlank { u.name } }
                    }
                }
            } else {
                customer = customerId?.let { customerDataService.getById(it) }
                invoice.customer = customer
            }
            customerName = customer?.name ?: ""
            dateLabel = formatDocDate()
            baseUnits = unitLookup.getActiveUnits().map { DocBaseUnitChoice(it.uid, it.shortName.ifBlank { it.name }) }
            refreshNumberPreview()
            computeTotals()
        }
        // Workspace price mode is observed (spec 010 v2): changing the setting recomputes open drafts.
        storeSettings.observeBoolean("common", "prices_include_tax", default = false)
            .onEach { inclusive ->
                val mode = if (inclusive) PriceMode.TAX_INCLUSIVE else PriceMode.TAX_EXCLUSIVE
                if (mode != priceMode) { priceMode = mode; recalculate() }
            }
            .launchIn(viewModelScope)
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
        // Series settings can change while the editor is open — keep the number preview live.
        storeSettings.observeString("invoice", "series_prefix", default = null)
            .onEach { viewModelScope.launch(DispatcherProvider.io) { refreshNumberPreview() } }
            .launchIn(viewModelScope)
        syncService.observeEntity(SyncEntity.INVOICE)
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
        val date = invoice.invoiceDate.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val month = date.month.name.take(3).lowercase().replaceFirstChar { it.uppercase() }
        return "${date.day.toString().padStart(2, '0')} $month ${date.year}"
    }
}
