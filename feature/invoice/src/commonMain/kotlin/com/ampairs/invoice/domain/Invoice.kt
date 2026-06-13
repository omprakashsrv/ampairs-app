package com.ampairs.invoice.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.common.model.DateTimeAdapter
import com.ampairs.customer.domain.Customer
import com.ampairs.invoice.db.entity.InvoiceEntity
import com.ampairs.invoice.db.model.TaxInfoEntity
import com.ampairs.invoice.db.model.toDomainModel
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val INVOICE_PREFIX = "INV"

@OptIn(ExperimentalTime::class)
class Invoice {
    var invoiceNumber: String? = null
    var orderRefId: String? = null
    var id: String = ""
    var invoiceDate: Instant = Clock.System.now()
    // Single buyer; the seller is the implicit current workspace. taxSpec is resolved by the tax
    // module (ScenarioResolver) from the buyer's GSTIN, not derived here.
    var customer: Customer? = null
    // Seller (issuing business) identity snapshotted at issue time (frozen, self-contained).
    var sellerName: String? = null
    var sellerAddress: String? = null
    var sellerGst: String? = null
    // Place of supply (state) — defaults from the buyer; with sellerGst decides intra vs inter.
    var placeOfSupply: String? = null
    var totalCost: Double by mutableStateOf(0.0)
    var basePrice: Double = 0.0
    var totalTax: Double = 0.0
    var active: Boolean = true
    var softDeleted: Boolean = true
    var taxSpec: TaxSpec = TaxSpec.INTER
    var status: InvoiceStatus = InvoiceStatus.NEW
    var taxInfos: MutableList<TaxInfo>? = mutableListOf()
    var createdBy = ""
    var updatedBy = ""
    var discount: MutableList<Discount>? = null
    var items: MutableList<InvoiceItem> = mutableListOf()
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
        items.forEach { invoiceItem ->
            invoiceItem.updateTaxes(taxSpec)
            basePrice += invoiceItem.basePrice
            totalTax += invoiceItem.totalTax
            invoiceItem.taxInfos.forEach { itemTaxInfo ->
                val taxInfo = taxInfos?.find { invoiceTaxInfo ->
                    itemTaxInfo.name.lowercase() == invoiceTaxInfo.name.lowercase() &&
                            itemTaxInfo.percentage == invoiceTaxInfo.percentage
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
        val invoiceDiscount = Discount(0.0, 0.0)
        items.forEach { invoiceItem ->
            invoiceItem.discount.forEach { discount ->
                invoiceDiscount.percent += discount.percent
                invoiceDiscount.value += discount.value
            }

        }
        if (invoiceDiscount.value > 0) {
            this.discount = mutableListOf(invoiceDiscount)
        }
    }

    init {
        if (id == "") {
            id = IdUtils.generateUniqueId(INVOICE_PREFIX, 64)
        }
    }
}

@OptIn(ExperimentalTime::class)
fun Invoice.asDatabaseModel(): InvoiceEntity {
    return InvoiceEntity(
        seq_id = 0,
        id = this.id,
        invoice_number = this.invoiceNumber ?: "",
        invoice_date = DateTimeAdapter.toDateTimeString(this.invoiceDate),
        customer_id = this.customer?.uid ?: "",
        customer_phone = this.customer?.phone,
        seller_name = this.sellerName,
        seller_address = this.sellerAddress,
        seller_gst = this.sellerGst,
        place_of_supply = this.placeOfSupply,
        total_cost = this.totalCost,
        base_price = this.basePrice,
        total_items = this.totalItems.toLong(),
        total_quantity = this.totalQuantity,
        status = this.status.name,
        customer_name = this.customer?.name ?: "",
        customer_gst = this.customer?.gstNumber ?: "",
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
        order_ref_id = this.orderRefId,
        discount = this.discount?.let { Json.encodeToString(it) }
    )
}


// TODO: Room-based invoice with items mapping
// This function needs to be replaced with proper Room @Relation or separate queries
@OptIn(ExperimentalTime::class) 
fun InvoiceEntity.asDomainModelSimple(): Invoice {
    val invoice = Invoice()
    invoice.id = this.id
    invoice.invoiceNumber = this.invoice_number
    invoice.orderRefId = this.order_ref_id
    invoice.invoiceDate = DateTimeAdapter.fromDateTimeString(this.invoice_date) ?: Clock.System.now()
    invoice.status = InvoiceStatus.valueOf(this.status.uppercase())
    invoice.basePrice = this.base_price
    invoice.totalTax = this.total_tax
    invoice.items = mutableListOf()
    invoice.customer = Customer(
        uid = this.customer_id,
        name = this.customer_name,
        gstNumber = this.customer_gst,
        phone = this.customer_phone,
    )
    invoice.sellerName = this.seller_name
    invoice.sellerAddress = this.seller_address
    invoice.sellerGst = this.seller_gst
    invoice.placeOfSupply = this.place_of_supply
    invoice.totalCost = this.total_cost
    invoice.totalItems = this.total_items.toInt()
    invoice.totalQuantity = this.total_quantity
    invoice.createdBy = this.created_by
    invoice.updatedBy = this.updated_by
    invoice.discount = this.discount?.let { Json.decodeFromString<MutableList<Discount>>(it) }
    val taxInfos = this.tax_info?.let { Json.decodeFromString<List<TaxInfoEntity>>(it) }
    invoice.taxInfos = taxInfos?.toDomainModel()?.toMutableList()
    return invoice
}


fun Invoice.toWhatsAppMsg(workspaceName: String): String {
    val msg = StringBuilder()
    msg.append("*").append(workspaceName).append("*").append("\n")
    msg.append("To : " + customer?.name)
    if (!customer?.address.isNullOrEmpty()) {
        msg.append("Address : " + customer?.address).append("\n")
    }
    if (!customer?.phone.isNullOrEmpty()) {
        msg.append("Phone : " + customer?.phone).append("\n")
    }
    msg.append("\n")
    msg.append("*Particulars*").append("\n").append("*Price*   ").append("*Qty*   ")
        .append("*Total*")
        .append("\n")
    var totalQty = 0.0
    items.forEach {
        msg.append("   *").append(it.totalCost)
            .append("*\n")
        totalQty += it.quantity
    }
    msg.append("\n")
    msg.append("*Total Items : ").append(items.size).append("*\n")
    msg.append("*Total Qty : ").append(totalQty).append("*\n")
    msg.append("*Total : ").append(totalCost).append("*\n")
    return msg.toString()
}