package com.ampairs.sync.db

import androidx.room.TypeConverter
import com.ampairs.sync.SyncEntity

class SyncStateConverters {
    @TypeConverter
    fun syncEntityToString(entity: SyncEntity): String = entity.name

    @TypeConverter
    fun stringToSyncEntity(name: String): SyncEntity =
        SyncEntity.entries.firstOrNull { it.name == name } ?: SyncEntity.CUSTOMER

    @TypeConverter
    fun syncPersistStatusToString(status: SyncPersistStatus): String = status.name

    @TypeConverter
    fun stringToSyncPersistStatus(name: String): SyncPersistStatus =
        SyncPersistStatus.fromName(name)
}
