package com.ampairs.cbemployee.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A maintenance-org roster row. Matches the backend `cb_employee`
 * EmployeeResponse. `role` is the backend enum name (EXECUTIVE / SENIOR_EXECUTIVE /
 * ASSISTANT_MANAGER / MAINTENANCE_INCHARGE / MAINTENANCE_LEADER).
 */
@Serializable
data class Employee(
    val uid: String = "",
    @SerialName("employee_no") val employeeNo: String = "",
    val name: String = "",
    val role: String = "EXECUTIVE",
    val email: String? = null,
    val mobile: String? = null,
    @SerialName("reports_to_employee_id") val reportsToEmployeeId: String? = null,
    @SerialName("zonal_office_id") val zonalOfficeId: String? = null,
    @SerialName("mapped_store_ids") val mappedStoreIds: List<String>? = null,
    @SerialName("user_id") val userId: String? = null,
    val active: Boolean = true,
    @SerialName("ref_id") val refId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

/** The maintenance roles, mirroring the backend enum. UI-only convenience. */
object MaintenanceRoles {
    const val EXECUTIVE = "EXECUTIVE"
    const val SENIOR_EXECUTIVE = "SENIOR_EXECUTIVE"
    const val ASSISTANT_MANAGER = "ASSISTANT_MANAGER"
    const val MAINTENANCE_INCHARGE = "MAINTENANCE_INCHARGE"
    const val MAINTENANCE_LEADER = "MAINTENANCE_LEADER"

    val ALL = listOf(EXECUTIVE, SENIOR_EXECUTIVE, ASSISTANT_MANAGER, MAINTENANCE_INCHARGE, MAINTENANCE_LEADER)
}
