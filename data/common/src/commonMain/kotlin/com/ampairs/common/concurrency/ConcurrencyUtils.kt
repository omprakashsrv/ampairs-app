package com.ampairs.common.concurrency

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.BINARY)
expect annotation class Volatile()

expect inline fun <T> synchronized(lock: Any, block: () -> T): T