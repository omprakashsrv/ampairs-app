package com.ampairs.cbemployee.data.api

import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response

/** API for the `cb_employee` backend module — canonical `/cb_employee/v1/employees/sync`. */
interface CbEmployeeApi {

    suspend fun getEmployeesSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<Employee>

    suspend fun bulkUpdateEmployees(employees: List<Employee>): List<Employee>

    suspend fun getEmployeeById(id: String): Response<Employee>
}
