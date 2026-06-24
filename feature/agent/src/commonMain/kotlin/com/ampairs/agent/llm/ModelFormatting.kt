package com.ampairs.agent.llm

import kotlin.math.round

/** Human-readable size for a model file, e.g. 300_000_000 → "286.1 MB", 4_400_000_000 → "4.1 GB". KMP-safe (no String.format). */
fun formatModelSize(bytes: Long): String {
    if (bytes <= 0) return ""
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1024.0) "${oneDecimal(mb / 1024.0)} GB" else "${oneDecimal(mb)} MB"
}

private fun oneDecimal(value: Double): String {
    val scaled = round(value * 10.0).toLong()
    val whole = scaled / 10
    val frac = scaled % 10
    return if (frac == 0L) "$whole" else "$whole.$frac"
}
