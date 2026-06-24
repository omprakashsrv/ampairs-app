package com.ampairs.pricing.di

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.pricing.data.db.PricingDatabase
import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides

@ContributesTo(WorkspaceScope::class)
interface PricingDaoModule {
    companion object {
        @Provides
        fun providePriceListDao(db: PricingDatabase): PriceListDao = db.priceListDao()

        @Provides
        fun providePriceListItemDao(db: PricingDatabase): PriceListItemDao = db.priceListItemDao()

        @Provides
        fun provideGeoZoneDao(db: PricingDatabase): GeoZoneDao = db.geoZoneDao()
    }
}

fun pricingModule() = Unit
