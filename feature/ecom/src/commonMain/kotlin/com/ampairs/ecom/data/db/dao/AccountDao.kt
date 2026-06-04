package com.ampairs.ecom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AddressDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(address: CustomerAddressEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(addresses: List<CustomerAddressEntity>)

    @Query("SELECT * FROM customer_address WHERE active = 1 ORDER BY is_default DESC, label ASC")
    fun observeAll(): Flow<List<CustomerAddressEntity>>

    @Query("SELECT * FROM customer_address WHERE synced = 0")
    suspend fun unsynced(): List<CustomerAddressEntity>

    @Query("SELECT * FROM customer_address WHERE uid = :uid")
    suspend fun byId(uid: String): CustomerAddressEntity?

    @Query("DELETE FROM customer_address WHERE uid = :uid")
    suspend fun hardDelete(uid: String)
}

@Dao
interface EcomOrderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOrder(order: EcomOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLineItems(items: List<EcomOrderLineItemEntity>)

    @Query("SELECT * FROM ecom_order WHERE storefront_id = :storefrontId ORDER BY placed_at DESC")
    fun observeForStorefront(storefrontId: String): Flow<List<EcomOrderEntity>>

    @Query("SELECT * FROM ecom_order WHERE ecom_order_ref = :ref")
    fun observeByRef(ref: String): Flow<EcomOrderEntity?>

    @Query("SELECT * FROM ecom_order_line_item WHERE order_uid = :orderUid")
    fun observeLineItems(orderUid: String): Flow<List<EcomOrderLineItemEntity>>

    @Transaction
    suspend fun upsertWithItems(order: EcomOrderEntity, items: List<EcomOrderLineItemEntity>) {
        upsertOrder(order)
        upsertLineItems(items)
    }
}
