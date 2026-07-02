package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.product.db.entity.ProductStandardCostEntity

@Dao
interface ProductStandardCostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cost: ProductStandardCostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(costs: List<ProductStandardCostEntity>)

    /** All active cost versions for a product (every variant + base) — the resolver filters in memory. */
    @Query("SELECT * FROM product_standard_cost WHERE product_id = :productId AND active = 1")
    suspend fun getByProductId(productId: String): List<ProductStandardCostEntity>

    @Query("SELECT * FROM product_standard_cost")
    suspend fun getAll(): List<ProductStandardCostEntity>

    /** Unsynced rows (active upserts AND soft-deletes) for the bulk push. */
    @Query("SELECT * FROM product_standard_cost WHERE synced = 0")
    suspend fun getUnsynced(): List<ProductStandardCostEntity>

    @Query("UPDATE product_standard_cost SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM product_standard_cost WHERE id = :id")
    suspend fun deleteByUid(id: String)
}
