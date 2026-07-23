package com.ampairs.supplier.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.post
import com.ampairs.common.model.Response
import com.ampairs.common.model.PageResponse
import com.ampairs.supplier.domain.Supplier
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class SupplierApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository
) : SupplierApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getSuppliers(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String
    ): PageResponse<Supplier> {
        val params = mutableMapOf(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir
        )
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        val response: Response<PageResponse<Supplier>> = get(
            client,
            ApiUrlBuilder.supplierUrl("v1/suppliers/sync"),
            params
        )
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: PageResponse(
            content = emptyList(),
            pageNumber = page,
            pageSize = size,
            totalPages = 0,
            totalElements = 0L,
            hasNext = false,
            hasPrevious = false,
            first = true,
            last = true
        )
    }

    override suspend fun bulkUpdateSuppliers(suppliers: List<Supplier>): List<Supplier> {
        val response: Response<List<Supplier>> = post(
            client,
            ApiUrlBuilder.supplierUrl("v1/suppliers/sync"),
            suppliers
        )
        return response.data ?: throw Exception("Failed to bulk update suppliers")
    }
}
