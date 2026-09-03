package com.ampairs.cbmaintenance.data.api

import com.ampairs.cbmaintenance.domain.model.AssetCategoryAlias
import com.ampairs.cbmaintenance.domain.model.PmEntry
import com.ampairs.cbmaintenance.domain.model.PmSchedule
import com.ampairs.cbmaintenance.domain.model.Ticket
import com.ampairs.cbmaintenance.domain.model.TicketBucket
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response

/**
 * API for the `cb_maintenance` backend module. All four resources use the canonical `/sync`
 * contract (`GET`/`POST /cb_maintenance/v1/{resource}/sync`). The pm-entries + tickets pull feeds
 * are zone-scoped server-side to the calling employee.
 */
interface CbMaintenanceApi {

    suspend fun getPmSchedulesSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<PmSchedule>
    suspend fun bulkUpdatePmSchedules(items: List<PmSchedule>): List<PmSchedule>
    suspend fun getPmScheduleById(id: String): Response<PmSchedule>

    suspend fun getPmEntriesSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<PmEntry>
    suspend fun bulkUpdatePmEntries(items: List<PmEntry>): List<PmEntry>
    suspend fun getPmEntryById(id: String): Response<PmEntry>

    suspend fun getTicketsSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<Ticket>
    suspend fun bulkUpdateTickets(items: List<Ticket>): List<Ticket>
    suspend fun getTicketById(id: String): Response<Ticket>

    suspend fun getAliasesSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<AssetCategoryAlias>
    suspend fun bulkUpdateAliases(items: List<AssetCategoryAlias>): List<AssetCategoryAlias>

    /** Ticket-classification catalog — global reference data, pull-only. */
    suspend fun getTicketBucketsSync(lastSync: String, page: Int = 0, size: Int = 100, sortBy: String = "updatedAt", sortDir: String = "ASC"): PageResponse<TicketBucket>

    /**
     * On-demand server-side PM generation (the same work the nightly job does): rolls due PM entries
     * forward from active schedules × stores. Returns the number generated. Non-sync, UI-invoked.
     */
    suspend fun generatePmEntries(): Int
}
