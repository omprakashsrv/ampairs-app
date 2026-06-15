package com.ampairs.customer.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Adds indexes on the columns used by the customer list filter (state, type, group) so multi-select
 * filtering stays fast as the customer table grows. Index names match the [Index] names declared on
 * [com.ampairs.customer.data.db.CustomerEntity] so Room's schema validation passes on open.
 */
val CUSTOMER_MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS `customer_state_idx` ON `customers` (`state`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `customer_type_idx` ON `customers` (`customer_type`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `customer_group_idx` ON `customers` (`customer_group`)")
    }
}
