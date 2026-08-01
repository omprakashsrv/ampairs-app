package com.ampairs.agent.ui

import com.ampairs.common.agent.DraftDocumentStore
import com.ampairs.common.agent.DraftLine

/**
 * UI representation of the current draft cart for display in the chat sticky section.
 */
data class DraftCartDisplay(
    val items: List<DraftLineUI> = emptyList(),
    val customerId: String? = null,
    val customerName: String? = null,
    val subtotal: Double = 0.0,
    val isEmpty: Boolean = true,
    val itemCount: Int = 0,
    val canCreateOrder: Boolean = false,
    val canCreateInvoice: Boolean = false,
) {
    companion object {
        fun from(draft: DraftDocumentStore): DraftCartDisplay {
            val canCreate = !draft.isEmpty && draft.customerId != null
            return DraftCartDisplay(
                items = draft.lines.map { DraftLineUI.from(it) },
                customerId = draft.customerId,
                customerName = draft.customerName,
                subtotal = draft.subtotal,
                isEmpty = draft.isEmpty,
                itemCount = draft.itemCount,
                canCreateOrder = canCreate,
                canCreateInvoice = canCreate,
            )
        }
    }
}

data class DraftLineUI(
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineSubtotal: Double,
) {
    companion object {
        fun from(line: DraftLine): DraftLineUI = DraftLineUI(
            productId = line.productId,
            productName = line.productName,
            quantity = line.quantity,
            unitPrice = line.unitPrice,
            lineSubtotal = line.lineSubtotal,
        )
    }
}
