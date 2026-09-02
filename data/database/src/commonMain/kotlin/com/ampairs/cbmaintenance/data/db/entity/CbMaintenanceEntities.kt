package com.ampairs.cbmaintenance.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * cb_maintenance Room entities. JSON-list columns (`checklist`, `checklist_result`,
 * `assisted_by_employee_ids`) are stored as JSON strings — the feature mappers encode/decode them,
 * so no Room type converters are required. `id` is the backend `uid`.
 */
@Entity(
    tableName = "cb_pm_schedules",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_pm_schedule_id_idx"),
        Index(value = ["asset_category"], name = "cb_pm_schedule_category_idx"),
    ],
)
data class PmScheduleEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "asset_category") val assetCategory: String,
    @ColumnInfo(name = "task_name") val taskName: String,
    @ColumnInfo(name = "checklist") val checklist: String? = null,
    @ColumnInfo(name = "frequency_unit") val frequencyUnit: String,
    @ColumnInfo(name = "frequency_interval") val frequencyInterval: Int,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)

@Entity(
    tableName = "cb_pm_entries",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_pm_entry_id_idx"),
        Index(value = ["zonal_office_id"], name = "cb_pm_entry_zone_idx"),
        Index(value = ["status"], name = "cb_pm_entry_status_idx"),
        Index(value = ["assigned_to_employee_id"], name = "cb_pm_entry_assignee_idx"),
    ],
)
data class PmEntryEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "zonal_office_id") val zonalOfficeId: String,
    @ColumnInfo(name = "asset_category") val assetCategory: String,
    @ColumnInfo(name = "pm_schedule_id") val pmScheduleId: String? = null,
    @ColumnInfo(name = "source") val source: String,
    @ColumnInfo(name = "due_date") val dueDate: String? = null,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "assigned_to_employee_id") val assignedToEmployeeId: String? = null,
    @ColumnInfo(name = "assisted_by_employee_ids") val assistedByEmployeeIds: String? = null,
    @ColumnInfo(name = "completed_at") val completedAt: String? = null,
    @ColumnInfo(name = "completed_by_employee_id") val completedByEmployeeId: String? = null,
    @ColumnInfo(name = "checklist_result") val checklistResult: String? = null,
    @ColumnInfo(name = "ticket_id") val ticketId: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)

@Entity(
    tableName = "cb_tickets",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_ticket_id_idx"),
        Index(value = ["zonal_office_id"], name = "cb_ticket_zone_idx"),
        Index(value = ["status"], name = "cb_ticket_status_idx"),
        Index(value = ["assigned_to_employee_id"], name = "cb_ticket_assignee_idx"),
    ],
)
data class TicketEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "store_id") val storeId: String,
    @ColumnInfo(name = "zonal_office_id") val zonalOfficeId: String,
    @ColumnInfo(name = "asset_category") val assetCategory: String,
    @ColumnInfo(name = "sub_category") val subCategory: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "assigned_to_employee_id") val assignedToEmployeeId: String? = null,
    @ColumnInfo(name = "assisted_by_employee_ids") val assistedByEmployeeIds: String? = null,
    @ColumnInfo(name = "raised_by_employee_id") val raisedByEmployeeId: String? = null,
    @ColumnInfo(name = "raised_at") val raisedAt: String? = null,
    @ColumnInfo(name = "resolved_at") val resolvedAt: String? = null,
    @ColumnInfo(name = "origin_pm_entry_id") val originPmEntryId: String? = null,
    @ColumnInfo(name = "suggested_spare_part") val suggestedSparePart: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)

@Entity(
    tableName = "cb_asset_category_aliases",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_asset_alias_id_idx"),
    ],
)
data class AssetCategoryAliasEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "canonical") val canonical: String,
    @ColumnInfo(name = "alias") val alias: String,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)
