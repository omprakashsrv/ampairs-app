package com.ampairs.pricing.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.pricing.data.db.dao.OfferDao
import com.ampairs.pricing.data.db.entity.OfferEntity

/**
 * Offers Room Database — workspace-isolated store for promotions/offers.
 *
 * Kept SEPARATE from [PricingDatabase] so adding offers needed no schema migration of the existing
 * price-list DB. Local-only for now (no sync delegate; no backend offer resource yet).
 *
 * Version: 1 (initial)
 */
@Database(
    entities = [
        OfferEntity::class,
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(OffersDatabaseConstructor::class)
abstract class OffersDatabase : RoomDatabase() {
    abstract fun offerDao(): OfferDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object OffersDatabaseConstructor : RoomDatabaseConstructor<OffersDatabase> {
    override fun initialize(): OffersDatabase
}
