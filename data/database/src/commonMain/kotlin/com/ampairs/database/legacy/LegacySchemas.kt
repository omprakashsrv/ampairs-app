package com.ampairs.database.legacy

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.ampairs.common.database.legacy.LegacyDatabaseSource
import com.ampairs.business.data.db.migrations.BUSINESS_MIGRATION_2_3
import com.ampairs.business.data.db.migrations.BUSINESS_MIGRATION_3_4
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_6_7
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_7_8
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_8_9
import com.ampairs.customer.data.db.migrations.CUSTOMER_MIGRATION_9_10
import com.ampairs.ecom.data.db.migrations.ECOM_MIGRATION_1_2
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_1_2
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_2_3
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_3_4
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_4_5
import com.ampairs.invoice.db.migrations.INVOICE_MIGRATION_5_6
import com.ampairs.order.db.migrations.ORDER_MIGRATION_1_2
import com.ampairs.order.db.migrations.ORDER_MIGRATION_2_3
import com.ampairs.order.db.migrations.ORDER_MIGRATION_3_4
import com.ampairs.order.db.migrations.ORDER_MIGRATION_4_5
import com.ampairs.order.db.migrations.ORDER_MIGRATION_5_6
import com.ampairs.pricing.data.db.migrations.PRICING_MIGRATION_1_2
import com.ampairs.product.db.migrations.MIGRATION_10_11
import com.ampairs.product.db.migrations.MIGRATION_1_2
import com.ampairs.product.db.migrations.MIGRATION_2_3
import com.ampairs.product.db.migrations.MIGRATION_3_4
import com.ampairs.product.db.migrations.MIGRATION_4_5
import com.ampairs.product.db.migrations.MIGRATION_5_6
import com.ampairs.product.db.migrations.MIGRATION_6_7
import com.ampairs.product.db.migrations.MIGRATION_7_8
import com.ampairs.product.db.migrations.MIGRATION_8_9
import com.ampairs.product.db.migrations.MIGRATION_9_10
import com.ampairs.store.data.db.migrations.STORE_MIGRATION_1_2
import com.ampairs.sync.db.migrations.SYNC_MIGRATION_1_2
import com.ampairs.tax.data.db.migrations.TAX_MIGRATION_2_3
import com.ampairs.unit.data.db.migrations.UNIT_MIGRATION_1_2

/**
 * The legacy per-module database files each consolidated database absorbs, with the module's Room
 * migrations so an out-of-date legacy file is brought to the latest schema before the copy.
 *
 * The migration lists mirror the (now replaced) per-feature platform DB providers — when a feature
 * ships a new legacy migration during the deprecation window it must be added here too. After the
 * window, this whole `legacy` package and the old per-feature database classes are deleted together.
 */
object LegacySchemas {

    /**
     * Auth 2→3 lived per-platform in feature/auth's platform modules (not commonMain), so it is
     * duplicated here verbatim rather than referenced.
     */
    private val LEGACY_AUTH_MIGRATION_2_3 = object : Migration(2, 3) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_url TEXT")
            connection.execSQL("ALTER TABLE userEntity ADD COLUMN profile_picture_thumbnail_url TEXT")
        }
    }

    /**
     * Legacy AppScope files → [com.ampairs.database.AmpairsAppDatabase].
     * [pathFor] resolves a bare file name (e.g. "auth.db") to the platform's app-database location.
     */
    fun appSources(pathFor: (fileName: String) -> String): List<LegacyDatabaseSource> = listOf(
        LegacyDatabaseSource(pathFor("auth.db"), migrations = listOf(LEGACY_AUTH_MIGRATION_2_3)),
        LegacyDatabaseSource(pathFor("workspace.db")),
        // Disposable cache — deleted, not copied; the model manifest re-pulls on next launch.
        LegacyDatabaseSource(pathFor("agent_catalog.db"), copyData = false),
    )

    /**
     * Legacy per-module workspace files → [com.ampairs.database.AmpairsWorkspaceDatabase].
     * [pathFor] resolves a legacy module name to that module's platform-specific workspace DB path.
     */
    fun workspaceSources(pathFor: (moduleName: String) -> String): List<LegacyDatabaseSource> = listOf(
        LegacyDatabaseSource(
            pathFor("customer"),
            migrations = listOf(
                CUSTOMER_MIGRATION_6_7, CUSTOMER_MIGRATION_7_8, CUSTOMER_MIGRATION_8_9, CUSTOMER_MIGRATION_9_10,
            ),
        ),
        LegacyDatabaseSource(
            pathFor("product"),
            migrations = listOf(
                MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6,
                MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11,
            ),
        ),
        LegacyDatabaseSource(pathFor("tax"), migrations = listOf(TAX_MIGRATION_2_3)),
        LegacyDatabaseSource(
            pathFor("order"),
            migrations = listOf(
                ORDER_MIGRATION_1_2, ORDER_MIGRATION_2_3, ORDER_MIGRATION_3_4, ORDER_MIGRATION_4_5,
                ORDER_MIGRATION_5_6,
            ),
        ),
        LegacyDatabaseSource(
            pathFor("invoice"),
            migrations = listOf(
                INVOICE_MIGRATION_1_2, INVOICE_MIGRATION_2_3, INVOICE_MIGRATION_3_4, INVOICE_MIGRATION_4_5,
                INVOICE_MIGRATION_5_6,
            ),
        ),
        LegacyDatabaseSource(pathFor("purchase")),
        LegacyDatabaseSource(pathFor("payment")),
        LegacyDatabaseSource(pathFor("inventory")),
        LegacyDatabaseSource(pathFor("unit"), migrations = listOf(UNIT_MIGRATION_1_2)),
        LegacyDatabaseSource(pathFor("form_v2")),
        LegacyDatabaseSource(pathFor("file")),
        LegacyDatabaseSource(pathFor("business"), migrations = listOf(BUSINESS_MIGRATION_2_3, BUSINESS_MIGRATION_3_4)),
        LegacyDatabaseSource(pathFor("store"), migrations = listOf(STORE_MIGRATION_1_2)),
        LegacyDatabaseSource(pathFor("subscription")),
        LegacyDatabaseSource(pathFor("supplier")),
        LegacyDatabaseSource(pathFor("sequence")),
        LegacyDatabaseSource(pathFor("pricing"), migrations = listOf(PRICING_MIGRATION_1_2)),
        LegacyDatabaseSource(pathFor("offers")),
        LegacyDatabaseSource(pathFor("printing")),
        LegacyDatabaseSource(pathFor("notification")),
        LegacyDatabaseSource(pathFor("ecom"), migrations = listOf(ECOM_MIGRATION_1_2)),
        LegacyDatabaseSource(pathFor("agent_chat")),
        LegacyDatabaseSource(pathFor("sync"), migrations = listOf(SYNC_MIGRATION_1_2)),
    )
}
