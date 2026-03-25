package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.CategoryEntity
import com.ampairs.product.db.model.CategoryModel

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categoryEntity WHERE id = :id")
    suspend fun categoryById(id: String): CategoryEntity?

    @Transaction
    @Query("SELECT * FROM categoryEntity ORDER BY name ASC, active DESC")
    suspend fun getCategories(): List<CategoryModel>

    @Query("SELECT * FROM categoryEntity WHERE id IN (:ids) ORDER BY name ASC, active DESC")
    suspend fun getCategoriesByIds(ids: List<String>): List<CategoryEntity>

    @Query("SELECT * FROM categoryEntity WHERE synced = 0")
    suspend fun unSyncedCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categoryEntity WHERE active = 1 ORDER BY name ASC")
    suspend fun getActiveCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categoryEntity WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getCategoriesByName(searchText: String): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Update
    suspend fun update(category: CategoryEntity)

    @Query("DELETE FROM categoryEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM categoryEntity")
    suspend fun deleteAll()

    @Query("UPDATE categoryEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE categoryEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE categoryEntity SET active = :active WHERE id = :id")
    suspend fun updateActiveStatus(id: String, active: Int)

    @Transaction
    suspend fun insertCategories(categories: List<CategoryEntity>) {
        insertAll(categories)
    }
}