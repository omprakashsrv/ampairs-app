package com.ampairs.invoice.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.product.domain.ProductSummary
import kotlinx.serialization.json.Json

const val INVOICE_ITEM_PREFIX = "IIT"

class InvoiceItem(var product: ProductSummary?) {
    var quantity: Double = product?.quantity ?: 0.0
        set(value) {
            field = value
            if (product != null) product!!.quantity = value
            baseQuantity = value * unitMultiplier
            updateTotal()
        }

    // spec 010 FR-014: unit of measure + base-unit quantity, and selected variant
    var unitId: String = product?.baseUnitId ?: ""
    var unitName: String by mutableStateOf("")          // transient display (short name)
    var unitMultiplier: Double = 1.0                    // 1 selected unit = multiplier base units
    var baseQuantity: Double by mutableStateOf(product?.quantity ?: 0.0)
    var variantSku: String? = null

    /** True once the user manually edited the unit price (spec 010 FR-016/C3). */
    var priceOverridden: Boolean = false

    /** Switch the line's unit of measure, rescaling the per-unit price and base quantity. */
    fun selectUnit(unitId: String, name: String, multiplier: Double) {
        this.unitId = unitId
        this.unitName = name
        this.unitMultiplier = if (multiplier > 0.0) multiplier else 1.0
        if (!priceOverridden) this.price = productPrice * this.unitMultiplier
        this.baseQuantity = quantity * this.unitMultiplier
        updateTotal()
    }

    /** Apply a variant: re-bases the per-unit price on the variant price unless overridden. */
    fun selectVariant(sku: String?, variantPrice: Double?) {
        this.variantSku = sku
        this.productPrice = variantPrice ?: product?.sellingPrice ?: productPrice
        if (!priceOverridden) this.price = productPrice * unitMultiplier
        updateTotal()
    }

    fun updateTotal() {
        totalCost = quantity * price
        baseQuantity = quantity * unitMultiplier
    }

    fun updateTaxes(taxSpec: TaxSpec) {
        this.taxSpec = taxSpec
        val taxPercent = this.getTaxPercent() / 100
        basePrice = totalCost / (1 + taxPercent)
        this.taxInfos.forEach {
            it.value = basePrice * it.percentage
        }
        totalTax = totalCost - basePrice
    }

    private fun getTaxPercent(): Double {
        return this.taxInfos.map { it.percentage }.sum()
    }

    var price: Double by mutableStateOf(product?.sellingPrice ?: 0.0)
    var description: String = (product?.name + " " + product?.code)
    var productId = product?.id

    /** HSN snapshot — kept on the line so it survives without an attached catalog product. */
    var taxCode: String = product?.taxCode ?: ""
    var mrp: Double = product?.mrp ?: 0.0
    var totalCost: Double by mutableStateOf(0.0)
    var basePrice: Double = 0.0
    var productPrice: Double = product?.sellingPrice ?: 0.0
    var dp: Double = product?.dp ?: 0.0
    var totalTax: Double = 0.0
    var active: Boolean = true
    var softDeleted: Boolean = false
    var taxSpec: TaxSpec = TaxSpec.INTER
    var taxInfos: List<TaxInfo> = arrayListOf()
    var discount = mutableStateListOf<Discount>()

    var id: String = ""
    var discountPercent: Double by mutableStateOf(0.0)

    // 009 pricing snapshot — set by the PriceResolver seam at line build; persisted to Room and
    // pushed verbatim on /sync (the backend never re-resolves). Null when no resolution ran yet.
    var resolvedUnitPriceMinor: Long? = null
    var currency: String? = null
    var priceSource: String? = null
    var matchedPriceListUid: String? = null
    var appliedTierMinQty: Double? = null
    var belowMoq: Boolean = false

    init {
        if (id == "") {
            id = IdUtils.generateUniqueId(INVOICE_ITEM_PREFIX, 64)
        }
        updateTotal()
    }
}

fun List<InvoiceItem>.asDatabaseModel(invoiceId: String): List<InvoiceItemEntity> {
    return map { invoiceItem ->
        InvoiceItemEntity(
            seq_id = 0,
            id = invoiceItem.id,
            // Derive from the catalog product when it's attached, else keep the line's own snapshot.
            // An untouched line (re-opened invoice whose product isn't in the local catalog) has a null
            // product; re-deriving from it would wipe the name to "null null" and blank the product id.
            description = invoiceItem.product?.let { "${it.name} ${it.code}" } ?: invoiceItem.description,
            item_no = 0,
            product_id = invoiceItem.product?.id ?: invoiceItem.productId ?: "",
            total_cost = invoiceItem.totalCost,
            base_price = invoiceItem.basePrice,
            product_price = invoiceItem.productPrice,
            quantity = invoiceItem.quantity,
            selling_price = invoiceItem.price,
            mrp = invoiceItem.mrp,
            dp = invoiceItem.dp,
            invoice_id = invoiceId,
            tax_code = invoiceItem.product?.taxCode ?: invoiceItem.taxCode,
            tax_info = Json.encodeToString(invoiceItem.taxInfos.toDatabaseEntity()),
            total_tax = invoiceItem.totalTax,
            active = if (invoiceItem.active) 1 else 0,
            soft_deleted = if (invoiceItem.softDeleted) 1 else 0,
            discount = if (invoiceItem.discount.size > 0) Json.encodeToString(invoiceItem.discount) else null,
            unit_id = invoiceItem.unitId,
            base_quantity = invoiceItem.baseQuantity,
            variant_sku = invoiceItem.variantSku,
            resolved_unit_price_minor = invoiceItem.resolvedUnitPriceMinor,
            currency = invoiceItem.currency,
            price_source = invoiceItem.priceSource,
            matched_price_list_uid = invoiceItem.matchedPriceListUid,
            applied_tier_min_qty = invoiceItem.appliedTierMinQty,
            below_moq = if (invoiceItem.belowMoq) 1 else 0,
        )
    }
}

