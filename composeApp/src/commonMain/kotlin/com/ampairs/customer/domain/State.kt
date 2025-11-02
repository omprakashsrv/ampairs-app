package com.ampairs.customer.domain

import kotlinx.serialization.Serializable

@Serializable
data class State(
    val id: String = "",
    val name: String = "",
)

@Serializable
data class StateListItem(
    val id: String,
    val name: String,
)

fun State.toListItem(): StateListItem = StateListItem(
    id = id,
    name = name,
)