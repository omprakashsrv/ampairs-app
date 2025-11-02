package com.ampairs.invoice.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.invoice.db.entity.InvoiceEntity

@Dao
interface InvoiceDao {

    @Query("SELECT * FROM invoiceEntity WHERE id = :id")
    suspend fun selectById(id: String): InvoiceEntity?

    @Query("SELECT * FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC")
    suspend fun selectAll(): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE from_customer_id = :customerId AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByCustomer(customerId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE to_customer_id = :customerId AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByToCustomer(customerId: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE status = :status AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByStatus(status: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE invoice_number LIKE '%' || :searchText || '%' AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByNumber(searchText: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE from_customer_name LIKE '%' || :searchText || '%' AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByCustomerName(searchText: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE invoice_date BETWEEN :startDate AND :endDate AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByDateRange(startDate: String, endDate: String): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE order_ref_id = :orderRefId AND active = 1 ORDER BY invoice_date DESC")
    suspend fun getInvoicesByOrderRef(orderRefId: String): List<InvoiceEntity>

    @Query("SELECT max(last_updated) FROM invoiceEntity")
    suspend fun getMaxLastUpdated(): Long?

    @Query("SELECT count(*) FROM invoiceEntity WHERE active = 1")
    suspend fun countInvoices(): Int

    @Query("SELECT count(*) FROM invoiceEntity WHERE from_customer_id = :customerId AND active = 1")
    suspend fun countInvoicesByCustomer(customerId: String): Int

    @Query("SELECT count(*) FROM invoiceEntity WHERE status = :status AND active = 1")
    suspend fun countInvoicesByStatus(status: String): Int

    @Query("SELECT * FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC LIMIT :limit OFFSET :offset")
    suspend fun getInvoicesPaginated(limit: Long, offset: Long): List<InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE synced = 0 AND active = 1")
    suspend fun getUnsyncedInvoices(): List<InvoiceEntity>

    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE active = 1")
    suspend fun getTotalInvoiceValue(): Double?

    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE from_customer_id = :customerId AND active = 1")
    suspend fun getTotalInvoiceValueByCustomer(customerId: String): Double?

    @Query("SELECT SUM(total_cost) FROM invoiceEntity WHERE invoice_date BETWEEN :startDate AND :endDate AND active = 1")
    suspend fun getTotalInvoiceValueByDateRange(startDate: String, endDate: String): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceEntity WHERE active = 1")
    suspend fun getTotalTaxValue(): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceEntity WHERE from_customer_id = :customerId AND active = 1")
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

    @Query("UPDATE invoiceEntity SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE invoiceEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("DELETE FROM invoiceEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invoiceEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM invoiceEntity WHERE active = 1 ORDER BY invoice_date DESC")
    fun getAllInvoicesPagingSource(): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE from_customer_id = :customerId AND active = 1 ORDER BY invoice_date DESC")
    fun getInvoicesByCustomerPagingSource(customerId: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE invoice_number LIKE '%' || :searchText || '%' AND active = 1 ORDER BY invoice_date DESC")
    fun getInvoicesBySearchPagingSource(searchText: String): PagingSource<Int, InvoiceEntity>

    @Query("SELECT * FROM invoiceEntity WHERE status = :status AND active = 1 ORDER BY invoice_date DESC")
    fun getInvoicesByStatusPagingSource(status: String): PagingSource<Int, InvoiceEntity>

    @Transaction
    suspend fun updateInvoices(invoices: List<InvoiceEntity>) {
        insertAll(invoices)
    }
}