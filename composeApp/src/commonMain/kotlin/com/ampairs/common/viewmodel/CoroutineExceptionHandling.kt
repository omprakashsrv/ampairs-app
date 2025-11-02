package com.ampairs.common.viewmodel

import kotlinx.coroutines.CancellationException

/**
 * Utility functions for handling coroutine exceptions in ViewModels.
 *
 * When switching workspaces, ViewModels' coroutines are cancelled, which should not
 * be treated as errors in the UI. These utilities help filter out cancellation exceptions.
 */

/**
 * Execute a suspend block, rethrowing CancellationException but handling other exceptions.
 *
 * Use this in ViewModel suspend functions to properly handle cancellation during workspace switches.
 *
 * Example:
 * ```
 * viewModelScope.launch {
 *     handleCancellation(
 *         onError = { error -> _uiState.update { it.copy(error = error) } }
 *     ) {
 *         // Your suspend operations here
 *         val data = repository.fetchData()
 *         _uiState.update { it.copy(data = data) }
 *     }
 * }
 * ```
 */
suspend inline fun <T> handleCancellation(
    crossinline onError: (String) -> Unit = {},
    crossinline block: suspend () -> T
): T? {
    return try {
        block()
    } catch (e: CancellationException) {
        // Rethrow cancellation to properly cancel the coroutine
        throw e
    } catch (e: Exception) {
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
 *
 * @param defaultMessage The message to return if the throwable has no message
 * @return Error message string, or null if this is a cancellation exception
 */
fun Throwable.toUserErrorMessage(defaultMessage: String = "Unknown error"): String? {
    return if (this is CancellationException) {
        null
    } else {
        this.message ?: defaultMessage
    }
}
