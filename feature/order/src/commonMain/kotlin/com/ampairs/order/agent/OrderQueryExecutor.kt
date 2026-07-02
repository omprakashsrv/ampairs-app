package com.ampairs.order.agent

import androidx.room.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.order.db.OrderRoomDatabase
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * SAFE_QUERY read-only executor for the order module (FR-016). Runs an already-validated `SELECT`
 * (gated upstream by `SafeSqlValidator` + the curated `ModuleQuerySchema`) against the order DB on a
 * Room **reader** connection — the runtime backstop that makes a mutating statement impossible even if
 * validation were bypassed. Columns render to display strings; null stays null.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("order")
class OrderQueryExecutor(
    private val database: OrderRoomDatabase,
) : ModuleQueryExecutor {

    override val moduleName: String = "order"

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
