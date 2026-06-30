package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.FieldOrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FieldOrderDao {
    @Query("SELECT * FROM sfa_field_orders WHERE active = 1")
    fun getAllFieldOrders(): Flow<List<FieldOrderEntity>>

    @Query("SELECT * FROM sfa_field_orders WHERE id = :id")
    suspend fun getFieldOrderById(id: String): FieldOrderEntity?

    @Query("SELECT * FROM sfa_field_orders WHERE synced = 0")
    suspend fun getUnsyncedFieldOrders(): List<FieldOrderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldOrder(row: FieldOrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFieldOrders(rows: List<FieldOrderEntity>)

    @Query("DELETE FROM sfa_field_orders WHERE id = :id")
    suspend fun hardDeleteFieldOrder(id: String)
}
