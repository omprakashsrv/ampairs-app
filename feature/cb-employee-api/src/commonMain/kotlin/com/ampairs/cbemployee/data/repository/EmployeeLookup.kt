package com.ampairs.cbemployee.data.repository

import com.ampairs.cbemployee.domain.model.Employee

/**
 * Cross-feature read surface for the maintenance roster. `cb-maintenance` depends on this (`-api`)
 * interface only — for assignment dropdowns and display.
 */
interface EmployeeLookup {
    suspend fun activeEmployees(): List<Employee>
    suspend fun employeesInZone(zoneId: String): List<Employee>
    suspend fun getEmployee(id: String): Employee?
}
