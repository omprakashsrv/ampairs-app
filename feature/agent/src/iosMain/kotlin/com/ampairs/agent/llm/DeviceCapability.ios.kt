package com.ampairs.agent.llm

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSProcessInfo

/** Total physical RAM via Foundation's `NSProcessInfo.physicalMemory`. */
actual object DeviceCapability {
    @OptIn(ExperimentalForeignApi::class)
    actual fun totalRamBytes(): Long = NSProcessInfo.processInfo.physicalMemory.toLong()
}
