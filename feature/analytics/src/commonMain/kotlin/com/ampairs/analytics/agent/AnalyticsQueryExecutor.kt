package com.ampairs.analytics.agent

import androidx.room3.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.agent.WorkspaceDatabaseProvider
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * Agent SafeQuery executor for the analytics module (feature 022, T048). Runs a validated read-only
 * `SELECT` against the `demand_forecast` mirror on a Room **reader** connection — the KPI source
 * modules (invoice/inventory/payment) are already queryable, so the analytics-specific delta is the
 * server-generated forecast table. Body mirrors `CustomerQueryExecutor` verbatim.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("analytics")
class AnalyticsQueryExecutor(
    private val databaseProvider: WorkspaceDatabaseProvider,
) : ModuleQueryExecutor {

    override val moduleName: String = "analytics"

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
