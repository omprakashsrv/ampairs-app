package com.ampairs.cbemployee.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CbEmployeeApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : CbEmployeeApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getEmployeesSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<Employee> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        ).also { if (lastSync.isNotBlank()) it["last_sync"] = lastSync }

        val response: Response<PageResponse<Employee>> =
            get(client, ApiUrlBuilder.cbEmployeeUrl("v1/employees/sync"), params)
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
            last = true,
        )
    }

    override suspend fun bulkUpdateEmployees(employees: List<Employee>): List<Employee> {
        val response: Response<List<Employee>> =
            post(client, ApiUrlBuilder.cbEmployeeUrl("v1/employees/sync"), employees)
        return response.data ?: throw Exception("Failed to bulk update employees")
    }

    override suspend fun getEmployeeById(id: String): Response<Employee> =
        get(client, ApiUrlBuilder.cbEmployeeUrl("v1/employees/$id"))
}
