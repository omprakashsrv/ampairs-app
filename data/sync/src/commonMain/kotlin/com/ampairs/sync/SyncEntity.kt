package com.ampairs.sync

enum class SyncEntity(val entityType: String) {
    CUSTOMER("customer"),
    PRODUCT("product"),
    ORDER("order"),
    INVOICE("invoice"),
    BUSINESS("business"),
    TAX("tax"),
    UNIT("unit"),
    INVENTORY("inventory"),
    FORM("form");

    companion object {
        fun fromEntityType(type: String): SyncEntity? =
            entries.firstOrNull { it.entityType.equals(type, ignoreCase = true) }
    }
}
