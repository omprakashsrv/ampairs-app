package com.ampairs.order.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.order.db.model.TaxInfoEntity
import com.ampairs.order.db.model.toDomainModel
import com.ampairs.product.domain.ProductSummary
import kotlinx.serialization.json.Json

const val ORDER_ITEM_PREFIX = "OIT"

class OrderItem(var product: ProductSummary?) {
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

    var mrp: Double = product?.mrp ?: 0.0
    var totalCost: Double by mutableStateOf(0.0)
    var basePrice: Double = 0.0
    var description: String = (product?.name + " " + product?.code)
    var productId = product?.id
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

    init {
        if (id == "") {
            id = IdUtils.generateUniqueId(ORDER_ITEM_PREFIX, 64)
        }
        updateTotal()
    }
}

fun List<OrderItem>.asDatabaseModel(orderId: String): List<OrderItemEntity> {
    return map { orderItem ->
        OrderItemEntity(
            seq_id = 0,
            id = orderItem.id,
            description = orderItem.product?.name + " " + orderItem.product?.code,
            item_no = 0,
            product_id = orderItem.product?.id ?: "",
            total_cost = orderItem.totalCost,
            base_price = orderItem.basePrice,
            product_price = orderItem.productPrice,
            quantity = orderItem.quantity,
            selling_price = orderItem.price,
            mrp = orderItem.mrp,
            dp = orderItem.dp,
            order_id = orderId,
            tax_code = orderItem.product?.taxCode ?: "",
            tax_info = Json.encodeToString(orderItem.taxInfos.toDatabaseEntity()),
            total_tax = orderItem.totalTax,
            active = if (orderItem.active) 1 else 0,
            soft_deleted = if (orderItem.softDeleted) 1 else 0,
            discount = if (orderItem.discount.isNotEmpty()) Json.encodeToString(orderItem.discount) else null,
            unit_id = orderItem.unitId,
            base_quantity = orderItem.baseQuantity,
            variant_sku = orderItem.variantSku
        )
    }
}

fun List<OrderItemEntity>.asItemsDomainModel(): List<OrderItem> {
    return map { orderItem ->
        val orderItem1 = OrderItem(null)
        orderItem1.id = orderItem.id
        orderItem1.productId = orderItem.product_id
        orderItem1.description = orderItem.description
        orderItem1.quantity = orderItem.quantity
        orderItem1.price = orderItem.selling_price
        orderItem1.mrp = orderItem.mrp
        orderItem1.totalCost = orderItem.total_cost
        orderItem1.basePrice = orderItem.base_price
        orderItem1.productPrice = orderItem.product_price
        orderItem1.totalTax = orderItem.total_tax
        orderItem1.active = orderItem.active == 1
        orderItem1.softDeleted = orderItem.soft_deleted == 1
        // tax_info is persisted as List<TaxInfoEntity> (domain TaxInfo is not @Serializable);
        // both blobs are nullable — never decode a null/blank string.
        orderItem1.taxInfos = orderItem.tax_info?.takeIf { it.isNotBlank() }
            ?.let { Json.decodeFromString<List<TaxInfoEntity>>(it).toDomainModel() }
            ?: emptyList()
        orderItem.discount?.takeIf { it.isNotBlank() }
            ?.let { Json.decodeFromString<List<Discount>>(it) }
            ?.let { orderItem1.discount.addAll(it) }
        orderItem1.discountPercent = orderItem1.discount.firstOrNull()?.percent ?: 0.0
        orderItem1.unitId = orderItem.unit_id
        orderItem1.baseQuantity = orderItem.base_quantity
        orderItem1.unitMultiplier =
            if (orderItem.quantity > 0.0 && orderItem.base_quantity > 0.0) orderItem.base_quantity / orderItem.quantity else 1.0
        orderItem1.variantSku = orderItem.variant_sku
        // derive the override flag: price differs from the unit-scaled catalog price
        orderItem1.priceOverridden =
            kotlin.math.abs(orderItem.selling_price - orderItem.product_price * orderItem1.unitMultiplier) > 0.005
        orderItem1
    }
}

