package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.LeaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaveDao {
    @Query("SELECT * FROM sfa_leaves WHERE active = 1 ORDER BY leave_date DESC")
    fun getAllLeaves(): Flow<List<LeaveEntity>>

    @Query("SELECT * FROM sfa_leaves WHERE id = :id")
    suspend fun getLeaveById(id: String): LeaveEntity?

    @Query("SELECT * FROM sfa_leaves WHERE synced = 0")
    suspend fun getUnsyncedLeaves(): List<LeaveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeave(row: LeaveEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeaves(rows: List<LeaveEntity>)

    @Query("DELETE FROM sfa_leaves WHERE id = :id")
    suspend fun hardDeleteLeave(id: String)
}
