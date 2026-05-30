package com.ampairs.sync

sealed class SyncResult {
    data class Success(val count: Int = 0) : SyncResult()
    data class Failure(val error: Throwable) : SyncResult()

    fun isSuccess() = this is Success
}
