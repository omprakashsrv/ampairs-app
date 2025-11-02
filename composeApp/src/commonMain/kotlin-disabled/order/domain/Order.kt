package com.ampairs.order.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.workspace.domain.Workspace
import com.ampairs.customer.domain.Customer
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.model.OrderModel
import com.ampairs.order.db.model.TaxInfoEntity
import com.ampairs.order.db.model.toDomainModel
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val ORDER_PREFIX = "ORD"

@OptIn(ExperimentalTime::class)
class Order {
    var orderDate: Instant = Clock.System.now()
    var orderNumber: String? = null
    var id: String = ""
    var fromCustomer: Customer? = null
    var toCustomer: Customer? = null
        set(value) {
            field = value
            taxSpec =
                if (toCustomer?.state !== fromCustomer?.state) TaxSpec.INTRA else TaxSpec.INTER
        }
    var totalCost: Double by mutableStateOf(0.0)
    var basePrice: Double = 0.0
    var totalTax: Double = 0.0
    var active: Boolean = true
    var softDeleted: Boolean = true
    var taxSpec: TaxSpec = TaxSpec.INTER
    var status: OrderStatus = OrderStatus.NEW
    var taxInfos: MutableList<TaxInfo>? = mutableListOf()
    var createdBy = ""
    var updatedBy = ""
    var invoiceRefId: String? = null
    var discount: MutableList<Discount>? = null
    var items: MutableList<OrderItem> = mutableListOf()
        set(value) {
            field = value
            updateTotalCost()
        }
    var totalItems: Int = 0
    var totalQuantity: Double = 0.0

    fun updateTotalCost() {
        totalCost = items.sumOf { item -> item.totalCost }
        totalQuantity = items.sumOf { item -> item.quantity }
        totalItems = items.size
    }

    fun updateTaxes() {
        basePrice = 0.0
        totalTax = 0.0
        taxInfos = mutableListOf()
        items.forEach { orderItem ->
            orderItem.updateTaxes(taxSpec)
            basePrice += orderItem.basePrice
            totalTax += orderItem.totalTax
            orderItem.taxInfos.forEach { itemTaxInfo ->
                val taxInfo = taxInfos?.find { orderTaxInfo ->
                    itemTaxInfo.name.lowercase() == orderTaxInfo.name.lowercase() && itemTaxInfo.percentage == orderTaxInfo.percentage
                }
                if (taxInfo != null) {
                    taxInfo.value = (taxInfo.value ?: 0.0) + (itemTaxInfo.value ?: 0.0)
                } else {
                    if (taxInfos == null) {
                        taxInfos = mutableListOf()
                    }
                    taxInfos?.add(
                        TaxInfo(
                            id = itemTaxInfo.id,
                            name = itemTaxInfo.name,
                            percentage = itemTaxInfo.percentage,
                            formattedName = itemTaxInfo.formattedName,
                            taxSpec = itemTaxInfo.taxSpec,
                            value = itemTaxInfo.value
                        )
                    )
                }
            }
        }
    }

    fun updateDiscount() {
        val orderDiscount = Discount(0.0, 0.0)
        items.forEach { orderItem ->
            orderItem.discount.forEach { discount ->
                orderDiscount.percent += discount.percent
                orderDiscount.value += discount.value
            }

        }
        if (orderDiscount.value > 0) {
            this.discount = mutableListOf(orderDiscount)
        }
    }

    init {
        if (id == "") {
            id = IdUtils.generateUniqueId(ORDER_PREFIX, 64)
        }
    }
}

@OptIn(ExperimentalTime::class)
fun Order.asDatabaseModel(): OrderEntity {
    return OrderEntity(
        seq_id = 0,
        id = this.id,
        order_number = this.orderNumber ?: "",
        order_date = DateTimeAdapter.toDateTimeString(this.orderDate),
        from_customer_id = this.fromCustomer?.id ?: "",
        to_customer_id = this.toCustomer?.id ?: "",
        invoice_ref_id = this.invoiceRefId,
        total_cost = this.totalCost,
        base_price = this.basePrice,
        total_items = this.totalItems.toLong(),
        total_quantity = this.totalQuantity,
        status = this.status.name,
        from_customer_name = this.fromCustomer?.name ?: "",
        to_customer_name = this.toCustomer?.name ?: "",
        from_customer_gst = this.fromCustomer?.gstin ?: "",
        to_customer_gst = this.toCustomer?.gstin ?: "",
        billing_address = "",
        shipping_address = "",
        tax_info = if (this.taxInfos != null) Json.encodeToString(this.taxInfos?.toDatabaseEntity()) else null,
        total_tax = totalTax,
        active = if (this.active) 1 else 0,
        soft_deleted = if (this.softDeleted) 1 else 0,
        synced = 0,
        last_updated = 0,
        created_by = this.createdBy,
        updated_by = this.updatedBy,
        discount = this.discount?.let { Json.encodeToString(it) })
}


