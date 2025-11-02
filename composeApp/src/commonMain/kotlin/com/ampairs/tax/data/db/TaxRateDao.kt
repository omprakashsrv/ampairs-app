package com.ampairs.tax.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaxRateDao {

    @Query("SELECT * FROM tax_rates WHERE is_active = 1 ORDER BY effective_from DESC")
    fun getAllActiveTaxRates(): Flow<List<TaxRateEntity>>

    @Query("SELECT * FROM tax_rates WHERE id = :id")
    suspend fun getTaxRateById(id: String): TaxRateEntity?

    @Query("""
        SELECT * FROM tax_rates
        WHERE hsn_code = :hsnCode
        AND business_type = :businessType
        AND effective_from <= :effectiveDate
        AND (effective_to IS NULL OR effective_to >= :effectiveDate)
        AND is_active = 1
        ORDER BY effective_from DESC
        LIMIT 1
    """)
    suspend fun getEffectiveTaxRate(
        hsnCode: String,
        businessType: String,
        effectiveDate: Long
    ): TaxRateEntity?

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT tr.*, hc.description as hsn_description
        FROM tax_rates tr
        LEFT JOIN hsn_codes hc ON tr.hsn_code = hc.hsn_code
        WHERE tr.hsn_code = :hsnCode
        AND tr.is_active = 1
        ORDER BY tr.effective_from DESC
    """)
    suspend fun getTaxRatesByHsnCode(hsnCode: String): List<TaxRateEntity>

    @Query("""
        SELECT * FROM tax_rates
        WHERE business_type = :businessType
        AND is_active = 1
        ORDER BY hsn_code ASC, effective_from DESC
    """)
    suspend fun getTaxRatesByBusinessType(businessType: String): List<TaxRateEntity>

    @Query("""
        SELECT * FROM tax_rates
        WHERE tax_type = :taxType
        AND is_active = 1
        ORDER BY hsn_code ASC, effective_from DESC
    """)
    suspend fun getTaxRatesByType(taxType: String): List<TaxRateEntity>

    @Query("""
        SELECT * FROM tax_rates
        WHERE effective_from <= :date
        AND (effective_to IS NULL OR effective_to >= :date)
        AND is_active = 1
        ORDER BY hsn_code ASC
    """)
    suspend fun getTaxRatesEffectiveOn(date: Long): List<TaxRateEntity>

    @Query("""
        SELECT DISTINCT hsn_code FROM tax_rates
        WHERE rate_percentage > :minRate
        AND is_active = 1
        ORDER BY hsn_code ASC
    """)
    suspend fun getHsnCodesWithRateAbove(minRate: Double): List<String>

    @Query("""
        SELECT * FROM tax_rates
        WHERE cess_rate IS NOT NULL
        AND cess_rate > 0
        AND is_active = 1
        ORDER BY hsn_code ASC, effective_from DESC
    """)
    suspend fun getTaxRatesWithCess(): List<TaxRateEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxRate(taxRate: TaxRateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaxRates(taxRates: List<TaxRateEntity>)

    @Update
    suspend fun updateTaxRate(taxRate: TaxRateEntity)

    @Delete
    suspend fun deleteTaxRate(taxRate: TaxRateEntity)

    @Query("DELETE FROM tax_rates WHERE id = :id")
    suspend fun deleteTaxRateById(id: String)

    @Query("UPDATE tax_rates SET is_active = 0 WHERE id = :id")
    suspend fun deactivateTaxRate(id: String)

    @Query("UPDATE tax_rates SET is_active = 1 WHERE id = :id")
    suspend fun activateTaxRate(id: String)

    @Query("""
        UPDATE tax_rates SET effective_to = :effectiveTo
        WHERE hsn_code = :hsnCode
        AND business_type = :businessType
        AND effective_to IS NULL
        AND id != :excludeId
    """)
    suspend fun closeOpenTaxRates(
        hsnCode: String,
        businessType: String,
        effectiveTo: Long,
        excludeId: String
    )

    @Query("SELECT COUNT(*) FROM tax_rates WHERE is_active = 1")
    suspend fun getActiveTaxRateCount(): Int

    @Query("SELECT * FROM tax_rates WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedTaxRates(): List<TaxRateEntity>

    @Query("UPDATE tax_rates SET sync_status = :status, last_sync = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String, timestamp: Long)

    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT tr.*, hc.description as hsn_description
        FROM tax_rates tr
        LEFT JOIN hsn_codes hc ON tr.hsn_code = hc.hsn_code
        WHERE (:hsnCode IS NULL OR tr.hsn_code LIKE '%' || :hsnCode || '%')
        AND (:businessType IS NULL OR tr.business_type = :businessType)
        AND (:taxType IS NULL OR tr.tax_type = :taxType)
        AND (:activeOnly = 0 OR tr.is_active = 1)
        ORDER BY tr.hsn_code ASC, tr.effective_from DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun searchTaxRates(
        hsnCode: String? = null,
        businessType: String? = null,
        taxType: String? = null,
        activeOnly: Boolean = true,
        limit: Int = 50,
        offset: Int = 0
    ): List<TaxRateEntity>
}