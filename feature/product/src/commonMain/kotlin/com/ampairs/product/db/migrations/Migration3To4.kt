package com.ampairs.product.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection

// No-op bridge: v4 was bumped without a schema change.
// Required so Room can migrate v3 → v5 via the chain 3→4→5.
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) = Unit
}
