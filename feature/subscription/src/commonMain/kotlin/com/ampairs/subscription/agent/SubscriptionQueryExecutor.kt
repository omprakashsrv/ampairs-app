package com.ampairs.subscription.agent

import androidx.room3.useReaderConnection
import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.QueryExecutorKey
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.subscription.db.SubscriptionDatabase
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject

/**
 * SAFE_QUERY read-only executor for the subscription module (FR-016). Runs an already-validated
 * `SELECT` against the subscription DB on a Room **reader** connection.
 */
@Inject
@ContributesIntoMap(WorkspaceScope::class)
@QueryExecutorKey("subscription")
class SubscriptionQueryExecutor(
    private val database: SubscriptionDatabase,
) : ModuleQueryExecutor {

    override val moduleName: String = "subscription"

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
