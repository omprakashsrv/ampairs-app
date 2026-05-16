package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.customer.domain.State
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@Entity(
    tableName = "states",
    indices = [
        Index(value = ["id"], unique = true),
        Index(value = ["name"])
    ]
)
data class StateEntity(
    @PrimaryKey val id: String,
    val name: String,
    val syncStatus: String = "SYNCED", // PENDING, SYNCING, SYNCED, FAILED
    val createdAt: Long,
    val updatedAt: Long
)

@OptIn(ExperimentalTime::class)
fun State.toEntity(): StateEntity {
    val now = Clock.System.now().toEpochMilliseconds()
    return StateEntity(
        id = id,
        name = name,
        syncStatus = "SYNCED",
        createdAt = now,
        updatedAt = now
    )
}

fun StateEntity.toDomain(): State {
    return State(
        id = id,
        name = name,
    )
}

fun List<StateEntity>.toDomainList(): List<State> {
    return map { it.toDomain() }
}