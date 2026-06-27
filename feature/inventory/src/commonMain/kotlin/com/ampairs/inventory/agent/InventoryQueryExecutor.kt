package com.ampairs.inventory.agent

import androidx.room.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.inventory.data.db.InventoryDatabase
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * SAFE_QUERY read-only executor for the inventory module (FR-016, T037). Runs an already-validated
 * `SELECT` (gated upstream by `SafeSqlValidator` + the curated `ModuleQuerySchema`) against the
 * inventory DB on a Room **reader** connection — the runtime backstop that makes a mutating statement
 * impossible even if validation were bypassed. Columns render to display strings; null stays null.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("inventory")
class InventoryQueryExecutor(
    private val database: InventoryDatabase,
) : ModuleQueryExecutor {

    override val moduleName: String = "inventory"

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
