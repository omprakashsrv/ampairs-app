package com.ampairs.sfa.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit

/**
 * Talks to the canonical SFA `/sync` contract (`GET`/`POST /sfa/v1/{resource}/sync`).
 * Pulls include soft-deleted rows; pushes are UID-keyed bulk upserts (deletes ride in-band).
 */
interface SfaApi {
    suspend fun getBeatsSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<Beat>
    suspend fun bulkUpdateBeats(beats: List<Beat>): List<Beat>

    suspend fun getVisitsSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<Visit>
    suspend fun bulkUpdateVisits(visits: List<Visit>): List<Visit>

    suspend fun getAttendanceSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<Attendance>
    suspend fun bulkUpdateAttendance(records: List<Attendance>): List<Attendance>

    suspend fun getBeatOutletsSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<BeatOutlet>
    suspend fun bulkUpdateBeatOutlets(rows: List<BeatOutlet>): List<BeatOutlet>

    suspend fun getJourneyPlansSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<JourneyPlan>
    suspend fun bulkUpdateJourneyPlans(rows: List<JourneyPlan>): List<JourneyPlan>

    suspend fun getPlannedVisitsSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<PlannedVisit>
    suspend fun bulkUpdatePlannedVisits(rows: List<PlannedVisit>): List<PlannedVisit>
}
