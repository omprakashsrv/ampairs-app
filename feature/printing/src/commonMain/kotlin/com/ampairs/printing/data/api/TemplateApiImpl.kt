package com.ampairs.printing.data.api

import com.ampairs.auth.api.TokenRepository
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

/** Ktor implementation of [TemplateApi] on the canonical `/printing/v1/templates/sync` contract. */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class TemplateApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : TemplateApi {

    private val client = httpClient(engine, tokenRepository)

    override suspend fun getTemplatesSync(
        lastSync: String,
        page: Int,
        size: Int,
        sortBy: String,
        sortDir: String,
    ): PageResponse<TemplateDto> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync

        val response: Response<PageResponse<TemplateDto>> =
            get(client, ApiUrlBuilder.printingUrl("v1/templates/sync"), params)
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: PageResponse(
            content = emptyList(), pageNumber = page, pageSize = size, totalPages = 0,
            totalElements = 0L, hasNext = false, hasPrevious = false, first = true, last = true,
        )
    }

    override suspend fun pushTemplates(templates: List<TemplateDto>): List<TemplateDto> {
        val response: Response<List<TemplateDto>> =
            post(client, ApiUrlBuilder.printingUrl("v1/templates/sync"), templates)
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyList()
    }
}
