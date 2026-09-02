package com.ampairs.cbemployee.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for a California Burrito maintenance-org roster row.
 *
 * `mapped_store_ids` is stored as a JSON string (the feature mapper encodes/decodes it) so no Room
 * type converter is needed. `id` is the backend `uid`.
 */
@Entity(
    tableName = "cb_employees",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_employee_id_idx"),
        Index(value = ["zonal_office_id"], name = "cb_employee_zone_idx"),
        Index(value = ["user_id"], name = "cb_employee_user_idx"),
    ],
)
data class EmployeeEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "employee_no") val employeeNo: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "role") val role: String,
    @ColumnInfo(name = "email") val email: String? = null,
    @ColumnInfo(name = "mobile") val mobile: String? = null,
    @ColumnInfo(name = "reports_to_employee_id") val reportsToEmployeeId: String? = null,
    @ColumnInfo(name = "zonal_office_id") val zonalOfficeId: String? = null,
    @ColumnInfo(name = "mapped_store_ids") val mappedStoreIds: String? = null,
    @ColumnInfo(name = "user_id") val userId: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)
