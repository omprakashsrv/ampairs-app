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
    INVENTORY_TRANSACTION("inventory_transaction"),
    FORM("form"),
    FILE("file"),
    ECOM_PRODUCT("ecom_product"),
    ECOM_ADDRESS("ecom_address"),
    ECOM_ORDER("ecom_order"),
    PRINT_TEMPLATE("print_template"),
    LEDGER_ENTRY("ledger_entry"),
    PARTY_BALANCE("party_balance"),
    PAYMENT_VOUCHER("payment_voucher"),
    PAYMENT_ALLOCATION("payment_allocation"),
    ADJUSTMENT("adjustment"),
    NOTIFICATION("notification_log"),
    COMM_TEMPLATE("comm_template"),
    COMM_BINDING("comm_binding"),
    COMM_SCHEDULE("comm_schedule"),
    COMM_CAMPAIGN("comm_campaign"),
    COMM_PREFERENCE("comm_preference"),
    COMM_LOG("comm_log");

    companion object {
        fun fromEntityType(type: String): SyncEntity? =
            entries.firstOrNull { it.entityType.equals(type, ignoreCase = true) }
    }
}
