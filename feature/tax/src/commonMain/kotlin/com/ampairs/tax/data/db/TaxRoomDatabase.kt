package com.ampairs.tax.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.ampairs.tax.data.db.dao.TaxCodeDao
import com.ampairs.tax.data.db.dao.TaxComponentDao
import com.ampairs.tax.data.db.dao.TaxComponentTypeDao
import com.ampairs.tax.data.db.dao.TaxConfigurationDao
import com.ampairs.tax.data.db.dao.TaxRuleDao
import com.ampairs.tax.data.db.entity.TaxCodeEntity
import com.ampairs.tax.data.db.entity.TaxComponentEntity
import com.ampairs.tax.data.db.entity.TaxComponentTypeEntity
import com.ampairs.tax.data.db.entity.TaxConfigurationEntity
import com.ampairs.tax.data.db.entity.TaxRuleEntity

/**
 * Tax Room Database - Fresh Implementation (V2 Architecture)
 *
 * This is a complete replacement of the old tax database.
 * Old entities (HsnCodeEntity, TaxRateEntity) have been removed.
 *
 * New Architecture:
 * - Master tax codes are server-side only (not synced to mobile)
 * - Subscribed tax codes synced to mobile per workspace
 * - Component-based tax system with reusable components
 * - Tax rules with scenario-based composition
 * - Strategy pattern for multi-country support
 * - Database isolation per workspace via WorkspaceAwareDatabaseFactory
 */
@Database(
    entities = [
        TaxCodeEntity::class,
        TaxComponentTypeEntity::class,
        TaxComponentEntity::class,
        TaxRuleEntity::class,
        TaxConfigurationEntity::class
    ],
    version = 3,  // v2→v3: added custom_name to tax_codes
    exportSchema = true
)
@ConstructedBy(TaxRoomDatabaseConstructor::class)
abstract class TaxRoomDatabase : RoomDatabase() {
    abstract fun taxCodeDao(): TaxCodeDao
    abstract fun taxComponentTypeDao(): TaxComponentTypeDao
    abstract fun taxComponentDao(): TaxComponentDao
    abstract fun taxRuleDao(): TaxRuleDao
    abstract fun taxConfigurationDao(): TaxConfigurationDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TaxRoomDatabaseConstructor : RoomDatabaseConstructor<TaxRoomDatabase> {
    override fun initialize(): TaxRoomDatabase
}