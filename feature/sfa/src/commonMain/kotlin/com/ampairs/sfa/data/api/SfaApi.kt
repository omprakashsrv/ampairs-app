package com.ampairs.sfa.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
import com.ampairs.sfa.domain.model.BeatOutlet
import com.ampairs.sfa.domain.model.FieldOrder
import com.ampairs.sfa.domain.model.Leave
import com.ampairs.sfa.domain.model.JourneyPlan
import com.ampairs.sfa.domain.model.PlannedVisit
import com.ampairs.sfa.domain.model.Visit
import com.ampairs.sfa.domain.model.VisitSurveyResponse

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

    suspend fun getFieldOrdersSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<FieldOrder>
    suspend fun bulkUpdateFieldOrders(rows: List<FieldOrder>): List<FieldOrder>

    suspend fun getLeavesSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<Leave>
    suspend fun bulkUpdateLeaves(rows: List<Leave>): List<Leave>

    suspend fun getVisitSurveysSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<VisitSurveyResponse>
    suspend fun bulkUpdateVisitSurveys(rows: List<VisitSurveyResponse>): List<VisitSurveyResponse>
}
