package com.ampairs.ecom.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.ecom.data.db.entity.ListedProductEntity
import com.ampairs.ecom.data.db.entity.StorefrontEntity
import com.ampairs.ecom.data.db.entity.SyncCursorEntity
import com.ampairs.ecom.data.db.entity.TaxonomyImageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorefrontDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(storefront: StorefrontEntity)

    @Query("SELECT * FROM storefront WHERE slug = :slug")
    suspend fun bySlug(slug: String): StorefrontEntity?

    @Query("SELECT * FROM storefront WHERE slug = :slug")
    fun observeBySlug(slug: String): Flow<StorefrontEntity?>
}

@Dao
interface TaxonomyImageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TaxonomyImageEntity>)

    @Query("SELECT * FROM taxonomy_image WHERE storefront_id = :storefrontId ORDER BY sort_order ASC, name ASC")
    fun observeForStorefront(storefrontId: String): Flow<List<TaxonomyImageEntity>>

    @Query("DELETE FROM taxonomy_image WHERE storefront_id = :storefrontId")
    suspend fun clearForStorefront(storefrontId: String)
}

@Dao
interface ListedProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(products: List<ListedProductEntity>)

    @Query("SELECT * FROM listed_product WHERE uid = :uid")
    suspend fun byId(uid: String): ListedProductEntity?

    @Query("SELECT * FROM listed_product WHERE uid = :uid")
    fun observeById(uid: String): Flow<ListedProductEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: ListedProductEntity)

    @Query("SELECT * FROM listed_product WHERE storefront_id = :storefrontId AND is_visible = 1 ORDER BY name ASC")
    fun observeVisible(storefrontId: String): Flow<List<ListedProductEntity>>

    @Query(
        "SELECT * FROM listed_product WHERE storefront_id = :storefrontId AND is_visible = 1 " +
            "AND (:category IS NULL OR category = :category) " +
            "AND (:brand IS NULL OR brand = :brand) " +
            "AND (:subcategory IS NULL OR subcategory = :subcategory) " +
            "ORDER BY name ASC"
    )
    fun observeFiltered(storefrontId: String, category: String?, brand: String?, subcategory: String?): Flow<List<ListedProductEntity>>

    @Query("UPDATE listed_product SET is_visible = 0 WHERE uid = :uid")
    suspend fun markHidden(uid: String)
}

@Dao
interface SyncCursorDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cursor: SyncCursorEntity)

    @Query("SELECT * FROM sync_cursor WHERE storefront_id = :storefrontId")
    suspend fun forStorefront(storefrontId: String): SyncCursorEntity?
}
