package com.ampairs.subscription.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        SubscriptionEntity::class,
        SubscriptionPlanEntity::class,
        DeviceRegistrationEntity::class,
        UsageMetricsEntity::class
    ],
    version = 1,
    exportSchema = true
)
@ConstructedBy(SubscriptionDatabaseConstructor::class)
abstract class SubscriptionDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object SubscriptionDatabaseConstructor : RoomDatabaseConstructor<SubscriptionDatabase> {
    override fun initialize(): SubscriptionDatabase
}
