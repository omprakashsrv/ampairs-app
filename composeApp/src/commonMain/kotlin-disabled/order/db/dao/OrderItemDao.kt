package com.ampairs.order.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.order.db.entity.OrderItemEntity

@Dao
interface OrderItemDao {

    @Query("SELECT * FROM orderItemEntity WHERE id = :id")
    suspend fun selectById(id: String): OrderItemEntity?

    @Query("SELECT * FROM orderItemEntity WHERE active = 1 ORDER BY item_no ASC")
    suspend fun selectAll(): List<OrderItemEntity>

    @Query("SELECT * FROM orderItemEntity WHERE order_id = :orderId AND active = 1 ORDER BY item_no ASC")
    suspend fun getOrderItems(orderId: String): List<OrderItemEntity>

    @Query("SELECT * FROM orderItemEntity WHERE product_id = :productId AND active = 1 ORDER BY item_no ASC")
    suspend fun getItemsByProduct(productId: String): List<OrderItemEntity>

    @Query("SELECT * FROM orderItemEntity WHERE order_id IN (:orderIds) AND active = 1 ORDER BY order_id, item_no ASC")
    suspend fun getItemsByOrderIds(orderIds: List<String>): List<OrderItemEntity>

    @Query("SELECT * FROM orderItemEntity WHERE description LIKE '%' || :searchText || '%' AND active = 1 ORDER BY item_no ASC")
    suspend fun getItemsByDescription(searchText: String): List<OrderItemEntity>

    @Query("SELECT count(*) FROM orderItemEntity WHERE order_id = :orderId AND active = 1")
    suspend fun countItemsByOrder(orderId: String): Int

    @Query("SELECT count(*) FROM orderItemEntity WHERE product_id = :productId AND active = 1")
    suspend fun countItemsByProduct(productId: String): Int

    @Query("SELECT count(*) FROM orderItemEntity WHERE active = 1")
    suspend fun countAllItems(): Int

    @Query("SELECT SUM(quantity) FROM orderItemEntity WHERE order_id = :orderId AND active = 1")
    suspend fun getTotalQuantityByOrder(orderId: String): Double?

    @Query("SELECT SUM(total_cost) FROM orderItemEntity WHERE order_id = :orderId AND active = 1")
    suspend fun getTotalCostByOrder(orderId: String): Double?

    @Query("SELECT SUM(total_tax) FROM orderItemEntity WHERE order_id = :orderId AND active = 1")
    suspend fun getTotalTaxByOrder(orderId: String): Double?

    @Query("SELECT SUM(quantity) FROM orderItemEntity WHERE product_id = :productId AND active = 1")
    suspend fun getTotalQuantityByProduct(productId: String): Double?

    @Query("SELECT MAX(item_no) FROM orderItemEntity WHERE order_id = :orderId")
    suspend fun getMaxItemNumberByOrder(orderId: String): Long?

    @Query("SELECT DISTINCT tax_code FROM orderItemEntity WHERE active = 1")
    suspend fun getDistinctTaxCodes(): List<String>

    @Query("SELECT * FROM orderItemEntity WHERE order_id = :orderId AND active = 1 ORDER BY item_no ASC LIMIT :limit OFFSET :offset")
    suspend fun getOrderItemsPaginated(orderId: String, limit: Long, offset: Long): List<OrderItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(orderItem: OrderItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orderItems: List<OrderItemEntity>)

    @Update
    suspend fun update(orderItem: OrderItemEntity)

    @Query("UPDATE orderItemEntity SET quantity = :quantity, total_cost = :totalCost WHERE id = :id")
    suspend fun updateQuantityAndCost(id: String, quantity: Double, totalCost: Double)

    @Query("UPDATE orderItemEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE orderItemEntity SET soft_deleted = 1 WHERE order_id = :orderId")
    suspend fun softDeleteByOrderId(orderId: String)

    @Query("DELETE FROM orderItemEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM orderItemEntity WHERE order_id = :orderId")
    suspend fun deleteByOrderId(orderId: String)

    @Query("DELETE FROM orderItemEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM orderItemEntity WHERE order_id = :orderId AND active = 1 ORDER BY item_no ASC")
    fun getOrderItemsPagingSource(orderId: String): PagingSource<Int, OrderItemEntity>

    @Query("SELECT * FROM orderItemEntity WHERE product_id = :productId AND active = 1 ORDER BY item_no ASC")
    fun getItemsByProductPagingSource(productId: String): PagingSource<Int, OrderItemEntity>

    @Query("SELECT * FROM orderItemEntity WHERE active = 1 ORDER BY item_no ASC")
    fun getAllItemsPagingSource(): PagingSource<Int, OrderItemEntity>

    @Transaction
    suspend fun replaceOrderItems(orderId: String, items: List<OrderItemEntity>) {
        deleteByOrderId(orderId)
        insertAll(items)
    }

    @Transaction
    suspend fun updateOrderItems(items: List<OrderItemEntity>) {
        insertAll(items)
    }
}