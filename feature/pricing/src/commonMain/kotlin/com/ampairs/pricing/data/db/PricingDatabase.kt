package com.ampairs.pricing.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.pricing.data.db.dao.GeoZoneDao
import com.ampairs.pricing.data.db.dao.PriceListDao
import com.ampairs.pricing.data.db.dao.PriceListItemDao
import com.ampairs.pricing.data.db.entity.GeoZoneEntity
import com.ampairs.pricing.data.db.entity.PriceListEntity
import com.ampairs.pricing.data.db.entity.PriceListItemEntity

/**
 * Pricing Room Database — workspace-isolated store for price lists, their items, and geo zones.
 *
 * Version: 2 (v1→v2: added effective_from/effective_to to price_list_items — effective-dated pricing)
 */
@Database(
    entities = [
        PriceListEntity::class,
        PriceListItemEntity::class,
        GeoZoneEntity::class,
    ],
    version = 2,
    exportSchema = true
)
@ConstructedBy(PricingDatabaseConstructor::class)
abstract class PricingDatabase : RoomDatabase() {
    abstract fun priceListDao(): PriceListDao
    abstract fun priceListItemDao(): PriceListItemDao
    abstract fun geoZoneDao(): GeoZoneDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PricingDatabaseConstructor : RoomDatabaseConstructor<PricingDatabase> {
    override fun initialize(): PricingDatabase
}
