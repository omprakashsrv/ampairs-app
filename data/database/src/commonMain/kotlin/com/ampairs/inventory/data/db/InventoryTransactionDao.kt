package com.ampairs.inventory.data.db

import androidx.paging.PagingSource
import androidx.room3.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryTransactionDao {

    @Query("SELECT * FROM inventory_transactions WHERE inventoryItemId = :itemId ORDER BY transactionDate DESC, id DESC")
    fun getMovementsByItem(itemId: String): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE inventoryItemId = :itemId ORDER BY transactionDate DESC, id DESC")
    fun pagingMovementsByItem(itemId: String): PagingSource<Int, InventoryTransactionEntity>

    @Query("SELECT * FROM inventory_transactions ORDER BY transactionDate DESC, id DESC LIMIT :limit")
    fun getRecentMovements(limit: Int): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions ORDER BY transactionDate DESC, id DESC")
    fun pagingAllMovements(): PagingSource<Int, InventoryTransactionEntity>

    /**
     * Global ledger feed with optional filters. A null bind param means "no filter on that column",
     * so the same query serves every filter combination + reference search.
     */
    @Query(
        """
        SELECT * FROM inventory_transactions
        WHERE (:type IS NULL OR transactionType = :type)
          AND (:reason IS NULL OR transactionReason = :reason)
          AND (:sourceType IS NULL OR sourceType = :sourceType)
          AND (:fromDate IS NULL OR transactionDate >= :fromDate)
          AND (:toDate IS NULL OR transactionDate <= :toDate)
          AND (
            :reference IS NULL
            OR referenceNumber LIKE '%' || :reference || '%'
            OR transactionNumber LIKE '%' || :reference || '%'
          )
        ORDER BY transactionDate DESC, id DESC
        LIMIT :limit
        """
    )
    fun getFilteredMovements(
        type: String?,
        reason: String?,
        sourceType: String?,
        fromDate: String?,
        toDate: String?,
        reference: String?,
        limit: Int,
    ): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE id = :id")
    suspend fun getMovementById(id: String): InventoryTransactionEntity?

    @Query("SELECT * FROM inventory_transactions WHERE synced = 0")
    suspend fun getUnsyncedMovements(): List<InventoryTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovement(movement: InventoryTransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<InventoryTransactionEntity>)

    @Query("DELETE FROM inventory_transactions WHERE id = :id")
    suspend fun hardDeleteMovement(id: String)

    @Query("DELETE FROM inventory_transactions")
    suspend fun clearAll()
}
