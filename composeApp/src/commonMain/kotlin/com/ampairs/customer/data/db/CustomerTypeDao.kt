package com.ampairs.customer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerTypeDao {

    @Query("SELECT * FROM customer_types WHERE active = 1 ORDER BY name ASC")
    fun getAllCustomerTypes(): Flow<List<CustomerTypeEntity>>

    @Query("SELECT * FROM customer_types WHERE name LIKE '%' || :query || '%' AND active = 1 ORDER BY name ASC")
    fun searchCustomerTypes(query: String): Flow<List<CustomerTypeEntity>>

    @Query("SELECT * FROM customer_types WHERE id = :id")
    suspend fun getCustomerTypeById(id: String): CustomerTypeEntity?

    @Query("SELECT * FROM customer_types WHERE name = :name AND active = 1")
    suspend fun getCustomerTypeByName(name: String): CustomerTypeEntity?

    @Query("SELECT * FROM customer_types WHERE synced = 0")
    suspend fun getUnsyncedCustomerTypes(): List<CustomerTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerType(customerType: CustomerTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerTypes(customerTypes: List<CustomerTypeEntity>)

    @Update
    suspend fun updateCustomerType(customerType: CustomerTypeEntity)

    @Query("UPDATE customer_types SET active = 0 WHERE id = :id")
    suspend fun deleteCustomerType(id: String)

    @Query("DELETE FROM customer_types WHERE id = :id")
    suspend fun hardDeleteCustomerType(id: String)

    @Query("DELETE FROM customer_types")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM customer_types WHERE active = 1")
    suspend fun getActiveCustomerTypesCount(): Int
}