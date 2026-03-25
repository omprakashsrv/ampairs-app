package com.ampairs.invoice.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ampairs.common.id_generator.IdUtils
import com.ampairs.invoice.db.entity.InvoiceItemEntity
import com.ampairs.product.domain.Product
import kotlinx.serialization.json.Json

const val INVOICE_ITEM_PREFIX = "IIT"

class InvoiceItem(var product: Product?) {
    var quantity: Double = product?.quantity ?: 0.0
        set(value) {
            field = value
            product?.quantity = value
            updateTotal()
        }

    fun updateTotal() {
        totalCost = quantity * price
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
            description = invoiceItem.product?.name + " " + invoiceItem.product?.code,
            item_no = 0,
            product_id = invoiceItem.product?.id ?: "",
            total_cost = invoiceItem.totalCost,
            base_price = invoiceItem.basePrice,
            product_price = invoiceItem.productPrice,
            quantity = invoiceItem.quantity,
            selling_price = invoiceItem.price,
            mrp = invoiceItem.mrp,
            dp = invoiceItem.dp,
            invoice_id = invoiceId,
            tax_code = invoiceItem.product?.taxCode ?: "",
            tax_info = Json.encodeToString(invoiceItem.taxInfos.toDatabaseEntity()),
            total_tax = invoiceItem.totalTax,
            active = if (invoiceItem.active) 1 else 0,
            soft_deleted = if (invoiceItem.softDeleted) 1 else 0,
            discount = if (invoiceItem.discount.size > 0) Json.encodeToString(invoiceItem.discount) else null
        )
    }
}

