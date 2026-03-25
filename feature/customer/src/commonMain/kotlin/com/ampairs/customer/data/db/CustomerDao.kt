package com.ampairs.customer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Query("SELECT * FROM customers WHERE active = 1 ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerById(customerId: String): CustomerEntity?

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun observeCustomerById(customerId: String): Flow<CustomerEntity?>

    @Query(
        """
        SELECT * FROM customers
        WHERE active = 1
        AND (name LIKE '%' || :searchQuery || '%'
             OR phone LIKE '%' || :searchQuery || '%'
             OR email LIKE '%' || :searchQuery || '%')
        ORDER BY name ASC
    """
    )
    fun searchCustomers(searchQuery: String): Flow<List<CustomerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomers(customers: List<CustomerEntity>)

    @Update
    suspend fun updateCustomer(customer: CustomerEntity)

    @Query("DELETE FROM customers WHERE id = :customerId")
    suspend fun deleteCustomer(customerId: String)

    @Query("SELECT COUNT(*) FROM customers WHERE active = 1")
    suspend fun getCustomerCount(): Int

    @Query("SELECT * FROM customers WHERE synced = 0")
    suspend fun getUnsyncedCustomers(): List<CustomerEntity>


    @Query("DELETE FROM customers")
    suspend fun clearWorkspaceCustomers()

    @Query(
        """
        SELECT DISTINCT city FROM customers
        WHERE active = 1 AND city IS NOT NULL AND city != ''
        ORDER BY created_at DESC
        LIMIT 1000
    """
    )
    suspend fun getUniqueCities(): List<String>

    @Query(
        """
        SELECT DISTINCT pincode FROM customers
        WHERE active = 1 AND pincode IS NOT NULL AND pincode != ''
        ORDER BY created_at DESC
        LIMIT 1000
    """
    )
    suspend fun getUniquePincodes(): List<String>
}