package com.ampairs.customer.agent

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Query

/** A customer hit for the assistant's chat search (id + name + phone + city). */
data class CustomerSearchHit(
    @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String?,
    @ColumnInfo(name = "city") val city: String?,
)

/**
 * Read-only report/search queries backing the assistant's customer actions — separate from the
 * operational [com.ampairs.customer.data.db.CustomerDao]. Over the existing `customers` table (no
 * schema impact).
 */
@Dao
interface CustomerAgentDao {
    /** Count of active customers. */
    @Query("SELECT count(*) FROM customers WHERE active = 1")
    suspend fun countActive(): Int

    /**
     * Chat-triggered customer search. Case-insensitive and whitespace-normalized on `name` (so a
     * single-space query matches double-spaced names), plus phone (spaces stripped), email and GST.
     */
    @Query(
        """
        SELECT id, name, phone, city FROM customers
        WHERE active = 1
          AND (REPLACE(REPLACE(REPLACE(REPLACE(LOWER(name), '    ', ' '), '   ', ' '), '  ', ' '), '  ', ' ') LIKE '%' || LOWER(:term) || '%'
               OR REPLACE(phone, ' ', '') LIKE '%' || REPLACE(:term, ' ', '') || '%'
               OR LOWER(email) LIKE '%' || LOWER(:term) || '%'
               OR LOWER(gstNumber) LIKE '%' || LOWER(:term) || '%')
        ORDER BY name ASC LIMIT :limit
        """
    )
    suspend fun searchCustomers(term: String, limit: Long): List<CustomerSearchHit>
}
