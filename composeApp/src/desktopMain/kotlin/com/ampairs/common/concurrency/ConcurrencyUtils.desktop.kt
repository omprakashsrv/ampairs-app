package com.ampairs.common.concurrency

actual typealias Volatile = kotlin.jvm.Volatile

actual inline fun <T> synchronized(lock: Any, block: () -> T): T = kotlin.synchronized(lock, block)