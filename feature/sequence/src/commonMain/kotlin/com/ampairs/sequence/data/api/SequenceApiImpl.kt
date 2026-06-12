package com.ampairs.sequence.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.domain.model.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.model.SequenceAllocationRequest
import com.ampairs.sequence.domain.model.SequenceDefinition
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class SequenceApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : SequenceApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getDefinitionsSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<SequenceDefinition> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) {
            params["last_sync"] = lastSync
        }

        val response: Response<PageResponse<SequenceDefinition>> = get(
            client,
            ApiUrlBuilder.sequenceUrl("v1/definitions/sync"),
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

    override suspend fun bulkUpdateDefinitions(definitions: List<SequenceDefinition>): List<SequenceDefinition> {
        val response: Response<List<SequenceDefinition>> = post(
            client,
            ApiUrlBuilder.sequenceUrl("v1/definitions/sync"),
            definitions
        )
        return response.data ?: throw Exception(
            response.error?.message ?: "Failed to bulk update sequence definitions"
        )
    }

    override suspend fun requestAllocation(request: SequenceAllocationRequest): SequenceAllocation {
        val response: Response<SequenceAllocation> = post(
            client,
            ApiUrlBuilder.sequenceUrl("v1/allocations"),
            request
        )
        return response.data ?: throw Exception(
            response.error?.message ?: "Failed to allocate sequence block"
        )
    }

    override suspend fun reportAllocations(
        reports: List<SequenceAllocationReportRequest>,
    ): List<SequenceAllocation> {
        val response: Response<List<SequenceAllocation>> = post(
            client,
            ApiUrlBuilder.sequenceUrl("v1/allocations/report"),
            reports
        )
        return response.data ?: throw Exception(
            response.error?.message ?: "Failed to report sequence consumption"
        )
    }

    override suspend fun getDeviceAllocations(deviceId: String, status: String?): List<SequenceAllocation> {
        val params = mutableMapOf<String, Any>("device_id" to deviceId)
        if (status != null) params["status"] = status

        val response: Response<List<SequenceAllocation>> = get(
            client,
            ApiUrlBuilder.sequenceUrl("v1/allocations"),
            params
        )
        return response.data ?: throw Exception(
            response.error?.message ?: "Failed to fetch device allocations"
        )
    }
}
