package com.ampairs.sync.db

enum class SyncPersistStatus {
    IDLE,
    PENDING_PUSH,
    PENDING_PULL,
    SYNCING,
    FAILED;

    companion object {
        fun fromName(name: String): SyncPersistStatus =
            entries.firstOrNull { it.name == name } ?: IDLE
    }
}
