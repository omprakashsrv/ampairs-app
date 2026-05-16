package com.ampairs.common.concurrency

import kotlin.native.concurrent.ThreadLocal

actual annotation class Volatile

@ThreadLocal
private val lockObjects = mutableMapOf<Any, Any>()

actual inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // On iOS, we use a simple implementation since true synchronization is limited
    // For production use, consider using AtomicRef or other thread-safe primitives
    return block()
}