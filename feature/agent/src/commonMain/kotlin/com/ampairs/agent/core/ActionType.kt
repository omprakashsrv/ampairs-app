package com.ampairs.agent.core

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    SEARCH,
    LIST,
    COUNT,
    SYNC,
    CALCULATE_TAX,
    GET_INVENTORY,
}
