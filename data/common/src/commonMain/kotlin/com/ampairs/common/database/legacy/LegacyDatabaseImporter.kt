package com.ampairs.common.database.legacy

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import co.touchlab.kermit.Logger

/**
 * One legacy per-module database file to fold into a consolidated database.
 *
 * @param path absolute path of the legacy `.db` file (may not exist — skipped silently).
 * @param migrations the module's Room migrations; applied manually on a raw connection when the
 *   legacy file's `user_version` is behind, so its schema matches the consolidated tables before
 *   the copy. (Bypassing Room here also skips identity-hash validation, which the legacy file
 *   would fail against the consolidated schema.)
 * @param copyData false for disposable caches (agent catalog): the file is deleted, nothing copied.
 */
class LegacyDatabaseSource(
    val path: String,
    val migrations: List<Migration> = emptyList(),
    val copyData: Boolean = true,
)

/**
 * One-time importer that folds the legacy per-module database files into a consolidated database.
 *
 * Runs inside the consolidated database's `onOpen` callback (i.e. on the DB's query dispatcher,
 * before the first query is answered), so no feature code can observe a half-imported state:
 *
 * 1. Skip sources whose file is gone or already marked imported (marker table `_legacy_imports`
 *    inside the consolidated DB — atomic with the copied rows).
 * 2. Open the legacy file with a raw [BundledSQLiteDriver] connection and bring it up to the
 *    module's latest schema by running its pending [Migration]s manually.
 * 3. Copy every table that exists on both sides, column-intersection, `INSERT OR REPLACE`,
 *    wrapped in a savepoint together with the marker row — a crash mid-copy rolls back and the
 *    import re-runs on next open.
 * 4. Delete the legacy file (+`-wal`/`-shm`) only after the savepoint released.
 *
 * A source that fails is rolled back, logged, and left on disk for the next attempt; the other
 * sources still import.
 */
object LegacyDatabaseImporter {

    private const val MARKER_TABLE = "_legacy_imports"
    private val log = Logger.withTag("LegacyDatabaseImporter")

    /** Builds the Room callback. [sources] is invoked lazily on first open. */
    fun callback(sources: () -> List<LegacyDatabaseSource>): RoomDatabase.Callback =
        object : RoomDatabase.Callback() {
            override suspend fun onOpen(connection: SQLiteConnection) {
                importAll(connection, sources())
            }
        }

    suspend fun importAll(target: SQLiteConnection, sources: List<LegacyDatabaseSource>) {
        val present = sources.filter { legacyFileExists(it.path) }
        if (present.isEmpty()) return
        target.execSQL(
            "CREATE TABLE IF NOT EXISTS $MARKER_TABLE (source TEXT NOT NULL PRIMARY KEY, imported_at TEXT NOT NULL)"
        )
        present.forEach { source ->
            try {
                importOne(target, source)
            } catch (t: Throwable) {
                log.e("import failed for ${source.path} — legacy file kept for retry", t)
            }
        }
    }

    private suspend fun importOne(target: SQLiteConnection, source: LegacyDatabaseSource) {
        val key = source.path.substringAfterLast('/')
        if (isImported(target, key)) {
            // Marked in a previous run but the file survived (delete failed / crash after commit).
            deleteLegacyDatabaseFiles(source.path)
            return
        }
        if (!source.copyData) {
            markImported(target, key)
            deleteLegacyDatabaseFiles(source.path)
            log.i("dropped disposable legacy db $key")
            return
        }

        val legacy = BundledSQLiteDriver().open(source.path)
        try {
            migrateLegacy(legacy, source.migrations, key)
            target.execSQL("SAVEPOINT legacy_import")
            try {
                target.execSQL("PRAGMA defer_foreign_keys = TRUE")
                val copied = copyTables(legacy, target)
                markImported(target, key)
                target.execSQL("RELEASE legacy_import")
                log.i("imported $copied table(s) from $key")
            } catch (t: Throwable) {
                target.execSQL("ROLLBACK TO legacy_import")
                target.execSQL("RELEASE legacy_import")
                throw t
            }
        } finally {
            legacy.close()
        }
        deleteLegacyDatabaseFiles(source.path)
    }

