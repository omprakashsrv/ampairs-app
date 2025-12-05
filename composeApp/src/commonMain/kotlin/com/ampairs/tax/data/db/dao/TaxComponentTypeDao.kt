package com.ampairs.tax.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.tax.data.db.entity.TaxComponentTypeEntity
import kotlinx.coroutines.flow.Flow

/**
 * Tax Component Type DAO
 */
@Dao
interface TaxComponentTypeDao {

    @Query("SELECT * FROM tax_component_types WHERE is_active = 1 ORDER BY sort_order ASC")
    fun observeAllComponentTypes(): Flow<List<TaxComponentTypeEntity>>

    @Query("SELECT * FROM tax_component_types WHERE country_code = :countryCode AND is_active = 1 ORDER BY sort_order ASC")
    fun observeComponentTypesByCountry(countryCode: String): Flow<List<TaxComponentTypeEntity>>

    @Query("SELECT * FROM tax_component_types WHERE id = :id")
    suspend fun getById(id: String): TaxComponentTypeEntity?

    @Query("SELECT * FROM tax_component_types WHERE country_code = :countryCode AND component_code = :componentCode AND is_active = 1")
    suspend fun getByCode(countryCode: String, componentCode: String): TaxComponentTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(componentType: TaxComponentTypeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(componentTypes: List<TaxComponentTypeEntity>)

    @Query("DELETE FROM tax_component_types WHERE country_code = :countryCode")
    suspend fun deleteByCountry(countryCode: String)
}
