package com.ampairs.tax.agent

import androidx.room.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.tax.data.db.TaxRoomDatabase
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * SAFE_QUERY read-only executor for the tax module (FR-016). Runs an already-validated `SELECT`
 * against the tax DB on a Room **reader** connection.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("tax")
class TaxQueryExecutor(
    private val database: TaxRoomDatabase,
) : ModuleQueryExecutor {

    override val moduleName: String = "tax"

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