    /** Replays the module's pending Room migrations on the raw legacy connection. */
    private suspend fun migrateLegacy(legacy: SQLiteConnection, migrations: List<Migration>, key: String) {
        var version = legacy.queryLong("PRAGMA user_version")?.toInt() ?: return
        if (version == 0) return // brand-new empty file — nothing meaningful to migrate/copy
        while (true) {
            val next = migrations.firstOrNull { it.startVersion == version } ?: return
            log.i("migrating legacy $key ${next.startVersion} -> ${next.endVersion}")
            next.migrate(legacy)
            version = next.endVersion
        }
    }

    /** Copies every user table present on both sides; returns the number of tables copied. */
    private fun copyTables(legacy: SQLiteConnection, target: SQLiteConnection): Int {
        val legacyTables = legacy.userTables()
        val targetTables = target.userTables().toSet()
        var copied = 0
        legacyTables.filter { it in targetTables }.forEach { table ->
            val legacyCols = legacy.tableColumns(table)
            val targetCols = target.tableColumns(table).map { it.first }.toSet()
            val cols = legacyCols.map { it.first }.filter { it in targetCols }
            if (cols.isEmpty()) return@forEach
            val colTypes = legacyCols.filter { it.first in targetCols }
            val colList = cols.joinToString(", ") { "\"$it\"" }
            val placeholders = cols.joinToString(", ") { "?" }
            target.prepare("INSERT OR REPLACE INTO \"$table\" ($colList) VALUES ($placeholders)").useStatement { insert ->
                legacy.prepare("SELECT $colList FROM \"$table\"").useStatement { select ->
                    while (select.step()) {
                        insert.reset()
                        insert.clearBindings()
                        colTypes.forEachIndexed { i, (_, declaredType) ->
                            bindColumn(select, insert, i, declaredType)
                        }
                        insert.step()
                    }
                }
            }
            copied++
        }
        return copied
    }

    /**
     * Binds column [index] of [select]'s current row onto [insert] (1-based bind index), using the
     * column's declared type for the getter — Room DDL declares exact INTEGER/TEXT/REAL/BLOB types.
     */
    private fun bindColumn(select: SQLiteStatement, insert: SQLiteStatement, index: Int, declaredType: String) {
        val bindIndex = index + 1
        if (select.isNull(index)) {
            insert.bindNull(bindIndex)
            return
        }
        val type = declaredType.uppercase()
        when {
            type.contains("INT") -> insert.bindLong(bindIndex, select.getLong(index))
            type.contains("REAL") || type.contains("FLOA") || type.contains("DOUB") ->
                insert.bindDouble(bindIndex, select.getDouble(index))
            type.contains("BLOB") -> insert.bindBlob(bindIndex, select.getBlob(index))
            else -> insert.bindText(bindIndex, select.getText(index))
        }
    }

    private fun isImported(target: SQLiteConnection, key: String): Boolean =
        target.prepare("SELECT 1 FROM $MARKER_TABLE WHERE source = ?").useStatement { stmt ->
            stmt.bindText(1, key)
            stmt.step()
        }

    private fun markImported(target: SQLiteConnection, key: String) {
        target.prepare("INSERT OR REPLACE INTO $MARKER_TABLE (source, imported_at) VALUES (?, datetime('now'))")
            .useStatement { stmt ->
                stmt.bindText(1, key)
                stmt.step()
            }
    }

    private fun SQLiteConnection.userTables(): List<String> =
        prepare(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT IN ('room_master_table', 'android_metadata', '$MARKER_TABLE')"
        ).useStatement { stmt ->
            buildList { while (stmt.step()) add(stmt.getText(0)) }
        }

    /** name -> declared type, in table order. */
    private fun SQLiteConnection.tableColumns(table: String): List<Pair<String, String>> =
        prepare("PRAGMA table_info(\"$table\")").useStatement { stmt ->
            buildList { while (stmt.step()) add(stmt.getText(1) to stmt.getText(2)) }
        }

    private fun SQLiteConnection.queryLong(sql: String): Long? =
        prepare(sql).useStatement { stmt -> if (stmt.step()) stmt.getLong(0) else null }

    private inline fun <R> SQLiteStatement.useStatement(block: (SQLiteStatement) -> R): R =
        try {
            block(this)
        } finally {
            close()
        }
}

internal expect fun legacyFileExists(path: String): Boolean

/** Deletes the legacy database file plus its `-wal` and `-shm` sidecars. */
internal expect fun deleteLegacyDatabaseFiles(path: String)
