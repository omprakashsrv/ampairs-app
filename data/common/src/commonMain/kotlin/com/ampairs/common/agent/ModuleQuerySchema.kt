package com.ampairs.common.agent

/**
 * A curated, **read-only** description of the tables/columns the agent's SAFE_QUERY fallback is
 * allowed to query for ONE module's SQLite (Room) database.
 *
 * This is deliberately hand-authored per module — NOT derived from `sqlite_master` — so that:
 *  - internal/sync plumbing columns (`synced`, soft-delete flags, audit columns) stay hidden,
 *  - other modules' tables are unreachable (each module is a separate DB anyway),
 *  - the schema fed to the LLM (for text-to-SQL) stays small and safe.
 *
 * Lives in `data/common` (alongside [ModuleQueryExecutor]) so each owning feature module can define
 * and contribute its own schema (per-module ownership, FR-016/T037) without depending on the agent
 * module. The agent's `SafeSqlValidator` rejects any query that references a table outside
 * [ModuleQuerySchema.allowedTables].
 */
data class ColumnSchema(
    val name: String,
    val type: String = "",
    val description: String = "",
)

data class TableSchema(
    val name: String,
    val columns: List<ColumnSchema> = emptyList(),
)

data class ModuleQuerySchema(
    val moduleName: String,
    val tables: List<TableSchema>,
) {
    /** Lower-cased table names the agent may read from this module's DB. */
    val allowedTables: Set<String> = tables.mapTo(HashSet()) { it.name.lowercase() }

    /** Compact schema text for an LLM text-to-SQL prompt (tables + columns only). */
    fun toPromptText(): String = buildString {
        appendLine("Read-only schema for module '$moduleName' (SELECT only):")
        tables.forEach { table ->
            val cols = table.columns.joinToString(", ") { col ->
                if (col.type.isBlank()) col.name else "${col.name} ${col.type}"
            }
            appendLine("  ${table.name}(${cols})")
        }
    }
}
