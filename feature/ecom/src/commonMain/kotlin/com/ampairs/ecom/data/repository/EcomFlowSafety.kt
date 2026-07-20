package com.ampairs.ecom.data.repository

import com.ampairs.ecom.domain.EcomLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * Guards a workspace Room `Flow` against the workspace database being torn down under it.
 *
 * The storefront apps tear down and recreate the whole workspace graph every time the buyer exits or
 * switches a store, which closes that workspace DB's connection pool. Any Room `Flow` still being
 * collected (a screen's read model, kept alive briefly by `stateIn`/`WhileSubscribed` or a not-yet-
 * disposed ViewModel) then re-queries the closed pool and throws
 * `SQLException: Connection pool is closed`. Because these flows are collected in a `viewModelScope`,
 * that exception is uncaught and crashes the app — and the chance of hitting it compounds the more
 * times the buyer goes in and out of stores.
 *
 * This swallows the terminal DB error and emits [fallback] so the collector completes cleanly instead
 * of crashing. [CancellationException] is rethrown so structured concurrency / normal flow
 * cancellation is unaffected. Intended for read-model SELECT flows, whose only realistic terminal
 * error is this teardown.
 */
internal fun <T> Flow<T>.orEmitOnDbClosed(fallback: T): Flow<T> = catch { cause ->
    if (cause is CancellationException) throw cause
    EcomLogger.w("EcomFlow", "workspace DB flow terminated (store teardown?) — emitting fallback", cause)
    emit(fallback)
}
