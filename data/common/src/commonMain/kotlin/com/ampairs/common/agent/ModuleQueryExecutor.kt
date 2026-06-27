package com.ampairs.common.agent

import dev.zacsweers.metro.MapKey

/** A read-only result set from a SAFE_QUERY: column names + string-rendered rows. */
data class QueryResultSet(
    val columns: List<String>,
    val rows: List<List<String?>>,
)

/**
 * Per-module read-only SQL executor for the SAFE_QUERY fallback (FR-016). Each workspace-aware module
 * contributes one (keyed by module name) so the agent can run a *validated* read-only `SELECT` against
 * that module's own SQLite DB — the multi-DB constraint means a query never spans modules.
 *
 * Implementations MUST use a **reader** connection and MUST NOT mutate data; the SQL is already
 * gated by `SafeSqlValidator` + the module's curated schema before it reaches here, but the read-only
 * connection is the runtime backstop. Contributed via
 * `@Inject @ContributesIntoMap(WorkspaceScope::class) @QueryExecutorKey("<module>")`.
 */
interface ModuleQueryExecutor {
    val moduleName: String

    /** Run an already-validated read-only SELECT and return the rows. */
    suspend fun executeReadOnly(sql: String): QueryResultSet
}

/**
 * Metro map key — contributes a [ModuleQueryExecutor] into the SAFE_QUERY executor map keyed by
 * module name (e.g. "invoice"). Mirrors [ActionHandlerKey].
 */
@MapKey
annotation class QueryExecutorKey(val value: String)
