package com.ampairs.sfa.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.sfa.domain.model.Attendance
import com.ampairs.sfa.domain.model.Beat
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
}
