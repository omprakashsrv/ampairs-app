package com.ampairs.inventory.data.db

import androidx.paging.PagingSource
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryTransactionDao {

    @Query("SELECT * FROM inventory_transactions WHERE inventoryItemId = :itemId ORDER BY transactionDate DESC, id DESC")
    fun getMovementsByItem(itemId: String): Flow<List<InventoryTransactionEntity>>

    @Query("SELECT * FROM inventory_transactions WHERE inventoryItemId = :itemId ORDER BY transactionDate DESC, id DESC")
    fun pagingMovementsByItem(itemId: String): PagingSource<Int, InventoryTransactionEntity>

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
