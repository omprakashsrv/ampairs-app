package com.ampairs.sfa.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.sfa.data.db.dao.AttendanceDao
import com.ampairs.sfa.data.db.dao.BeatDao
import com.ampairs.sfa.data.db.dao.BeatOutletDao
import com.ampairs.sfa.data.db.dao.FieldOrderDao
import com.ampairs.sfa.data.db.dao.JourneyPlanDao
import com.ampairs.sfa.data.db.dao.LeaveDao
import com.ampairs.sfa.data.db.dao.PlannedVisitDao
import com.ampairs.sfa.data.db.dao.VisitDao
import com.ampairs.sfa.data.db.dao.VisitSurveyResponseDao
import com.ampairs.sfa.data.db.entity.AttendanceEntity
import com.ampairs.sfa.data.db.entity.BeatEntity
import com.ampairs.sfa.data.db.entity.BeatOutletEntity
import com.ampairs.sfa.data.db.entity.FieldOrderEntity
import com.ampairs.sfa.data.db.entity.JourneyPlanEntity
import com.ampairs.sfa.data.db.entity.LeaveEntity
import com.ampairs.sfa.data.db.entity.PlannedVisitEntity
import com.ampairs.sfa.data.db.entity.VisitEntity
import com.ampairs.sfa.data.db.entity.VisitSurveyResponseEntity

/**
 * Workspace-isolated Room database for the field-sales (SFA) module.
 * Version 1 — beats, visits, attendance.
 */
@Database(
    entities = [
        BeatEntity::class,
        BeatOutletEntity::class,
        JourneyPlanEntity::class,
        PlannedVisitEntity::class,
        VisitEntity::class,
        AttendanceEntity::class,
        FieldOrderEntity::class,
        LeaveEntity::class,
        VisitSurveyResponseEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(SfaDatabaseConstructor::class)
abstract class SfaDatabase : RoomDatabase() {
    abstract fun beatDao(): BeatDao
    abstract fun beatOutletDao(): BeatOutletDao
    abstract fun journeyPlanDao(): JourneyPlanDao
    abstract fun plannedVisitDao(): PlannedVisitDao
    abstract fun visitDao(): VisitDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun fieldOrderDao(): FieldOrderDao
    abstract fun leaveDao(): LeaveDao
    abstract fun visitSurveyResponseDao(): VisitSurveyResponseDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SfaDatabaseConstructor : RoomDatabaseConstructor<SfaDatabase> {
    override fun initialize(): SfaDatabase
}
