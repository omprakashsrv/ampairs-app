package com.ampairs.agent.llm

import java.lang.management.ManagementFactory

/** Total physical RAM via the HotSpot OS MXBean (Java 14+, cross-OS). */
actual object DeviceCapability {
    actual fun totalRamBytes(): Long = runCatching {
        val bean = ManagementFactory.getOperatingSystemMXBean() as com.sun.management.OperatingSystemMXBean
        bean.totalMemorySize
    }.getOrDefault(0L)
}
