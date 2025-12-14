package com.ampairs.tax.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ampairs.tax.data.db.entity.TaxRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Tax Rule DAO
 */
@Dao
interface TaxRuleDao {

    @Query(
        """
        SELECT * FROM tax_rules
        WHERE is_active = 1
        ORDER BY created_at DESC
    """
    )
    fun observeTaxRules(): Flow<List<TaxRuleEntity>>

    @Query("SELECT * FROM tax_rules WHERE id = :id")
    suspend fun getById(id: String): TaxRuleEntity?

    @Query(
        """
        SELECT * FROM tax_rules
        WHERE tax_code = :taxCode
        AND jurisdiction = :jurisdiction
        AND is_active = 1
        ORDER BY created_at DESC
        LIMIT 1
    """
    )
    suspend fun getEffectiveRule(
        taxCode: String,
        jurisdiction: String
    ): TaxRuleEntity?

    @Query(
        """
        SELECT * FROM tax_rules
        WHERE tax_code = :taxCode
        AND is_active = 1
    """
    )
    fun observeRulesByCodeString(
        taxCode: String
    ): Flow<List<TaxRuleEntity>>

    @Query(
        """
        SELECT * FROM tax_rules
        WHERE tax_code_id = :taxCodeId
        AND is_active = 1
    """
    )
    fun observeRulesByTaxCode(
        taxCodeId: String
    ): Flow<List<TaxRuleEntity>>

    @Query(
        """
        SELECT * FROM tax_rules
        WHERE tax_code_id = :taxCodeId
        AND is_active = 1
    """
    )
    suspend fun getRulesByTaxCode(
        taxCodeId: String
    ): List<TaxRuleEntity>

    @Query("SELECT * FROM tax_rules WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedRules(): List<TaxRuleEntity>

    @Query("SELECT * FROM tax_rules WHERE updated_at > :modifiedAfter")
    suspend fun getModifiedAfter(modifiedAfter: Long): List<TaxRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: TaxRuleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<TaxRuleEntity>)

    @Update
    suspend fun update(rule: TaxRuleEntity)

    @Query("UPDATE tax_rules SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)

    @Query("UPDATE tax_rules SET is_active = 0 WHERE id = :id")
    suspend fun deactivate(id: String)

    @Query("DELETE FROM tax_rules WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM tax_rules")
    suspend fun deleteAll()
}
