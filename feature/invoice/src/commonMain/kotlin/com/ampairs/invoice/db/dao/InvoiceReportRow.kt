package com.ampairs.invoice.db.dao

import androidx.room.ColumnInfo

/** One row of the "top customers by sales" report — projected straight out of [InvoiceDao]. */
data class InvoiceCustomerSalesRow(
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "total") val total: Double,
)

/** One row of the "top products by sales" report (joined invoice ⨝ invoice-item). */
data class InvoiceProductSalesRow(
    @ColumnInfo(name = "label") val label: String?,
    @ColumnInfo(name = "total") val total: Double,
)
