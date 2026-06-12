package com.ampairs.sequence.domain

/**
 * Client mirror of the backend `SequenceFormatter` — the single formatting rule shared by
 * server-side generation and offline on-device generation. Keep both in sync.
 */
object SequenceFormatter {

    private val DEFAULT_PREFIXES = mapOf(
        "product" to "PRD",
        "customer" to "CUS",
        "order" to "ORD",
        "invoice" to "INV",
    )

    /** `[prefix-]paddedValue[-suffix]` — separator omitted when prefix/suffix absent. */
    fun format(prefix: String?, suffix: String?, paddingLength: Int, value: Long): String = buildString {
        prefix?.takeIf { it.isNotBlank() }?.let {
            append(it)
            append('-')
        }
        append(value.toString().padStart(paddingLength, '0'))
        suffix?.takeIf { it.isNotBlank() }?.let {
            append('-')
            append(it)
        }
    }

    fun defaultPrefix(entityType: String): String =
        DEFAULT_PREFIXES[entityType.lowercase()]
            ?: entityType.filter { it.isLetterOrDigit() }.take(3).uppercase().ifBlank { "SEQ" }
}
