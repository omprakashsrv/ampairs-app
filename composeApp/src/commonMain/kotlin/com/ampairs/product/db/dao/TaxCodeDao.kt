package com.ampairs.product.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.product.db.entity.TaxCodeEntity

@Dao
interface TaxCodeDao {

    @Query("SELECT * FROM taxCodeEntity WHERE id = :id")
    suspend fun taxCodeById(id: String): TaxCodeEntity?

    @Query("SELECT * FROM taxCodeEntity WHERE code = :code")
    suspend fun findByCode(code: String): TaxCodeEntity?

    @Query("SELECT * FROM taxCodeEntity")
    suspend fun taxCodes(): List<TaxCodeEntity>

    @Query("SELECT * FROM taxCodeEntity WHERE synced = 0")
    suspend fun unSyncedTaxCodes(): List<TaxCodeEntity>

    @Query("SELECT count(*) FROM taxCodeEntity")
    suspend fun countTaxCodes(): Int

    @Query("SELECT * FROM taxCodeEntity ORDER BY code ASC LIMIT :limit OFFSET :offset")
    suspend fun taxCodesPaging(limit: Long, offset: Long): List<TaxCodeEntity>

    @Query("SELECT count(*) FROM taxCodeEntity WHERE code LIKE ('%' || :code || '%')")
    suspend fun countTaxCodesByCode(code: String): Int

    @Query("SELECT * FROM taxCodeEntity WHERE code LIKE ('%' || :code || '%') ORDER BY code ASC LIMIT :limit OFFSET :offset")
    suspend fun taxCodesByCode(code: String, limit: Long, offset: Long): List<TaxCodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(taxCode: TaxCodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(taxCodes: List<TaxCodeEntity>)

    @Update
    suspend fun update(taxCode: TaxCodeEntity)

    @Query("DELETE FROM taxCodeEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM taxCodeEntity")
    suspend fun deleteAll()

    // For Paging3 support
    @Query("SELECT * FROM taxCodeEntity ORDER BY code ASC")
    fun getAllTaxCodesPagingSource(): PagingSource<Int, TaxCodeEntity>

    @Query("SELECT * FROM taxCodeEntity WHERE code LIKE ('%' || :searchText || '%') ORDER BY code ASC")
    fun getTaxCodesPagingSource(searchText: String): PagingSource<Int, TaxCodeEntity>

    @Transaction
    suspend fun updateTaxCodes(taxCodes: List<TaxCodeEntity>) {
        insertAll(taxCodes)
    }
}