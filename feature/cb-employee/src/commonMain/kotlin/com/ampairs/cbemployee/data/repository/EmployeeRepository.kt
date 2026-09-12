package com.ampairs.cbemployee.data.repository

import com.ampairs.cbemployee.data.db.dao.EmployeeDao
import com.ampairs.cbemployee.data.db.entity.toEmployee
import com.ampairs.cbemployee.data.db.entity.toEntity
import com.ampairs.cbemployee.domain.model.Employee
import com.ampairs.cbemployee.util.CbEmployeeLogger
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.db.SyncStateDao
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Local-only data access for the maintenance roster. The [CbEmployeeApi] is owned by
 * [com.ampairs.cbemployee.sync.EmployeeSyncDelegate]; writes here persist to Room as unsynced and
 * mark CB_EMPLOYEE PENDING_PUSH.
 */
@OptIn(ExperimentalTime::class)
@Inject
class EmployeeRepository(
    private val employeeDao: EmployeeDao,
    private val syncStateDao: SyncStateDao,
) : EmployeeLookup {

    fun observeEmployees(): Flow<List<Employee>> =
        employeeDao.getAllEmployees().map { list -> list.map { it.toEmployee() } }

    suspend fun saveEmployee(employee: Employee): Result<Employee> = runCatching {
        require(employee.uid.isNotBlank()) { "UID must be set by ViewModel" }
        employeeDao.insertEmployee(employee.toEntity().copy(synced = false))
        markPending()
        employee
    }.onFailure { CbEmployeeLogger.e("EmployeeRepository", "saveEmployee failed", it) }

    suspend fun deleteEmployee(id: String): Result<Unit> = runCatching {
        val existing = employeeDao.getEmployeeById(id)
        if (existing != null) {
            employeeDao.insertEmployee(existing.copy(active = false, synced = false))
            markPending()
        }
        Unit
    }.onFailure { CbEmployeeLogger.e("EmployeeRepository", "deleteEmployee failed", it) }

    // --- EmployeeLookup (cross-feature reads) -----------------------------------------------
    override suspend fun activeEmployees(): List<Employee> =
        employeeDao.getAllEmployees().first().map { it.toEmployee() }

    override suspend fun employeesInZone(zoneId: String): List<Employee> =
        employeeDao.getEmployeesByZone(zoneId).first().map { it.toEmployee() }

    override suspend fun getEmployee(id: String): Employee? =
        employeeDao.getEmployeeById(id)?.toEmployee()

    private suspend fun markPending() {
        syncStateDao.markPendingPush(SyncEntity.CB_EMPLOYEE, Clock.System.now().toEpochMilliseconds())
    }
}
