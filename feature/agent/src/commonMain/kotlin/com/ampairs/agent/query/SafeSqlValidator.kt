package com.ampairs.agent.query

/**
 * Defense-in-depth validator for LLM-generated SQL used by the **SAFE_QUERY fallback** (when no typed
 * action matched). Read-only by construction:
 *  - `SELECT` (or a leading `WITH … SELECT`) only — never DDL/DML,
 *  - exactly one statement, no SQL comments,
 *  - no mutating/dangerous keywords (whole-word denylist),
 *  - every `FROM`/`JOIN` target must be an allow-listed table (per [ModuleQuerySchema]) or a CTE,
 *  - a hard row `LIMIT` is enforced.
 *
 * This is the early, explain-why gate; the runtime backstop is still a **read-only DB connection**.
 * Pure Kotlin (commonMain) so it is unit-tested on every target.
 */
class SafeSqlValidator(private val maxRows: Int = DEFAULT_MAX_ROWS) {

    fun validate(raw: String, schema: ModuleQuerySchema): SqlValidationResult {
        val trimmed = raw.trim().removeSuffix(";").trim()
        if (trimmed.isEmpty()) return reject("Empty query.")

        // Comments could hide a denied keyword or a second statement.
        if (trimmed.contains("--") || trimmed.contains("/*")) {
            return reject("Comments are not allowed.")
        }
        // Single statement only (no chaining).
        if (trimmed.contains(";")) return reject("Only a single statement is allowed.")

        val lower = trimmed.lowercase()

        // SELECT-only (a leading CTE is fine as long as it resolves to a SELECT).
        if (!lower.startsWith("select") && !lower.startsWith("with")) {
            return reject("Only SELECT queries are allowed.")
        }
        if (lower.startsWith("with") && !Regex("""\bselect\b""").containsMatchIn(lower)) {
            return reject("A WITH query must contain a SELECT.")
        }

        // Reject mutating / dangerous keywords (whole-word; word boundaries keep `created_at` etc. safe).
        for (kw in DENY) {
            if (Regex("""\b$kw\b""").containsMatchIn(lower)) {
                return reject("Keyword '$kw' is not allowed.")
            }
        }

        // CTE names declared via `name AS ( … )` are valid FROM/JOIN targets.
        val cteNames = Regex("""\b([a-z_][a-z0-9_]*)\s+as\s*\(""")
            .findAll(lower).map { it.groupValues[1] }.toSet()

        // Every FROM/JOIN target must be an allow-listed table or a declared CTE.
        val referenced = Regex("""\b(?:from|join)\s+([a-z_][a-z0-9_]*)""")
            .findAll(lower).map { it.groupValues[1] }.toList()
        if (referenced.isEmpty()) return reject("No table referenced.")
        for (table in referenced) {
            if (table !in schema.allowedTables && table !in cteNames) {
                return reject("Table '$table' is not queryable.")
            }
        }

        return SqlValidationResult.Valid(enforceLimit(trimmed, lower))
    }

    /** Append a LIMIT when absent; cap it to [maxRows] when present and larger. */
    private fun enforceLimit(sql: String, lower: String): String {
        val match = Regex("""\blimit\s+(\d+)""").find(lower) ?: return "$sql LIMIT $maxRows"
        val n = match.groupValues[1].toIntOrNull() ?: return "$sql LIMIT $maxRows"
        if (n <= maxRows) return sql
        // lower is sql.lowercase() (same length for ASCII), so the match range maps onto sql.
        return sql.substring(0, match.range.first) + "LIMIT $maxRows" + sql.substring(match.range.last + 1)
    }

    private fun reject(reason: String): SqlValidationResult = SqlValidationResult.Rejected(reason)

    companion object {
        const val DEFAULT_MAX_ROWS = 200

        // Whole-word denylist — anything that could mutate, escalate, or read outside the schema.
        private val DENY = listOf(
            "insert", "update", "delete", "drop", "alter", "create", "replace",
            "attach", "detach", "pragma", "vacuum", "reindex", "trigger",
            "grant", "revoke", "truncate", "upsert",
        )
    }
}

sealed interface SqlValidationResult {
    /** The normalized, LIMIT-bounded statement that is safe to run on a read-only connection. */
    data class Valid(val sql: String) : SqlValidationResult

    /** Why the candidate was refused (surfaced to the user / fed back to the model to retry). */
    data class Rejected(val reason: String) : SqlValidationResult
}
