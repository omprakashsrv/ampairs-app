package com.ampairs.store.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.store.data.db.StoreDatabase
import com.ampairs.store.data.db.dao.StoreSettingDao
import com.ampairs.store.data.db.dao.StoreSettingDefinitionDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface StoreDaoModule {
    companion object {
        @Provides
        fun provideStoreSettingDao(db: StoreDatabase): StoreSettingDao = db.storeSettingDao()

        @Provides
        fun provideStoreSettingDefinitionDao(db: StoreDatabase): StoreSettingDefinitionDao =
            db.storeSettingDefinitionDao()
    }
}
