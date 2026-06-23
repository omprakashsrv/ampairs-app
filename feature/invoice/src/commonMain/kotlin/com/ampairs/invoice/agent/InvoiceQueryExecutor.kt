package com.ampairs.invoice.agent

import androidx.room.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.invoice.db.InvoiceRoomDatabase
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * SAFE_QUERY read-only executor for the invoice module (FR-016, T037). Runs an **already-validated**
 * `SELECT` (gated upstream by `SafeSqlValidator` + the curated `ModuleQuerySchema`) against the
 * invoice DB on a Room **reader** connection — the read-only connection is the runtime backstop that
 * makes a mutating statement impossible even if validation were bypassed.
 *
 * Every column is rendered to a display string (`getText` coerces numbers/blobs; null stays null) so
 * the result is uniform for chat rendering and never leaks typed internals.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("invoice")
class InvoiceQueryExecutor(
    private val database: InvoiceRoomDatabase,
) : ModuleQueryExecutor {

    override val moduleName: String = "invoice"

    override suspend fun executeReadOnly(sql: String): QueryResultSet =
        database.useReaderConnection { connection ->
            connection.usePrepared(sql) { statement ->
                val columnCount = statement.getColumnCount()
                val columns = (0 until columnCount).map { statement.getColumnName(it) }
                val rows = ArrayList<List<String?>>()
                while (statement.step()) {
                    rows.add((0 until columnCount).map { i ->
                        if (statement.isNull(i)) null else statement.getText(i)
                    })
                }
                QueryResultSet(columns = columns, rows = rows)
            }
        }
}
