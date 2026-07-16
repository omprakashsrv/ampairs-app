package com.ampairs.customer.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Update
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
             OR REPLACE(phone, ' ', '') LIKE '%' || REPLACE(:searchQuery, ' ', '') || '%'
             OR email LIKE '%' || :searchQuery || '%'
             OR gstNumber LIKE '%' || :searchQuery || '%'
             OR state LIKE '%' || :searchQuery || '%')
        ORDER BY name ASC
    """
    )
    fun searchCustomers(searchQuery: String): Flow<List<CustomerEntity>>

    /**
     * Search + multi-select filter in a single query. Each filter dimension is bypassed when its
     * `hasX` flag is 0 (i.e. nothing selected), otherwise it matches the stored value against the
     * selected set. Backed by the `customer_state_idx` / `customer_type_idx` / `customer_group_idx`
     * indexes for fast filtering.
     */
    @Query(
        """
        SELECT * FROM customers
        WHERE active = 1
        AND (:hasStates = 0 OR state IN (:states))
        AND (:hasTypes = 0 OR customer_type IN (:types))
        AND (:hasGroups = 0 OR customer_group IN (:groups))
        AND (
            :searchQuery = ''
            OR name LIKE '%' || :searchQuery || '%'
            OR REPLACE(phone, ' ', '') LIKE '%' || REPLACE(:searchQuery, ' ', '') || '%'
            OR email LIKE '%' || :searchQuery || '%'
            OR gstNumber LIKE '%' || :searchQuery || '%'
            OR state LIKE '%' || :searchQuery || '%'
        )
        ORDER BY name ASC
    """
    )
    fun filterCustomers(
        searchQuery: String,
        states: List<String>,
        hasStates: Int,
        types: List<String>,
        hasTypes: Int,
        groups: List<String>,
        hasGroups: Int,
    ): Flow<List<CustomerEntity>>

    @Query(
        "SELECT DISTINCT state FROM customers WHERE active = 1 AND state IS NOT NULL AND state != '' ORDER BY state ASC"
    )
    suspend fun getDistinctStates(): List<String>

    @Query(
        "SELECT DISTINCT customer_type FROM customers WHERE active = 1 AND customer_type IS NOT NULL AND customer_type != '' ORDER BY customer_type ASC"
    )
    suspend fun getDistinctCustomerTypes(): List<String>

    @Query(
        "SELECT DISTINCT customer_group FROM customers WHERE active = 1 AND customer_group IS NOT NULL AND customer_group != '' ORDER BY customer_group ASC"
    )
    suspend fun getDistinctCustomerGroups(): List<String>

    @Query("SELECT * FROM customers WHERE ref_id IN (:refs)")
    suspend fun getCustomersByTallyRefIds(refs: List<String>): List<CustomerEntity>

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

    @Query("SELECT COUNT(*) FROM customers WHERE synced = 0")
    fun observeUnsyncedCount(): Flow<Int>


    @Query("DELETE FROM customers")
    suspend fun clearWorkspaceCustomers()

    @Query("UPDATE customers SET phone = NULL WHERE phone IS NOT NULL AND length(phone) != 10")
    suspend fun nullifyInvalidPhones()

    @Query("UPDATE customers SET pincode = NULL WHERE pincode IS NOT NULL AND length(pincode) != 6")
    suspend fun nullifyInvalidPincodes()

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