package com.ampairs.common.agent

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

    /** Append a line item to the conversational document draft (cart). */
    ADD_ITEM,

    /** Set the customer on the conversational document draft (cart). */
    SET_CUSTOMER,
}
