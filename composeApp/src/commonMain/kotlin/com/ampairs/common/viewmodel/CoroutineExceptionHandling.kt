package com.ampairs.common.viewmodel

import com.ampairs.common.sentry.ErrorTracking
import kotlinx.coroutines.CancellationException

/**
 * Utility functions for handling coroutine exceptions in ViewModels.
 *
 * When switching workspaces, ViewModels' coroutines are cancelled, which should not
 * be treated as errors in the UI. These utilities help filter out cancellation exceptions.
 *
 * All non-cancellation exceptions are automatically reported to Sentry for error tracking.
 */

/**
 * Execute a suspend block, rethrowing CancellationException but handling other exceptions.
 * Non-cancellation exceptions are automatically reported to Sentry.
 *
 * Use this in ViewModel suspend functions to properly handle cancellation during workspace switches.
 *
 * Example:
 * ```
 * viewModelScope.launch {
 *     handleCancellation(
 *         tag = "CustomerList",
 *         onError = { error -> _uiState.update { it.copy(error = error) } }
 *     ) {
 *         // Your suspend operations here
 *         val data = repository.fetchData()
 *         _uiState.update { it.copy(data = data) }
 *     }
 * }
 * ```
 *
 * @param tag Optional tag for Sentry error categorization
 * @param onError Callback for handling error messages in UI
 * @param block The suspend block to execute
 */
suspend inline fun <T> handleCancellation(
    tag: String? = null,
    crossinline onError: (String) -> Unit = {},
    crossinline block: suspend () -> T
): T? {
    return try {
        block()
    } catch (e: CancellationException) {
        // Rethrow cancellation to properly cancel the coroutine
        // Don't report cancellations to Sentry - they're expected during workspace switches
        throw e
    } catch (e: Exception) {
        // Report all non-cancellation exceptions to Sentry
        ErrorTracking.captureException(e, tag ?: "ViewModel")
        onError(e.message ?: "Unknown error")
        null
    }
}

/**
 * Check if an exception should be treated as an error in the UI.
 *
 * @return true if the exception should be shown to the user, false if it should be ignored
 */
fun Throwable.shouldShowAsError(): Boolean {
    return this !is CancellationException
}

/**
 * Get a user-friendly error message from a throwable, or null if it's a cancellation.
 * Non-cancellation exceptions are automatically reported to Sentry.
 *
 * @param defaultMessage The message to return if the throwable has no message
 * @param tag Optional tag for Sentry error categorization
 * @param reportToSentry Whether to report this exception to Sentry (default: true)
 * @return Error message string, or null if this is a cancellation exception
 */
fun Throwable.toUserErrorMessage(
    defaultMessage: String = "Unknown error",
    tag: String? = null,
    reportToSentry: Boolean = true
): String? {
    return if (this is CancellationException) {
        null
    } else {
        // Report to Sentry if enabled
        if (reportToSentry) {
            ErrorTracking.captureException(this, tag)
        }
        this.message ?: defaultMessage
    }
}
