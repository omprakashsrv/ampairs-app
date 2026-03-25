package com.ampairs.product.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.SubCategoryEntity
import com.ampairs.product.db.model.SubCategoryModel

@Dao
interface SubCategoryDao {

    @Query("SELECT * FROM subCategoryEntity WHERE id = :id")
    suspend fun subCategoryById(id: String): SubCategoryEntity?

    @Transaction
    @Query("SELECT * FROM subCategoryEntity ORDER BY name ASC, active DESC")
    suspend fun getSubCategories(): List<SubCategoryModel>

    @Query("SELECT * FROM subCategoryEntity WHERE synced = 0")
    suspend fun unSyncedSubCategories(): List<SubCategoryEntity>

    @Query("SELECT * FROM subCategoryEntity WHERE active = 1 ORDER BY name ASC")
    suspend fun getActiveSubCategories(): List<SubCategoryEntity>

    @Query("SELECT * FROM subCategoryEntity WHERE name LIKE '%' || :searchText || '%' ORDER BY name ASC")
    suspend fun getSubCategoriesByName(searchText: String): List<SubCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subCategory: SubCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subCategories: List<SubCategoryEntity>)

    @Update
    suspend fun update(subCategory: SubCategoryEntity)

    @Query("DELETE FROM subCategoryEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM subCategoryEntity")
    suspend fun deleteAll()

    @Query("UPDATE subCategoryEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE subCategoryEntity SET soft_deleted = 1 WHERE id = :id")
    suspend fun softDelete(id: String)

    @Query("UPDATE subCategoryEntity SET active = :active WHERE id = :id")
    suspend fun updateActiveStatus(id: String, active: Int)

    @Transaction
    suspend fun insertSubCategories(subCategories: List<SubCategoryEntity>) {
        insertAll(subCategories)
    }
}