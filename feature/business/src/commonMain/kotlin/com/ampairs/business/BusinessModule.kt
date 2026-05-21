package com.ampairs.business

import com.ampairs.common.di.AppScope
import com.ampairs.business.data.db.BusinessDatabase
import com.ampairs.business.data.db.BusinessDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(AppScope::class)
interface BusinessDaoModule {
    companion object {
        @Provides
        fun provideBusinessDao(db: BusinessDatabase): BusinessDao = db.businessDao()
    }
}
