package com.ampairs.agent.llm

import java.io.File

/**
 * Reads total RAM from `/proc/meminfo` (no Android Context needed — it's a readable Linux file on
 * every device). Line form: `MemTotal:       8138752 kB`.
 */
actual object DeviceCapability {
    actual fun totalRamBytes(): Long = runCatching {
        File("/proc/meminfo").useLines { lines ->
            val memTotal = lines.firstOrNull { it.startsWith("MemTotal:") } ?: return@useLines 0L
            val kb = memTotal.filter { it.isDigit() }.toLongOrNull() ?: 0L
            kb * 1024
        }
    }.getOrDefault(0L)
}
