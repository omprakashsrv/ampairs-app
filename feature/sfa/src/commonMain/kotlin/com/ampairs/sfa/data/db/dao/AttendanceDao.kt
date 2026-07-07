package com.ampairs.sfa.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ampairs.sfa.data.db.entity.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM sfa_attendance WHERE active = 1 ORDER BY check_in_at DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM sfa_attendance WHERE id = :id")
    suspend fun getAttendanceById(id: String): AttendanceEntity?

    @Query("SELECT * FROM sfa_attendance WHERE synced = 0")
    suspend fun getUnsyncedAttendance(): List<AttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendances(attendances: List<AttendanceEntity>)

    @Query("DELETE FROM sfa_attendance WHERE id = :id")
    suspend fun hardDeleteAttendance(id: String)
}
