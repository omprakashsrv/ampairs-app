package com.ampairs.sync

enum class SyncEntity(val entityType: String) {
    CUSTOMER("customer"),
    CUSTOMER_GROUP("customer_group"),
    CUSTOMER_TYPE("customer_type"),
    SUPPLIER("supplier"),
    PRODUCT("product"),
    PRODUCT_CATALOG("product_catalog"),
    PRODUCT_STANDARD_COST("product_standard_cost"),
    PRODUCT_IMAGE("product_image"),
    CUSTOMER_IMAGE("customer_image"),
    ORDER("order"),
    PURCHASE("purchase"),
    INVOICE("invoice"),
    BUSINESS("business"),
    TAX("tax"),
    UNIT("unit"),
    PRICE_LIST("price_list"),
    PRICE_LIST_ITEM("price_list_item"),
    GEO_ZONE("geo_zone"),
    OFFER("offer"),
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
    DEMAND_FORECAST("demand_forecast"),
    MODULE("module"),
    NOTIFICATION("notification_log");

    companion object {
        fun fromEntityType(type: String): SyncEntity? =
            entries.firstOrNull { it.entityType.equals(type, ignoreCase = true) }
    }
}
