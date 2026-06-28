package com.ampairs.communication.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.postList
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

/**
 * The communication module's syncable resources on the canonical `/communication/v1/{resource}/sync`
 * contract. Templates are aggregate-grained (header + variants); the rest are standard single-row.
 * Logs are pull-only (server-authored).
 */
interface CommunicationSyncApi {
    suspend fun pullTemplates(page: Int, size: Int, lastSync: String?): Response<PageResponse<TemplateAggregateDto>>
    suspend fun pushTemplates(items: List<TemplateAggregateDto>): Response<List<TemplateAggregateDto>>

    suspend fun pullBindings(page: Int, size: Int, lastSync: String?): Response<PageResponse<BindingDto>>
    suspend fun pushBindings(items: List<BindingDto>): Response<List<BindingDto>>

    suspend fun pullSchedules(page: Int, size: Int, lastSync: String?): Response<PageResponse<ScheduleDto>>
    suspend fun pushSchedules(items: List<ScheduleDto>): Response<List<ScheduleDto>>

    suspend fun pullCampaigns(page: Int, size: Int, lastSync: String?): Response<PageResponse<CampaignDto>>
    suspend fun pushCampaigns(items: List<CampaignDto>): Response<List<CampaignDto>>

    suspend fun pullPreferences(page: Int, size: Int, lastSync: String?): Response<PageResponse<PreferenceDto>>
    suspend fun pushPreferences(items: List<PreferenceDto>): Response<List<PreferenceDto>>

    suspend fun pullLogs(page: Int, size: Int, lastSync: String?): Response<PageResponse<CommunicationLogDto>>
}

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CommunicationSyncApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : CommunicationSyncApi {

    private val client = httpClient(engine, tokenRepository)

    private fun syncParams(page: Int, size: Int, lastSync: String?): MutableMap<String, Any> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to "updatedAt",
            "sort_dir" to "ASC",
        )
        lastSync?.takeIf { it.isNotBlank() }?.let { params["last_sync"] = it }
        return params
    }

    private fun url(resource: String) = ApiUrlBuilder.communicationUrl("v1/$resource/sync")

    override suspend fun pullTemplates(page: Int, size: Int, lastSync: String?): Response<PageResponse<TemplateAggregateDto>> =
        get(client, url("templates"), syncParams(page, size, lastSync))

    override suspend fun pushTemplates(items: List<TemplateAggregateDto>): Response<List<TemplateAggregateDto>> =
        postList(client, url("templates"), items)

    override suspend fun pullBindings(page: Int, size: Int, lastSync: String?): Response<PageResponse<BindingDto>> =
        get(client, url("bindings"), syncParams(page, size, lastSync))

    override suspend fun pushBindings(items: List<BindingDto>): Response<List<BindingDto>> =
        postList(client, url("bindings"), items)

    override suspend fun pullSchedules(page: Int, size: Int, lastSync: String?): Response<PageResponse<ScheduleDto>> =
        get(client, url("schedules"), syncParams(page, size, lastSync))

    override suspend fun pushSchedules(items: List<ScheduleDto>): Response<List<ScheduleDto>> =
        postList(client, url("schedules"), items)

    override suspend fun pullCampaigns(page: Int, size: Int, lastSync: String?): Response<PageResponse<CampaignDto>> =
        get(client, url("campaigns"), syncParams(page, size, lastSync))

    override suspend fun pushCampaigns(items: List<CampaignDto>): Response<List<CampaignDto>> =
        postList(client, url("campaigns"), items)

    override suspend fun pullPreferences(page: Int, size: Int, lastSync: String?): Response<PageResponse<PreferenceDto>> =
        get(client, url("preferences"), syncParams(page, size, lastSync))

    override suspend fun pushPreferences(items: List<PreferenceDto>): Response<List<PreferenceDto>> =
        postList(client, url("preferences"), items)

    override suspend fun pullLogs(page: Int, size: Int, lastSync: String?): Response<PageResponse<CommunicationLogDto>> =
        get(client, url("logs"), syncParams(page, size, lastSync))
}
