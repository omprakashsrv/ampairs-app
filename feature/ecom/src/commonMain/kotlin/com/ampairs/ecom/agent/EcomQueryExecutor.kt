package com.ampairs.ecom.agent

import androidx.room3.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.agent.WorkspaceDatabaseProvider
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * SAFE_QUERY read-only executor for the ecom (storefront) module (FR-016). Runs an already-validated
 * `SELECT` against the ecom DB on a Room **reader** connection.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("ecom")
class EcomQueryExecutor(
    private val databaseProvider: WorkspaceDatabaseProvider,
) : ModuleQueryExecutor {

    override val moduleName: String = "ecom"

    override suspend fun executeReadOnly(sql: String): QueryResultSet =
        databaseProvider.get().useReaderConnection { connection ->
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
