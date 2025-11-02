package com.ampairs.customer.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerGroupDao {

    @Query("SELECT * FROM customer_groups WHERE active = 1 ORDER BY name ASC")
    fun getAllCustomerGroups(): Flow<List<CustomerGroupEntity>>

    @Query("SELECT * FROM customer_groups WHERE name LIKE '%' || :query || '%' AND active = 1 ORDER BY name ASC")
    fun searchCustomerGroups(query: String): Flow<List<CustomerGroupEntity>>

    @Query("SELECT * FROM customer_groups WHERE id = :id")
    suspend fun getCustomerGroupById(id: String): CustomerGroupEntity?

    @Query("SELECT * FROM customer_groups WHERE name = :name AND active = 1")
    suspend fun getCustomerGroupByName(name: String): CustomerGroupEntity?

    @Query("SELECT * FROM customer_groups WHERE synced = 0")
    suspend fun getUnsyncedCustomerGroups(): List<CustomerGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerGroup(customerGroup: CustomerGroupEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerGroups(customerGroups: List<CustomerGroupEntity>)

    @Update
    suspend fun updateCustomerGroup(customerGroup: CustomerGroupEntity)

    @Query("UPDATE customer_groups SET active = 0 WHERE id = :id")
    suspend fun deleteCustomerGroup(id: String)

    @Query("DELETE FROM customer_groups WHERE id = :id")
    suspend fun hardDeleteCustomerGroup(id: String)

    @Query("DELETE FROM customer_groups")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM customer_groups WHERE active = 1")
    suspend fun getActiveCustomerGroupsCount(): Int
}