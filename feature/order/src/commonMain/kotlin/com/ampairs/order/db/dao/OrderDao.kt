package com.ampairs.order.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.order.db.entity.OrderEntity
import com.ampairs.order.db.entity.OrderItemEntity
import com.ampairs.order.db.model.OrderModel

@Dao
interface OrderDao {

    @Query("SELECT * FROM orderEntity WHERE id = :id")
    suspend fun selectById(id: String): OrderEntity?

    @Transaction
    @Query("SELECT * FROM orderEntity WHERE id = :id")
    suspend fun getOrderById(id: String): OrderModel?

    @Query("SELECT * FROM orderEntity WHERE active = 1 ORDER BY order_date DESC")
    suspend fun selectAll(): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE from_customer_id = :customerId AND active = 1 ORDER BY order_date DESC")
    suspend fun getOrdersByCustomer(customerId: String): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE to_customer_id = :customerId AND active = 1 ORDER BY order_date DESC")
    suspend fun getOrdersByToCustomer(customerId: String): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE status = :status AND active = 1 ORDER BY order_date DESC")
    suspend fun getOrdersByStatus(status: String): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE order_number LIKE '%' || :searchText || '%' AND active = 1 ORDER BY order_date DESC")
    suspend fun getOrdersByNumber(searchText: String): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE from_customer_name LIKE '%' || :searchText || '%' AND active = 1 ORDER BY order_date DESC")
    suspend fun getOrdersByCustomerName(searchText: String): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE order_date BETWEEN :startDate AND :endDate AND active = 1 ORDER BY order_date DESC")
    suspend fun getOrdersByDateRange(startDate: String, endDate: String): List<OrderEntity>

    @Query("SELECT max(last_updated) FROM orderEntity")
    suspend fun getMaxLastUpdated(): Long?

    @Query("SELECT count(*) FROM orderEntity WHERE active = 1")
    suspend fun countOrders(): Int

    @Query("SELECT count(*) FROM orderEntity WHERE from_customer_id = :customerId AND active = 1")
    suspend fun countOrdersByCustomer(customerId: String): Int

    @Query("SELECT count(*) FROM orderEntity WHERE status = :status AND active = 1")
    suspend fun countOrdersByStatus(status: String): Int

    @Query("SELECT * FROM orderEntity WHERE active = 1 ORDER BY order_date DESC LIMIT :limit OFFSET :offset")
    suspend fun getOrdersPaginated(limit: Long, offset: Long): List<OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE synced = 0 AND active = 1")
    suspend fun getUnsyncedOrders(): List<OrderEntity>

    @Query("SELECT SUM(total_cost) FROM orderEntity WHERE active = 1")
    suspend fun getTotalOrderValue(): Double?

    @Query("SELECT SUM(total_cost) FROM orderEntity WHERE from_customer_id = :customerId AND active = 1")
    suspend fun getTotalOrderValueByCustomer(customerId: String): Double?

    @Query("SELECT SUM(total_cost) FROM orderEntity WHERE order_date BETWEEN :startDate AND :endDate AND active = 1")
    suspend fun getTotalOrderValueByDateRange(startDate: String, endDate: String): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(orders: List<OrderEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(orders: List<OrderItemEntity>)

    @Update
    suspend fun update(order: OrderEntity)

    @Query("UPDATE orderEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE orderEntity SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE orderEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("DELETE FROM orderEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM orderEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM orderEntity WHERE active = 1 ORDER BY order_date DESC")
    fun getAllOrdersPagingSource(): PagingSource<Int, OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE from_customer_id = :customerId AND active = 1 ORDER BY order_date DESC")
    fun getOrdersByCustomerPagingSource(customerId: String): PagingSource<Int, OrderEntity>

    @Query("SELECT * FROM orderEntity WHERE order_number LIKE '%' || :searchText || '%' AND active = 1 ORDER BY order_date DESC")
    fun getOrdersBySearchPagingSource(searchText: String): PagingSource<Int, OrderEntity>

    @Transaction
    suspend fun updateOrders(orders: List<OrderEntity>, orderItems: List<OrderItemEntity>) {
        insertAll(orders)
        insertOrderItems(orderItems)
    }

}