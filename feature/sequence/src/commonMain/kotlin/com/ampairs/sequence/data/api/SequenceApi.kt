package com.ampairs.sequence.data.api

import com.ampairs.common.model.PageResponse
import com.ampairs.sequence.domain.model.SequenceAllocation
import com.ampairs.sequence.domain.model.SequenceAllocationReportRequest
import com.ampairs.sequence.domain.model.SequenceAllocationRequest
import com.ampairs.sequence.domain.model.SequenceDefinition

/**
 * API for the backend `sequence` module.
 *
 * Definitions ride the canonical `/v1/definitions/sync` contract (pull includes inactive
 * rows; push is a uid-keyed bulk upsert). Allocations are an off-contract device RPC:
 * grant ([requestAllocation]), consumption report ([reportAllocations]) and device
 * recovery ([getDeviceAllocations]).
 */
interface SequenceApi {

    /** Incremental pull of definitions — includes inactive rows. Throws on a non-null API error. */
    suspend fun getDefinitionsSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<SequenceDefinition>

    /** Bulk upsert definitions (deactivations in-band). Throws on failure. */
    suspend fun bulkUpdateDefinitions(definitions: List<SequenceDefinition>): List<SequenceDefinition>

    /** Request an exclusive number block for this device. Throws on failure. */
    suspend fun requestAllocation(request: SequenceAllocationRequest): SequenceAllocation

    /** Report consumption progress for one or more allocations. Throws on failure. */
    suspend fun reportAllocations(reports: List<SequenceAllocationReportRequest>): List<SequenceAllocation>

    /** Allocations granted to this device (recovery after reinstall). Throws on failure. */
    suspend fun getDeviceAllocations(deviceId: String, status: String? = null): List<SequenceAllocation>
}
