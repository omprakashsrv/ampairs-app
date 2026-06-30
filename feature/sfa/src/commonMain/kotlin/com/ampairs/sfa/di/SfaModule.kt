package com.ampairs.sfa.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.sfa.data.db.SfaDatabase
import com.ampairs.sfa.data.db.dao.AttendanceDao
import com.ampairs.sfa.data.db.dao.BeatDao
import com.ampairs.sfa.data.db.dao.BeatOutletDao
import com.ampairs.sfa.data.db.dao.JourneyPlanDao
import com.ampairs.sfa.data.db.dao.FieldOrderDao
import com.ampairs.sfa.data.db.dao.LeaveDao
import com.ampairs.sfa.data.db.dao.PlannedVisitDao
import com.ampairs.sfa.data.db.dao.VisitSurveyResponseDao
import com.ampairs.sfa.data.db.dao.VisitDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface SfaDaoModule {
    companion object {
        @Provides
        fun provideBeatDao(db: SfaDatabase): BeatDao = db.beatDao()

        @Provides
        fun provideVisitDao(db: SfaDatabase): VisitDao = db.visitDao()

        @Provides
        fun provideAttendanceDao(db: SfaDatabase): AttendanceDao = db.attendanceDao()

        @Provides
        fun provideBeatOutletDao(db: SfaDatabase): BeatOutletDao = db.beatOutletDao()

        @Provides
        fun provideJourneyPlanDao(db: SfaDatabase): JourneyPlanDao = db.journeyPlanDao()

        @Provides
        fun providePlannedVisitDao(db: SfaDatabase): PlannedVisitDao = db.plannedVisitDao()

        @Provides
        fun provideFieldOrderDao(db: SfaDatabase): FieldOrderDao = db.fieldOrderDao()

        @Provides
        fun provideLeaveDao(db: SfaDatabase): LeaveDao = db.leaveDao()

        @Provides
        fun provideVisitSurveyResponseDao(db: SfaDatabase): VisitSurveyResponseDao = db.visitSurveyResponseDao()
    }
}
