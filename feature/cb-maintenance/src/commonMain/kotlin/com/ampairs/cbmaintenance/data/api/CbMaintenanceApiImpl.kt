package com.ampairs.cbmaintenance.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.cbmaintenance.domain.model.AssetCategoryAlias
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbmaintenance.domain.model.TicketBucket
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
class CbMaintenanceApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : CbMaintenanceApi {

    private val client = httpClient(engine, tokenRepository)

    private fun params(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String) =
        mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        ).also { if (lastSync.isNotBlank()) it["last_sync"] = lastSync }

    private fun <T> emptyPage(page: Int, size: Int): PageResponse<T> = PageResponse(
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

    // --- PM schedules ---------------------------------------------------------------------------
    override suspend fun getPmSchedulesSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<PmSchedule> {
        val response: Response<PageResponse<PmSchedule>> =
            get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/pm-schedules/sync"), params(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdatePmSchedules(items: List<PmSchedule>): List<PmSchedule> {
        val response: Response<List<PmSchedule>> = post(client, ApiUrlBuilder.cbMaintenanceUrl("v1/pm-schedules/sync"), items)
        return response.data ?: throw Exception("Failed to bulk update PM schedules")
    }

    override suspend fun getPmScheduleById(id: String): Response<PmSchedule> =
        get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/pm-schedules/$id"))

    // --- PM entries -----------------------------------------------------------------------------
    override suspend fun getPmEntriesSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<PmEntry> {
        val response: Response<PageResponse<PmEntry>> =
            get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/pm-entries/sync"), params(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdatePmEntries(items: List<PmEntry>): List<PmEntry> {
        val response: Response<List<PmEntry>> = post(client, ApiUrlBuilder.cbMaintenanceUrl("v1/pm-entries/sync"), items)
        return response.data ?: throw Exception("Failed to bulk update PM entries")
    }

    override suspend fun getPmEntryById(id: String): Response<PmEntry> =
        get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/pm-entries/$id"))

    // --- Tickets --------------------------------------------------------------------------------
    override suspend fun getTicketsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<Ticket> {
        val response: Response<PageResponse<Ticket>> =
            get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/tickets/sync"), params(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateTickets(items: List<Ticket>): List<Ticket> {
        val response: Response<List<Ticket>> = post(client, ApiUrlBuilder.cbMaintenanceUrl("v1/tickets/sync"), items)
        return response.data ?: throw Exception("Failed to bulk update tickets")
    }

    override suspend fun getTicketById(id: String): Response<Ticket> =
        get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/tickets/$id"))

    // --- Asset-category aliases -----------------------------------------------------------------
    override suspend fun getAliasesSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<AssetCategoryAlias> {
        val response: Response<PageResponse<AssetCategoryAlias>> =
            get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/asset-category-aliases/sync"), params(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateAliases(items: List<AssetCategoryAlias>): List<AssetCategoryAlias> {
        val response: Response<List<AssetCategoryAlias>> =
            post(client, ApiUrlBuilder.cbMaintenanceUrl("v1/asset-category-aliases/sync"), items)
        return response.data ?: throw Exception("Failed to bulk update aliases")
    }

    // --- Ticket buckets (global reference catalog, pull-only) -----------------------------------
    override suspend fun getTicketBucketsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<TicketBucket> {
        val response: Response<PageResponse<TicketBucket>> =
            get(client, ApiUrlBuilder.cbMaintenanceUrl("v1/ticket-buckets/sync"), params(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }
}
