package com.ampairs.invoice.api.model

import com.ampairs.invoice.domain.Discount
import com.ampairs.invoice.domain.Invoice
import com.ampairs.invoice.domain.InvoiceItem
import com.ampairs.invoice.domain.toApiModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class InvoiceItemApiModel(
    @SerialName("id") var id: String = "",
    @SerialName("description") var description: String = "",
    @SerialName("quantity") var quantity: Double = 0.0,
    @SerialName("price") var price: Double = 0.0,
    @SerialName("product_price") var productPrice: Double = 0.0,
    @SerialName("mrp") var mrp: Double = 0.0,
    @SerialName("dp") var dp: Double = 0.0,
    @SerialName("total_cost") var totalCost: Double = 0.0,
    @SerialName("total_tax") var totalTax: Double = 0.0,
    @SerialName("base_price") var basePrice: Double = 0.0,
    @SerialName("invoice_id") var invoiceId: String = "",
    @SerialName("product_id") var productId: String = "",
    @SerialName("tax_code") var taxCode: String = "",
    @SerialName("active") var active: Boolean,
    @SerialName("soft_deleted") var softDeleted: Boolean,
    @SerialName("discount") var discount: List<Discount>? = null,
    @SerialName("tax_infos") val taxInfoApiModels: List<TaxInfoApiModel> = arrayListOf(),
)

fun List<InvoiceItem>.toApiModel(invoice: Invoice): List<InvoiceItemApiModel> {
    return map {
        InvoiceItemApiModel(
            id = it.id,
            description = it.product?.name + " " + it.product?.code,
            quantity = it.quantity,
            price = it.price,
            productPrice = it.productPrice,
            mrp = it.mrp,
            dp = it.dp,
            totalCost = it.totalCost,
            totalTax = it.totalTax,
            basePrice = it.basePrice,
            invoiceId = invoice.id,
            productId = it.product?.id ?: "",
            taxCode = it.product?.taxCode ?: "",
            taxInfoApiModels = it.taxInfos.toApiModel(),
            active = it.active,
            softDeleted = it.softDeleted,
            discount = it.discount
        )
    }
}