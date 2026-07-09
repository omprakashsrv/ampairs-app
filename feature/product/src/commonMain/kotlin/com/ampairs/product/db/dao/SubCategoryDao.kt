package com.ampairs.product.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Update
import com.ampairs.product.db.entity.SubCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubCategoryDao {

    @Query("SELECT * FROM subCategoryEntity WHERE id = :id")
    suspend fun subCategoryById(id: String): SubCategoryEntity?

    @Query("SELECT * FROM subCategoryEntity ORDER BY name ASC, active DESC")
    suspend fun getSubCategories(): List<SubCategoryEntity>

    @Query("SELECT * FROM subCategoryEntity ORDER BY name ASC, active DESC")
    fun observeSubCategories(): Flow<List<SubCategoryEntity>>

    @Query("SELECT * FROM subCategoryEntity WHERE synced = 0")
    suspend fun unSyncedSubCategories(): List<SubCategoryEntity>

    @Query("SELECT * FROM subCategoryEntity WHERE active = 1 ORDER BY name ASC")
    suspend fun getActiveSubCategories(): List<SubCategoryEntity>

    @Query("SELECT * FROM subCategoryEntity ORDER BY name ASC, active DESC")
    suspend fun getAllSubCategoryEntities(): List<SubCategoryEntity>

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
