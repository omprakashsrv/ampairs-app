package com.ampairs.supplier.data.api

import com.ampairs.supplier.domain.Supplier
import com.ampairs.common.model.PageResponse

interface SupplierApi {
    suspend fun getSuppliers(
        lastSync: String = "",
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC"
    ): PageResponse<Supplier>

    suspend fun bulkUpdateSuppliers(suppliers: List<Supplier>): List<Supplier>
}
