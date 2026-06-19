package com.ampairs.ecom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ampairs.ecom.data.db.entity.CartEntity
import com.ampairs.ecom.data.db.entity.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCart(cart: CartEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<CartItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: CartItemEntity)

    @Query("SELECT * FROM cart WHERE storefront_id = :storefrontId LIMIT 1")
    suspend fun cartForStorefront(storefrontId: String): CartEntity?

    @Query("SELECT * FROM cart WHERE storefront_id = :storefrontId LIMIT 1")
    fun observeCartForStorefront(storefrontId: String): Flow<CartEntity?>

    @Query("SELECT * FROM cart_item WHERE cart_id = :cartId")
    fun observeItems(cartId: String): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_item WHERE cart_id = :cartId")
    suspend fun itemsForCart(cartId: String): List<CartItemEntity>

    @Query("SELECT * FROM cart_item WHERE cart_id = :cartId AND listed_product_id = :listedProductId LIMIT 1")
    suspend fun itemForProduct(cartId: String, listedProductId: String): CartItemEntity?

    @Query("DELETE FROM cart_item WHERE cart_id = :cartId AND listed_product_id = :listedProductId")
    suspend fun deleteItemByProduct(cartId: String, listedProductId: String)

    @Query("DELETE FROM cart_item WHERE uid = :uid")
    suspend fun deleteItemByUid(uid: String)

    @Query("DELETE FROM cart_item WHERE cart_id = :cartId")
    suspend fun clearItems(cartId: String)

    /** Replace the local mirror with the authoritative server cart contents. */
    @Transaction
    suspend fun replaceItems(cartId: String, items: List<CartItemEntity>) {
        clearItems(cartId)
        upsertItems(items)
    }
}
