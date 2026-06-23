package com.ampairs.agent.query

import com.ampairs.common.agent.ModuleQueryExecutor
import com.ampairs.common.agent.ModuleQuerySchema
import com.ampairs.common.agent.QueryResultSet
import com.ampairs.common.di.WorkspaceScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Multibinds

/**
 * Runs the SAFE_QUERY fallback (FR-016): given a model-proposed `SELECT` for one module, it looks up
 * that module's curated [ModuleQuerySchema], validates the SQL with [SafeSqlValidator] (SELECT-only,
 * single-statement, allow-listed tables, enforced LIMIT), and — only if valid — executes it on the
 * module's read-only [ModuleQueryExecutor]. Writes are impossible by construction; cross-module
 * queries are impossible (one executor per SQLite DB).
 *
 * The executor map is populated per module by the (device-side) `SqlQueryDelegate`s (T037); until
 * those land it is empty and the service reports the module as not-yet-queryable rather than failing.
 */
@Inject
class SafeQueryService(
    private val schemas: Map<String, ModuleQuerySchema>,
    private val executors: Map<String, ModuleQueryExecutor>,
) {
    // Pure, dependency-free SQL gate — not a DI binding, so construct it directly (not injected).
    private val validator = SafeSqlValidator()

    suspend fun run(moduleName: String, candidateSql: String): SafeQueryOutcome {
        // Each module owns and contributes its curated schema into the multibound map.
        val schema = schemas[moduleName]
            ?: return SafeQueryOutcome.Unavailable("I can't query the '$moduleName' module.")

        val sql = when (val validation = validator.validate(candidateSql, schema)) {
            is SqlValidationResult.Valid -> validation.sql
            is SqlValidationResult.Rejected -> return SafeQueryOutcome.Rejected(validation.reason)
        }

        val executor = executors[moduleName]
            ?: return SafeQueryOutcome.Unavailable("Querying '$moduleName' isn't available yet.")

        return try {
            SafeQueryOutcome.Rows(executor.executeReadOnly(sql))
        } catch (e: Exception) {
            SafeQueryOutcome.Failed(e.message ?: "Query failed.")
        }
    }
}

/** Outcome of a SAFE_QUERY attempt; [toText] renders a chat-friendly summary. */
sealed interface SafeQueryOutcome {
    data class Rows(val result: QueryResultSet) : SafeQueryOutcome
    data class Rejected(val reason: String) : SafeQueryOutcome
    data class Unavailable(val reason: String) : SafeQueryOutcome
    data class Failed(val message: String) : SafeQueryOutcome

    fun toText(): String = when (this) {
        is Rows -> result.toText()
        is Rejected -> "I can't run that query: $reason"
        is Unavailable -> reason
        is Failed -> "The query failed: $message"
    }
}

/** Compact text rendering of a result set for the chat bubble (caps rows so the bubble stays small). */
private fun QueryResultSet.toText(): String {
    if (rows.isEmpty()) return "No matching records."
    val header = columns.joinToString(" | ")
    val shown = rows.take(MAX_RENDERED_ROWS)
    val body = shown.joinToString("\n") { row -> row.joinToString(" | ") { it ?: "" } }
    val more = if (rows.size > MAX_RENDERED_ROWS) "\n… and ${rows.size - MAX_RENDERED_ROWS} more" else ""
    return "$header\n$body$more"
}

private const val MAX_RENDERED_ROWS = 20

/**
 * Workspace-scope multibindings for the SAFE_QUERY maps — both `allowEmpty` so the graph resolves
 * before any per-module executor/schema (T037) contributes. Mirrors the sync-delegate pattern. Each
 * feature module contributes its executor (`@ContributesIntoMap` + `@QueryExecutorKey`) and its
 * curated schema (`@Provides @IntoMap @QuerySchemaKey`).
 */
@ContributesTo(WorkspaceScope::class)
interface SafeQueryExecutorModule {
    @Multibinds(allowEmpty = true)
    fun moduleQueryExecutors(): Map<String, ModuleQueryExecutor>

    @Multibinds(allowEmpty = true)
    fun moduleQuerySchemas(): Map<String, ModuleQuerySchema>
}
