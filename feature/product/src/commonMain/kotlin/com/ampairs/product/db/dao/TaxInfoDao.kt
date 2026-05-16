package com.ampairs.product.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.TaxInfoEntity

@Dao
interface TaxInfoDao {

    @Query("SELECT * FROM taxInfoEntity WHERE id = :id")
    suspend fun taxInfoById(id: String): TaxInfoEntity?

    @Query("SELECT * FROM taxInfoEntity")
    suspend fun taxInfos(): List<TaxInfoEntity>

    @Query("SELECT * FROM taxInfoEntity WHERE synced = 0")
    suspend fun unSyncedTaxInfos(): List<TaxInfoEntity>

    @Query("SELECT count(*) FROM taxInfoEntity")
    suspend fun countTaxInfos(): Int

    @Query("SELECT * FROM taxInfoEntity ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun taxInfosPaging(limit: Long, offset: Long): List<TaxInfoEntity>

    @Query("SELECT count(*) FROM taxInfoEntity WHERE name LIKE ('%' || :code || '%')")
    suspend fun countTaxInfosByCode(code: String): Int

    @Query("SELECT * FROM taxInfoEntity WHERE name LIKE ('%' || :code || '%') ORDER BY name ASC LIMIT :limit OFFSET :offset")
    suspend fun taxInfosByCode(code: String, limit: Long, offset: Long): List<TaxInfoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taxInfo: TaxInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(taxInfos: List<TaxInfoEntity>)

    @Update
    suspend fun update(taxInfo: TaxInfoEntity)

    @Query("DELETE FROM taxInfoEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM taxInfoEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM taxInfoEntity ORDER BY name ASC")
    fun getAllTaxInfosPagingSource(): PagingSource<Int, TaxInfoEntity>

    @Query("SELECT * FROM taxInfoEntity WHERE name LIKE ('%' || :searchText || '%') ORDER BY name ASC")
    fun getTaxInfosPagingSource(searchText: String): PagingSource<Int, TaxInfoEntity>

    @Transaction
    suspend fun updateTaxInfos(taxInfos: List<TaxInfoEntity>) {
        insertAll(taxInfos)
    }
}