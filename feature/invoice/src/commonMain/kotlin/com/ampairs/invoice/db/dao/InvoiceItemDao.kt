package com.ampairs.invoice.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.invoice.db.entity.InvoiceItemEntity

@Dao
interface InvoiceItemDao {

    @Query("SELECT * FROM invoiceItemEntity WHERE id = :id")
    suspend fun selectById(id: String): InvoiceItemEntity?

    @Query("SELECT * FROM invoiceItemEntity WHERE active = 1 ORDER BY item_no ASC")
    suspend fun selectAll(): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1 ORDER BY item_no ASC")
    suspend fun getInvoiceItems(invoiceId: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE product_id = :productId AND active = 1 ORDER BY item_no ASC")
    suspend fun getItemsByProduct(productId: String): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE invoice_id IN (:invoiceIds) AND active = 1 ORDER BY invoice_id, item_no ASC")
    suspend fun getItemsByInvoiceIds(invoiceIds: List<String>): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE description LIKE '%' || :searchText || '%' AND active = 1 ORDER BY item_no ASC")
    suspend fun getItemsByDescription(searchText: String): List<InvoiceItemEntity>

    @Query("SELECT count(*) FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1")
    suspend fun countItemsByInvoice(invoiceId: String): Int

    @Query("SELECT count(*) FROM invoiceItemEntity WHERE product_id = :productId AND active = 1")
    suspend fun countItemsByProduct(productId: String): Int

    @Query("SELECT count(*) FROM invoiceItemEntity WHERE active = 1")
    suspend fun countAllItems(): Int

    @Query("SELECT SUM(quantity) FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1")
    suspend fun getTotalQuantityByInvoice(invoiceId: String): Double?

    @Query("SELECT SUM(total_cost) FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1")
    suspend fun getTotalCostByInvoice(invoiceId: String): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1")
    suspend fun getTotalTaxByInvoice(invoiceId: String): Double?

    @Query("SELECT SUM(base_price) FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1")
    suspend fun getTotalBasePriceByInvoice(invoiceId: String): Double?

    @Query("SELECT SUM(quantity) FROM invoiceItemEntity WHERE product_id = :productId AND active = 1")
    suspend fun getTotalQuantityByProduct(productId: String): Double?

    @Query("SELECT SUM(total_cost) FROM invoiceItemEntity WHERE product_id = :productId AND active = 1")
    suspend fun getTotalRevenueByProduct(productId: String): Double?

    @Query("SELECT MAX(item_no) FROM invoiceItemEntity WHERE invoice_id = :invoiceId")
    suspend fun getMaxItemNumberByInvoice(invoiceId: String): Long?

    @Query("SELECT DISTINCT tax_code FROM invoiceItemEntity WHERE active = 1")
    suspend fun getDistinctTaxCodes(): List<String>

    @Query("SELECT DISTINCT product_id FROM invoiceItemEntity WHERE active = 1")
    suspend fun getDistinctProducts(): List<String>

    @Query("SELECT * FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1 ORDER BY item_no ASC LIMIT :limit OFFSET :offset")
    suspend fun getInvoiceItemsPaginated(invoiceId: String, limit: Long, offset: Long): List<InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE tax_code = :taxCode AND active = 1 ORDER BY item_no ASC")
    suspend fun getItemsByTaxCode(taxCode: String): List<InvoiceItemEntity>

    @Query("SELECT SUM(total_cost) FROM invoiceItemEntity WHERE tax_code = :taxCode AND active = 1")
    suspend fun getTotalCostByTaxCode(taxCode: String): Double?

    @Query("SELECT SUM(total_tax) FROM invoiceItemEntity WHERE tax_code = :taxCode AND active = 1")
    suspend fun getTotalTaxByTaxCode(taxCode: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(invoiceItem: InvoiceItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(invoiceItems: List<InvoiceItemEntity>)

    @Update
    suspend fun update(invoiceItem: InvoiceItemEntity)

    @Query("UPDATE invoiceItemEntity SET quantity = :quantity, total_cost = :totalCost WHERE id = :id")
    suspend fun updateQuantityAndCost(id: String, quantity: Double, totalCost: Double)

    @Query("UPDATE invoiceItemEntity SET selling_price = :sellingPrice, total_cost = :totalCost WHERE id = :id")
    suspend fun updateSellingPriceAndCost(id: String, sellingPrice: Double, totalCost: Double)

    @Query("UPDATE invoiceItemEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE invoiceItemEntity SET soft_deleted = 1 WHERE invoice_id = :invoiceId")
    suspend fun softDeleteByInvoiceId(invoiceId: String)

    @Query("DELETE FROM invoiceItemEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM invoiceItemEntity WHERE invoice_id = :invoiceId")
    suspend fun deleteByInvoiceId(invoiceId: String)

    @Query("DELETE FROM invoiceItemEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM invoiceItemEntity WHERE invoice_id = :invoiceId AND active = 1 ORDER BY item_no ASC")
    fun getInvoiceItemsPagingSource(invoiceId: String): PagingSource<Int, InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE product_id = :productId AND active = 1 ORDER BY item_no ASC")
    fun getItemsByProductPagingSource(productId: String): PagingSource<Int, InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE active = 1 ORDER BY item_no ASC")
    fun getAllItemsPagingSource(): PagingSource<Int, InvoiceItemEntity>

    @Query("SELECT * FROM invoiceItemEntity WHERE tax_code = :taxCode AND active = 1 ORDER BY item_no ASC")
    fun getItemsByTaxCodePagingSource(taxCode: String): PagingSource<Int, InvoiceItemEntity>

    @Transaction
    suspend fun replaceInvoiceItems(invoiceId: String, items: List<InvoiceItemEntity>) {
        deleteByInvoiceId(invoiceId)
        insertAll(items)
    }

    @Transaction
    suspend fun updateInvoiceItems(items: List<InvoiceItemEntity>) {
        insertAll(items)
    }
}