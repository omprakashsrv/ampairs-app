package com.ampairs.common.coroutines

import kotlinx.coroutines.CoroutineDispatcher

expect object DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}