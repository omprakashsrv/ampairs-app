package com.ampairs.sync

data class SyncLogEntry(
    val timestamp: Long,
    val entity: SyncEntity,
    val direction: Direction,
    val outcome: Outcome,
    val message: String,
    val detail: String? = null,
    val id: Long = 0L,
) {
    enum class Direction { PUSH, PULL }
    enum class Outcome { SUCCESS, FAILURE, STARTED }
}
