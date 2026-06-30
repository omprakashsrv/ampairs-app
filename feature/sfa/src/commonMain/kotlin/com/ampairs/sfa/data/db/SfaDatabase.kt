package com.ampairs.sfa.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.sfa.data.db.dao.AttendanceDao
import com.ampairs.sfa.data.db.dao.BeatDao
import com.ampairs.sfa.data.db.dao.VisitDao
import com.ampairs.sfa.data.db.entity.AttendanceEntity
import com.ampairs.sfa.data.db.entity.BeatEntity
import com.ampairs.sfa.data.db.entity.VisitEntity

/**
 * Workspace-isolated Room database for the field-sales (SFA) module.
 * Version 1 — beats, visits, attendance.
 */
@Database(
    entities = [
        BeatEntity::class,
        VisitEntity::class,
        AttendanceEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(SfaDatabaseConstructor::class)
abstract class SfaDatabase : RoomDatabase() {
    abstract fun beatDao(): BeatDao
    abstract fun visitDao(): VisitDao
    abstract fun attendanceDao(): AttendanceDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SfaDatabaseConstructor : RoomDatabaseConstructor<SfaDatabase> {
    override fun initialize(): SfaDatabase
}
