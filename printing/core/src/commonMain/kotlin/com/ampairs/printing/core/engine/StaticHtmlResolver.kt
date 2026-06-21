package com.ampairs.printing.core.engine

import com.ampairs.printing.core.model.EntityRef
import com.ampairs.printing.core.model.FieldValue
import com.ampairs.printing.core.model.PlainValueFormatter
import com.ampairs.printing.core.model.ValueFormatter
import com.ampairs.printing.core.provider.LineValues

/**
 * Resolves a **static HTML template** by substituting `{{variables}}` against a document's values.
 * The HTML file itself is never modified — this only produces the rendered output string.
 *
 * Supported syntax (mustache-ish):
 * - `{{field_key}}` — a DOCUMENT-scope value (standard, then custom, then computed).
 * - `{{customer.field}}` / `{{product.field}}` — a nested entity's value (`ref` = [EntityRef] name,
 *   lowercased). Inside a line loop, `{{product.field}}` resolves against that line's product.
 * - `{{#lines}} … {{/lines}}` — the inner block is repeated once per line item; inside it, bare
 *   `{{field}}` resolves against the line first, then falls back to the document.
 *
 * Substituted values are HTML-escaped so data can never break the surrounding markup. Unknown tokens
 * resolve to an empty string. Pure (no IO) so it is unit-testable and shared by preview + print.
 */
object StaticHtmlResolver {

    private val lineSection = Regex("\\{\\{#lines\\}\\}([\\s\\S]*?)\\{\\{/lines\\}\\}")
    private val token = Regex("\\{\\{\\s*([\\w.]+)\\s*\\}\\}")

    fun resolve(
        html: String,
        standard: Map<String, FieldValue>,
        custom: Map<String, String> = emptyMap(),
        nested: Map<EntityRef, Map<String, FieldValue>> = emptyMap(),
        lines: List<LineValues> = emptyList(),
        computed: Map<String, FieldValue> = emptyMap(),
        formatter: ValueFormatter = PlainValueFormatter,
    ): String {
        // 1) Expand line loops first (the inner block resolves per-line; product. nested supported).
        val withLines = lineSection.replace(html) { match ->
            val inner = match.groupValues[1]
            lines.joinToString(separator = "") { line ->
                token.replace(inner) { t -> resolveToken(t.groupValues[1], line, standard, custom, nested, computed, formatter) }
            }
        }
        // 2) Resolve the remaining document-scope tokens.
        return token.replace(withLines) { t ->
            resolveToken(t.groupValues[1], line = null, standard, custom, nested, computed, formatter)
        }
    }

    private fun resolveToken(
        key: String,
        line: LineValues?,
        standard: Map<String, FieldValue>,
        custom: Map<String, String>,
        nested: Map<EntityRef, Map<String, FieldValue>>,
        computed: Map<String, FieldValue>,
        formatter: ValueFormatter,
    ): String {
        if ('.' in key) {
            val refName = key.substringBefore('.')
            val field = key.substringAfter('.')
            val ref = EntityRef.entries.firstOrNull { it.name.equals(refName, ignoreCase = true) }
                ?: return ""
            val map = line?.nested?.get(ref) ?: nested[ref] ?: return ""
            return map[field]?.let { esc(formatter.format(it)) } ?: ""
        }
        // Bare key: line scope first (inside a loop), then document standard/custom/computed.
        line?.let { l ->
            l.standard[key]?.let { return esc(formatter.format(it)) }
            l.custom[key]?.let { return esc(it) }
        }
        standard[key]?.let { return esc(formatter.format(it)) }
        custom[key]?.let { return esc(it) }
        computed[key]?.let { return esc(formatter.format(it)) }
        return ""
    }

    private fun esc(s: String): String = buildString(s.length) {
        for (ch in s) when (ch) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            else -> append(ch)
        }
    }
}
