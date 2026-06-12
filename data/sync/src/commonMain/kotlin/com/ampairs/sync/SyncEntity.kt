package com.ampairs.sync

enum class SyncEntity(val entityType: String) {
    CUSTOMER("customer"),
    CUSTOMER_GROUP("customer_group"),
    CUSTOMER_TYPE("customer_type"),
    PRODUCT("product"),
    PRODUCT_CATALOG("product_catalog"),
    PRODUCT_IMAGE("product_image"),
    CUSTOMER_IMAGE("customer_image"),
    ORDER("order"),
    INVOICE("invoice"),
    BUSINESS("business"),
    TAX("tax"),
    UNIT("unit"),
    SEQUENCE("sequence"),
    STORE("setting"),
    INVENTORY("inventory"),
    FORM("form"),
    FILE("file"),
    ECOM_PRODUCT("ecom_product"),
    ECOM_ADDRESS("ecom_address"),
    ECOM_ORDER("ecom_order");

    companion object {
        fun fromEntityType(type: String): SyncEntity? =
            entries.firstOrNull { it.entityType.equals(type, ignoreCase = true) }
    }
}
