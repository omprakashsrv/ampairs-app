package com.ampairs.communication.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * Workspace-isolated communication database. Mirrors the backend communication module's syncable
 * resources (templates/bindings/schedules/campaigns/preferences) plus a pull-only delivery-log
 * cache. One file per workspace per device — created via
 * [com.ampairs.common.database.WorkspaceAwareDatabaseFactory].
 */
@Database(
    entities = [
        CommTemplateEntity::class,
        CommBindingEntity::class,
        CommScheduleEntity::class,
        CommCampaignEntity::class,
        CommPreferenceEntity::class,
        CommLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@ConstructedBy(CommunicationDatabaseConstructor::class)
abstract class CommunicationDatabase : RoomDatabase() {
    abstract fun templateDao(): CommTemplateDao
    abstract fun bindingDao(): CommBindingDao
    abstract fun scheduleDao(): CommScheduleDao
    abstract fun campaignDao(): CommCampaignDao
    abstract fun preferenceDao(): CommPreferenceDao
    abstract fun logDao(): CommLogDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object CommunicationDatabaseConstructor : RoomDatabaseConstructor<CommunicationDatabase> {
    override fun initialize(): CommunicationDatabase
}
