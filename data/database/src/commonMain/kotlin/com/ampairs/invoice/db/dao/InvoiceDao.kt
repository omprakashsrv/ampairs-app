package com.ampairs.invoice.db.dao

import androidx.paging.PagingSource
import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.ampairs.invoice.db.entity.InvoiceEntity

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoiceEntity WHERE id = :id")
    suspend fun selectById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC")
    suspend fun selectAll(): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE customer_id = :customerId AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByCustomer(customerId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE customer_id = :customerId AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByToCustomer(customerId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE status = :status AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByStatus(status: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE invoice_number LIKE '%' || :searchText || '%' AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByNumber(searchText: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE customer_name LIKE '%' || :searchText || '%' AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByCustomerName(searchText: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE invoice_date BETWEEN :startDate AND :endDate AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByDateRange(startDate: String, endDate: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE order_ref_id = :orderRefId AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByOrderRef(orderRefId: String): List<InvoiceEntity>

    @Query("SELECT count(*) FROM invoiceEntity WHERE active = 1")
    suspend fun countInvoices(): Int

    @Query("SELECT count(*) FROM invoiceEntity WHERE customer_id = :customerId AND active = 1")
    suspend fun countInvoicesByCustomer(customerId: String): Int

    @Query("SELECT count(*) FROM invoiceEntity WHERE status = :status AND active = 1")
    suspend fun countInvoicesByStatus(status: String): Int

    @Query("SELECT * FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC LIMIT :limit OFFSET :offset")
    suspend fun getInvoicesPaginated(limit: Long, offset: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE synced = 0 AND active = 1")
    suspend fun getUnsyncedInvoices(): List<InvoiceEntity>

    /** Highest sequence number used in a numbering series (spec 010 C4/C5) — for client numbering. */
    @Query("SELECT MAX(sequence_number) FROM invoiceEntity WHERE series = :series")
    suspend fun maxSequenceForSeries(series: String): Long?

    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE active = 1")
    suspend fun getTotalInvoiceValue(): Double?

    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE customer_id = :customerId AND active = 1")
    suspend fun getTotalInvoiceValueByCustomer(customerId: String): Double?

    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE invoice_date BETWEEN :startDate AND :endDate AND active = 1")
    suspend fun getTotalInvoiceValueByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceEntity WHERE active = 1")
    suspend fun getTotalTaxValue(): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceEntity WHERE customer_id = :customerId AND active = 1")
    suspend fun getTotalTaxValueByCustomer(customerId: String): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceEntity WHERE invoice_date BETWEEN :startDate AND :endDate AND active = 1")
    suspend fun getTotalTaxValueByDateRange(startDate: String, endDate: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoice: InvoiceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoices: List<InvoiceEntity>)

    @Update
    suspend fun update(invoice: InvoiceEntity)

    @Query("UPDATE invoiceEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    /**
     * Records the external reference (the Tally voucher master id) after an app→Tally push and marks
     * the row unsynced so the captured `ref_id` round-trips to the backend on the next push. Presence
     * of a non-blank `ref_id` is what makes the Tally push idempotent (the invoice is skipped next run).
     */
    @Query("UPDATE invoiceEntity SET ref_id = :refId, synced = 0 WHERE id = :id")
    suspend fun setTallyRef(id: String, refId: String)

    @Query("UPDATE invoiceEntity SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    /** Repairs a blank `customer_name` (e.g. an invoice whose Customer object was only a stub with
     * `uid` set) by backfilling from the customer table. */
    @Query("UPDATE invoiceEntity SET customer_name = :customerName WHERE id = :id")
    suspend fun setCustomerName(id: String, customerName: String)

    @Query("UPDATE invoiceEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("DELETE FROM invoiceEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invoiceEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC")
    fun getAllInvoicesPagingSource(): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE customer_id = :customerId AND active = 1 ORDER BY invoice_date DESC")
    fun getInvoicesByCustomerPagingSource(customerId: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE invoice_number LIKE '%' || :searchText || '%' AND active = 1 ORDER BY invoice_date DESC")
    fun getInvoicesBySearchPagingSource(searchText: String): PagingSource<Int, InvoiceEntity>

    // Invoice list v2 (docs/design/order-list-and-view, mirrored): one search box matches the
    // number and the buyer/seller names; status / from-order / offline predicates compose on top.
    @Query(
        """
        SELECT * FROM invoiceEntity
        WHERE active = 1
          AND (:searchText = '' OR invoice_number LIKE '%' || :searchText || '%'
               OR customer_name LIKE '%' || :searchText || '%')
          AND (:status = '' OR status = :status)
          AND (:fromOrderOnly = 0 OR (order_ref_id IS NOT NULL AND order_ref_id != ''))
          AND (:unsyncedOnly = 0 OR synced = 0)
        ORDER BY invoice_date DESC
        """
    )
    fun getInvoicesFilteredPagingSource(
        searchText: String,
        status: String,
        fromOrderOnly: Int,
        unsyncedOnly: Int,
    ): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE status = :status AND active = 1 ORDER BY invoice_date DESC")
    fun getInvoicesByStatusPagingSource(status: String): PagingSource<Int, InvoiceEntity>

    @Transaction
    suspend fun updateInvoices(invoices: List<InvoiceEntity>) {
        insertAll(invoices)
    }
}