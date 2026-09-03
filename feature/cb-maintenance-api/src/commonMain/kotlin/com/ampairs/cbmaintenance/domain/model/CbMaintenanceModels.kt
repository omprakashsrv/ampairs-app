package com.ampairs.cbmaintenance.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One checklist line result, part of a [PmEntry.checklistResult]. Matches the backend model. */
@Serializable
data class ChecklistItemResult(
    val item: String = "",
    val passed: Boolean = true,
    val note: String? = null,
)

/** A preventive-maintenance task attached to an asset category (backend `cb_maintenance`). */
@Serializable
data class PmSchedule(
    val uid: String = "",
    val department: String = "",
    @SerialName("asset_category") val assetCategory: String = "",
    @SerialName("ticket_bucket_id") val ticketBucketId: String? = null,
    @SerialName("task_name") val taskName: String = "",
    val checklist: List<String>? = null,
    @SerialName("frequency_unit") val frequencyUnit: String = "MONTH",
    @SerialName("frequency_interval") val frequencyInterval: Int = 1,
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** One occurrence of a PM task at a store (backend `cb_maintenance`). */
@Serializable
data class PmEntry(
    val uid: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("zonal_office_id") val zonalOfficeId: String = "",
    @SerialName("asset_category") val assetCategory: String = "",
    @SerialName("pm_schedule_id") val pmScheduleId: String? = null,
    val source: String = "SCHEDULED",
    @SerialName("due_date") val dueDate: String? = null,
    val status: String = "DUE",
    @SerialName("assigned_to_employee_id") val assignedToEmployeeId: String? = null,
    @SerialName("assisted_by_employee_ids") val assistedByEmployeeIds: List<String>? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("completed_by_employee_id") val completedByEmployeeId: String? = null,
    @SerialName("checklist_result") val checklistResult: List<ChecklistItemResult>? = null,
    @SerialName("ticket_id") val ticketId: String? = null,
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** A reactive maintenance ticket (backend `cb_maintenance`). */
@Serializable
data class Ticket(
    val uid: String = "",
    @SerialName("store_id") val storeId: String = "",
    @SerialName("zonal_office_id") val zonalOfficeId: String = "",
    @SerialName("asset_category") val assetCategory: String = "",
    @SerialName("sub_category") val subCategory: String = "",
    @SerialName("ticket_bucket_id") val ticketBucketId: String? = null,
    val description: String? = null,
    val status: String = "OPEN",
    @SerialName("assigned_to_employee_id") val assignedToEmployeeId: String? = null,
    @SerialName("assisted_by_employee_ids") val assistedByEmployeeIds: List<String>? = null,
    @SerialName("raised_by_employee_id") val raisedByEmployeeId: String? = null,
    @SerialName("raised_at") val raisedAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("origin_pm_entry_id") val originPmEntryId: String? = null,
    @SerialName("suggested_spare_part") val suggestedSparePart: String? = null,
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/**
 * One leaf of the ticket-classification taxonomy: Department › Category › Sub category 1
 * [› Sub category 2]. Global reference data (backend `cb_maintenance`) — drives the cascading
 * pickers on the raise-ticket form. `subCategory2` is "" when the leaf has only three levels.
 */
@Serializable
data class TicketBucket(
    val uid: String = "",
    val department: String = "",
    val category: String = "",
    @SerialName("sub_category1") val subCategory1: String = "",
    @SerialName("sub_category2") val subCategory2: String = "",
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** Normalizes messy asset-category source names to a canonical value (backend `cb_maintenance`). */
@Serializable
data class AssetCategoryAlias(
    val uid: String = "",
    val canonical: String = "",
    val alias: String = "",
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)
