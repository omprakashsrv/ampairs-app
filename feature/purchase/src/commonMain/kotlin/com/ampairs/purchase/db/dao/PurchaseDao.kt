package com.ampairs.purchase.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.ampairs.purchase.db.entity.PurchaseEntity
import com.ampairs.purchase.db.entity.PurchaseItemEntity
import com.ampairs.purchase.db.model.PurchaseModel
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseDao {

    // --- Parent reads ----------------------------------------------------------------------

    /** Reactive list for the list screen — active rows newest first. */
    @Query("SELECT * FROM purchaseEntity WHERE active = 1 ORDER BY purchase_date DESC")
    fun getAllPurchases(): Flow<List<PurchaseEntity>>

    /** Reactive single parent (drives the details screen header). */
    @Query("SELECT * FROM purchaseEntity WHERE id = :purchaseId")
    fun observePurchaseById(purchaseId: String): Flow<PurchaseEntity?>

    @Query("SELECT * FROM purchaseEntity WHERE id = :id")
    suspend fun selectById(id: String): PurchaseEntity?

    @Transaction
    @Query("SELECT * FROM purchaseEntity WHERE id = :id")
    suspend fun getPurchaseById(id: String): PurchaseModel?

    @Query("SELECT * FROM purchaseEntity WHERE active = 1 ORDER BY purchase_date DESC")
    suspend fun selectAll(): List<PurchaseEntity>

    @Query("SELECT * FROM purchaseEntity WHERE status = :status AND active = 1 ORDER BY purchase_date DESC")
    suspend fun getPurchasesByStatus(status: String): List<PurchaseEntity>

    @Query("SELECT max(last_updated) FROM purchaseEntity")
    suspend fun getMaxLastUpdated(): Long?

    // Highest local PUR-#### sequence — purchase numbers are client-assigned at save.
    @Query("SELECT max(CAST(substr(purchase_number, 5) AS INTEGER)) FROM purchaseEntity WHERE purchase_number LIKE 'PUR-%'")
    suspend fun maxPurchaseSequence(): Long?

    @Query("SELECT count(*) FROM purchaseEntity WHERE active = 1")
    suspend fun countPurchases(): Int

    @Query("SELECT * FROM purchaseEntity WHERE synced = 0 AND active = 1")
    suspend fun getUnsyncedPurchases(): List<PurchaseEntity>

    /** Soft-deleted (inactive) rows that still need to be pushed (CANCELLED). */
    @Query("SELECT * FROM purchaseEntity WHERE synced = 0 AND active = 0")
    suspend fun getUnsyncedDeletedPurchases(): List<PurchaseEntity>

    // --- Item reads ------------------------------------------------------------------------

    /** Reactive item list for the details screen. */
    @Query("SELECT * FROM purchaseItemEntity WHERE purchase_id = :purchaseId AND active = 1 ORDER BY item_no ASC")
    fun observePurchaseItems(purchaseId: String): Flow<List<PurchaseItemEntity>>

    @Query("SELECT * FROM purchaseItemEntity WHERE purchase_id = :purchaseId AND active = 1 ORDER BY item_no ASC")
    suspend fun getPurchaseItems(purchaseId: String): List<PurchaseItemEntity>

    /**
     * Items belonging to a not-yet-synced parent. Item sync is parent-driven (items have no own
     * `synced` flag); the delegate sends each unsynced parent's items in-band with the parent.
     */
    @Query(
        """
        SELECT i.* FROM purchaseItemEntity i
        JOIN purchaseEntity p ON p.id = i.purchase_id
        WHERE p.synced = 0
        """
    )
    suspend fun getUnsyncedPurchaseItems(): List<PurchaseItemEntity>

    // --- Writes ----------------------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchase(purchase: PurchaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchases(purchases: List<PurchaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurchaseItems(items: List<PurchaseItemEntity>)

    @Update
    suspend fun update(purchase: PurchaseEntity)

    @Query("UPDATE purchaseEntity SET synced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("DELETE FROM purchaseEntity WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM purchaseItemEntity WHERE purchase_id = :purchaseId")
    suspend fun deletePurchaseItems(purchaseId: String)

    @Query("DELETE FROM purchaseEntity")
    suspend fun deleteAllPurchases()

    @Query("DELETE FROM purchaseItemEntity")
    suspend fun deleteAllItems()

    // --- Transactions ----------------------------------------------------------------------

    /** Save a parent and atomically replace its line items (delete existing, insert new). */
    @Transaction
    suspend fun savePurchaseWithItems(purchase: PurchaseEntity, items: List<PurchaseItemEntity>) {
        insertPurchase(purchase)
        deletePurchaseItems(purchase.id)
        insertPurchaseItems(items)
    }

    /** Bulk upsert from the server pull (parents + nested items). */
    @Transaction
    suspend fun upsertPurchases(purchases: List<PurchaseEntity>, items: List<PurchaseItemEntity>) {
        insertPurchases(purchases)
        insertPurchaseItems(items)
    }
}