// TODO: Room-based order with items mapping
@OptIn(ExperimentalTime::class)
fun OrderEntity.asDomainModel(): Order {
    val order = Order()
    order.id = this.id
    order.orderNumber = this.order_number
    order.orderDate = DateTimeAdapter.fromDateTimeString(this.order_date) ?: Clock.System.now()
    order.status = OrderStatus.valueOf(this.status.uppercase())
    order.basePrice = this.base_price
    order.totalTax = this.total_tax
    order.items = mutableListOf()
    order.fromCustomer = Customer(
        id = this.from_customer_id,
        name = this.from_customer_name,
        gstin = this.from_customer_gst
    )
    order.toCustomer = Customer(
        id = this.to_customer_id,
        name = this.to_customer_name,
        gstin = this.to_customer_gst
    )
    order.totalCost = this.total_cost
    order.totalItems = this.total_items.toInt()
    order.totalQuantity = this.total_quantity
    order.createdBy = this.created_by
    order.updatedBy = this.updated_by
    order.discount = this.discount?.let { Json.decodeFromString<MutableList<Discount>>(it) }
    val taxInfos = this.tax_info?.let { Json.decodeFromString<List<TaxInfoEntity>>(it) }
    order.taxInfos = taxInfos?.toDomainModel()?.toMutableList()
    return order
}

@OptIn(ExperimentalTime::class)
fun OrderModel.asDomainModel(): Order {
    val order = Order()
    order.id = this.order.id
    order.orderNumber = this.order.order_number
    order.orderDate =
        DateTimeAdapter.fromDateTimeString(this.order.order_date) ?: Clock.System.now()
    order.status = OrderStatus.valueOf(this.order.status.uppercase())
    order.basePrice = this.order.base_price
    order.totalTax = this.order.total_tax
    order.items = this.orderItems.asItemsDomainModel().toMutableList()
    order.fromCustomer = Customer(
        id = this.order.from_customer_id,
        name = this.order.from_customer_name,
        gstin = this.order.from_customer_gst
    )
    order.toCustomer = Customer(
        id = this.order.to_customer_id,
        name = this.order.to_customer_name,
        gstin = this.order.to_customer_gst
    )
    order.totalCost = this.order.total_cost
    order.totalItems = this.order.total_items.toInt()
    order.totalQuantity = this.order.total_quantity
    order.createdBy = this.order.created_by
    order.updatedBy = this.order.updated_by
    order.discount = this.order.discount?.let { Json.decodeFromString<MutableList<Discount>>(it) }
    val taxInfos = this.order.tax_info?.let { Json.decodeFromString<List<TaxInfoEntity>>(it) }
    order.taxInfos = taxInfos?.toDomainModel()?.toMutableList()
    return order
}


fun Order.toWhatsAppMsg(workspace: Workspace): String {
    val msg = StringBuilder()
    msg.append("*").append(workspace.name).append("*").append("\n")
    msg.append("To : " + fromCustomer?.name)
    if (!fromCustomer?.address.isNullOrEmpty()) {
        msg.append("Address : " + fromCustomer?.address).append("\n")
    }
    if (!fromCustomer?.phone.isNullOrEmpty()) {
        msg.append("Phone : " + fromCustomer?.phone).append("\n")
    }
    msg.append("\n")
    msg.append("*Particulars*").append("\n").append("*Price*   ").append("*Qty*   ")
        .append("*Total*").append("\n")
    var totalQty = 0.0
    items.forEach {
        msg.append("   *").append(it.totalCost).append("*\n")
        totalQty += it.quantity
    }
    msg.append("\n")
    msg.append("*Total Items : ").append(items.size).append("*\n")
    msg.append("*Total Qty : ").append(totalQty).append("*\n")
    msg.append("*Total : ").append(totalCost).append("*\n")
    return msg.toString()
}