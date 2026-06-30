package com.ampairs.sfa.data.api

import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.di.AppScope
import com.ampairs.common.get
import com.ampairs.common.httpClient
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response
import com.ampairs.common.post
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import io.ktor.client.engine.HttpClientEngine

@Inject @SingleIn(AppScope::class) @ContributesBinding(AppScope::class)
class SfaApiImpl(
    engine: HttpClientEngine,
    tokenRepository: TokenRepository,
) : SfaApi {

    private val client = httpClient(engine, tokenRepository)

    private fun syncParams(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): MutableMap<String, Any> {
        val params = mutableMapOf<String, Any>(
            "page" to page,
            "size" to size,
            "sort_by" to sortBy,
            "sort_dir" to sortDir,
        )
        if (lastSync.isNotBlank()) params["last_sync"] = lastSync
        return params
    }

    private fun <T> emptyPage(page: Int, size: Int): PageResponse<T> = PageResponse(
        content = emptyList(), pageNumber = page, pageSize = size, totalPages = 0,
        totalElements = 0L, hasNext = false, hasPrevious = false, first = true, last = true,
    )

    override suspend fun getBeatsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<Beat> {
        val response: Response<PageResponse<Beat>> = get(client, ApiUrlBuilder.sfaUrl("v1/beats/sync"), syncParams(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateBeats(beats: List<Beat>): List<Beat> {
        val response: Response<List<Beat>> = post(client, ApiUrlBuilder.sfaUrl("v1/beats/sync"), beats)
        return response.data ?: throw Exception("Failed to bulk update beats")
    }

    override suspend fun getVisitsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<Visit> {
        val response: Response<PageResponse<Visit>> = get(client, ApiUrlBuilder.sfaUrl("v1/visits/sync"), syncParams(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateVisits(visits: List<Visit>): List<Visit> {
        val response: Response<List<Visit>> = post(client, ApiUrlBuilder.sfaUrl("v1/visits/sync"), visits)
        return response.data ?: throw Exception("Failed to bulk update visits")
    }

    override suspend fun getAttendanceSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<Attendance> {
        val response: Response<PageResponse<Attendance>> = get(client, ApiUrlBuilder.sfaUrl("v1/attendance/sync"), syncParams(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateAttendance(records: List<Attendance>): List<Attendance> {
        val response: Response<List<Attendance>> = post(client, ApiUrlBuilder.sfaUrl("v1/attendance/sync"), records)
        return response.data ?: throw Exception("Failed to bulk update attendance")
    }

    override suspend fun getBeatOutletsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<BeatOutlet> {
        val response: Response<PageResponse<BeatOutlet>> = get(client, ApiUrlBuilder.sfaUrl("v1/beat-outlets/sync"), syncParams(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateBeatOutlets(rows: List<BeatOutlet>): List<BeatOutlet> {
        val response: Response<List<BeatOutlet>> = post(client, ApiUrlBuilder.sfaUrl("v1/beat-outlets/sync"), rows)
        return response.data ?: throw Exception("Failed to bulk update beat outlets")
    }

    override suspend fun getJourneyPlansSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<JourneyPlan> {
        val response: Response<PageResponse<JourneyPlan>> = get(client, ApiUrlBuilder.sfaUrl("v1/journey-plans/sync"), syncParams(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdateJourneyPlans(rows: List<JourneyPlan>): List<JourneyPlan> {
        val response: Response<List<JourneyPlan>> = post(client, ApiUrlBuilder.sfaUrl("v1/journey-plans/sync"), rows)
        return response.data ?: throw Exception("Failed to bulk update journey plans")
    }

    override suspend fun getPlannedVisitsSync(lastSync: String, page: Int, size: Int, sortBy: String, sortDir: String): PageResponse<PlannedVisit> {
        val response: Response<PageResponse<PlannedVisit>> = get(client, ApiUrlBuilder.sfaUrl("v1/planned-visits/sync"), syncParams(lastSync, page, size, sortBy, sortDir))
        if (response.error != null) throw Exception(response.error?.message ?: "Network error")
        return response.data ?: emptyPage(page, size)
    }

    override suspend fun bulkUpdatePlannedVisits(rows: List<PlannedVisit>): List<PlannedVisit> {
        val response: Response<List<PlannedVisit>> = post(client, ApiUrlBuilder.sfaUrl("v1/planned-visits/sync"), rows)
        return response.data ?: throw Exception("Failed to bulk update planned visits")
    }
}
