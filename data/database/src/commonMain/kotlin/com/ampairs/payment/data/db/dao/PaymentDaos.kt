package com.ampairs.payment.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.payment.data.db.entity.AdjustmentVoucherEntity
import com.ampairs.payment.data.db.entity.LedgerEntryEntity
import com.ampairs.payment.data.db.entity.PartyBalanceEntity
import com.ampairs.payment.data.db.entity.PaymentAllocationEntity
import com.ampairs.payment.data.db.entity.PaymentVoucherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PartyBalanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PartyBalanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PartyBalanceEntity>)

    @Query("SELECT * FROM party_balance WHERE party_uid = :partyUid LIMIT 1")
    suspend fun getByParty(partyUid: String): PartyBalanceEntity?

    @Query("SELECT * FROM party_balance WHERE party_uid = :partyUid LIMIT 1")
    fun observeByParty(partyUid: String): Flow<PartyBalanceEntity?>

    @Query("SELECT * FROM party_balance WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): PartyBalanceEntity?

    @Query("SELECT * FROM party_balance WHERE active = 1")
    fun observeAll(): Flow<List<PartyBalanceEntity>>

    @Query("SELECT * FROM party_balance WHERE active = 1")
    suspend fun getAll(): List<PartyBalanceEntity>

    @Query("SELECT * FROM party_balance WHERE synced = 0")
    suspend fun getUnsynced(): List<PartyBalanceEntity>

    @Query("UPDATE party_balance SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("UPDATE party_balance SET cached_closing_minor = :closingMinor, last_computed_at = :computedAt WHERE party_uid = :partyUid")
    suspend fun updateCachedClosing(partyUid: String, closingMinor: Long, computedAt: String)

    @Query("DELETE FROM party_balance WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

@Dao
interface LedgerEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LedgerEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<LedgerEntryEntity>)

    @Query("SELECT * FROM ledger_entry WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): LedgerEntryEntity?

    /** Active entries for a party, in statement order. */
    @Query("SELECT * FROM ledger_entry WHERE party_uid = :partyUid AND active = 1 ORDER BY entry_date ASC, uid ASC")
    suspend fun getActiveByParty(partyUid: String): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entry WHERE party_uid = :partyUid AND active = 1 ORDER BY entry_date ASC, uid ASC")
    fun observeActiveByParty(partyUid: String): Flow<List<LedgerEntryEntity>>

    @Query("SELECT * FROM ledger_entry WHERE source_type = :sourceType AND source_uid = :sourceUid AND active = 1")
    suspend fun getBySource(sourceType: String, sourceUid: String): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entry WHERE active = 1")
    suspend fun getAllActive(): List<LedgerEntryEntity>

    @Query("SELECT * FROM ledger_entry WHERE synced = 0")
    suspend fun getUnsynced(): List<LedgerEntryEntity>

    @Query("UPDATE ledger_entry SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("UPDATE ledger_entry SET reversed = 1 WHERE uid = :uid")
    suspend fun markReversed(uid: String)

    @Query("DELETE FROM ledger_entry WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

@Dao
interface PaymentVoucherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PaymentVoucherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PaymentVoucherEntity>)

    @Query("SELECT * FROM payment_voucher WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): PaymentVoucherEntity?

    @Query("SELECT * FROM payment_voucher WHERE party_uid = :partyUid AND active = 1 ORDER BY voucher_date DESC")
    suspend fun getByParty(partyUid: String): List<PaymentVoucherEntity>

    @Query("SELECT * FROM payment_voucher WHERE party_uid = :partyUid AND active = 1 ORDER BY voucher_date DESC")
    fun observeByParty(partyUid: String): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_voucher WHERE clearance_status = 'PENDING' AND active = 1 ORDER BY voucher_date DESC")
    fun observePending(): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_voucher WHERE active = 1 ORDER BY voucher_date DESC")
    fun observeAll(): Flow<List<PaymentVoucherEntity>>

    @Query("SELECT * FROM payment_voucher WHERE synced = 0")
    suspend fun getUnsynced(): List<PaymentVoucherEntity>

    @Query("UPDATE payment_voucher SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM payment_voucher WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

@Dao
interface PaymentAllocationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PaymentAllocationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PaymentAllocationEntity>)

    @Query("SELECT * FROM payment_allocation WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): PaymentAllocationEntity?

    @Query("SELECT * FROM payment_allocation WHERE payment_voucher_uid = :voucherUid AND active = 1")
    suspend fun getByVoucher(voucherUid: String): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocation WHERE target_type = :targetType AND target_uid = :targetUid AND active = 1")
    suspend fun getByTarget(targetType: String, targetUid: String): List<PaymentAllocationEntity>

    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM payment_allocation WHERE target_type = :targetType AND target_uid = :targetUid AND active = 1")
    suspend fun sumAllocatedToTarget(targetType: String, targetUid: String): Long

    @Query("SELECT * FROM payment_allocation WHERE active = 1")
    suspend fun getAllActive(): List<PaymentAllocationEntity>

    @Query("SELECT * FROM payment_allocation WHERE synced = 0")
    suspend fun getUnsynced(): List<PaymentAllocationEntity>

    @Query("UPDATE payment_allocation SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM payment_allocation WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}

@Dao
interface AdjustmentVoucherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AdjustmentVoucherEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<AdjustmentVoucherEntity>)

    @Query("SELECT * FROM adjustment_voucher WHERE uid = :uid LIMIT 1")
    suspend fun getByUid(uid: String): AdjustmentVoucherEntity?

    @Query("SELECT * FROM adjustment_voucher WHERE party_uid = :partyUid AND active = 1 ORDER BY voucher_date DESC")
    suspend fun getByParty(partyUid: String): List<AdjustmentVoucherEntity>

    @Query("SELECT * FROM adjustment_voucher WHERE party_uid = :partyUid AND active = 1 ORDER BY voucher_date DESC")
    fun observeByParty(partyUid: String): Flow<List<AdjustmentVoucherEntity>>

    @Query("SELECT * FROM adjustment_voucher WHERE synced = 0")
    suspend fun getUnsynced(): List<AdjustmentVoucherEntity>

    @Query("UPDATE adjustment_voucher SET synced = 1 WHERE uid = :uid")
    suspend fun markSynced(uid: String)

    @Query("DELETE FROM adjustment_voucher WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)
}
