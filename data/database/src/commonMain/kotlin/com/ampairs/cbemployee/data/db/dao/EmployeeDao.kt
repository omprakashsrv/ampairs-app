package com.ampairs.cbemployee.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.cbemployee.data.db.entity.EmployeeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {

    @Query("SELECT * FROM cb_employees WHERE active = 1 ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM cb_employees WHERE zonal_office_id = :zoneId AND active = 1 ORDER BY name ASC")
    fun getEmployeesByZone(zoneId: String): Flow<List<EmployeeEntity>>

    @Query("SELECT * FROM cb_employees WHERE id = :id")
    suspend fun getEmployeeById(id: String): EmployeeEntity?

    @Query("SELECT * FROM cb_employees WHERE synced = 0")
    suspend fun getUnsyncedEmployees(): List<EmployeeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: EmployeeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployees(employees: List<EmployeeEntity>)

    @Query("UPDATE cb_employees SET active = 0, synced = 0 WHERE id = :id")
    suspend fun softDeleteEmployee(id: String)

    @Query("DELETE FROM cb_employees WHERE id = :id")
    suspend fun hardDeleteEmployee(id: String)
}
