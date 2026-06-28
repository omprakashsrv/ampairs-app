package com.ampairs.communication.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.communication.data.db.CommBindingDao
import com.ampairs.communication.data.db.CommCampaignDao
import com.ampairs.communication.data.db.CommLogDao
import com.ampairs.communication.data.db.CommPreferenceDao
import com.ampairs.communication.data.db.CommScheduleDao
import com.ampairs.communication.data.db.CommTemplateDao
import com.ampairs.communication.data.db.CommunicationDatabase
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

/** Provides the communication DAOs from the workspace-scoped [CommunicationDatabase]. */
@ContributesTo(WorkspaceScope::class)
interface CommunicationDaoModule {
    companion object {
        @Provides
        fun provideTemplateDao(db: CommunicationDatabase): CommTemplateDao = db.templateDao()

        @Provides
        fun provideBindingDao(db: CommunicationDatabase): CommBindingDao = db.bindingDao()

        @Provides
        fun provideScheduleDao(db: CommunicationDatabase): CommScheduleDao = db.scheduleDao()

        @Provides
        fun provideCampaignDao(db: CommunicationDatabase): CommCampaignDao = db.campaignDao()

        @Provides
        fun providePreferenceDao(db: CommunicationDatabase): CommPreferenceDao = db.preferenceDao()

        @Provides
        fun provideLogDao(db: CommunicationDatabase): CommLogDao = db.logDao()
    }
}
