package com.ampairs.sync

/**
 * A syncable entity type.
 *
 * [moduleCode] is the workspace module (backend `moduleCode`, e.g. "invoice-billing",
 * "cb-maintenance") that owns this entity, used to hide sync rows for modules a workspace has NOT
 * installed. `null` marks core/infrastructure entities (settings, form, file, sequence, module,
 * notification) that sync regardless of which feature modules are installed — those are always shown.
 * Module-code strings mirror the backend contract intentionally; this module cannot depend on the
 * `feature/workspace` `ModuleCodes` object.
 */
enum class SyncEntity(val entityType: String, val moduleCode: String? = null) {
    CUSTOMER("customer", "customer-management"),
    CUSTOMER_GROUP("customer_group", "customer-management"),
    CUSTOMER_TYPE("customer_type", "customer-management"),
    CUSTOMER_IMAGE("customer_image", "customer-management"),
    SUPPLIER("supplier", "supplier-management"),
    PRODUCT("product", "product-management"),
    PRODUCT_CATALOG("product_catalog", "product-management"),
    PRODUCT_STANDARD_COST("product_standard_cost", "product-management"),
    PRODUCT_IMAGE("product_image", "product-management"),
    ORDER("order", "order-management"),
    PURCHASE("purchase", "purchase-management"),
    INVOICE("invoice", "invoice-billing"),
    BUSINESS("business", "business-profile"),
    TAX("tax", "tax-code-management"),
    UNIT("unit", "unit-management"),
    PRICE_LIST("price_list", "pricing-management"),
    PRICE_LIST_ITEM("price_list_item", "pricing-management"),
    GEO_ZONE("geo_zone", "pricing-management"),
    OFFER("offer", "pricing-management"),
    SEQUENCE("sequence"),
    STORE("setting"),
    INVENTORY("inventory", "inventory-management"),
    INVENTORY_TRANSACTION("inventory_transaction", "inventory-management"),
    FORM("form"),
    FILE("file"),
    ECOM_PRODUCT("ecom_product", "storefront-management"),
    ECOM_ADDRESS("ecom_address", "storefront-management"),
    ECOM_ORDER("ecom_order", "storefront-management"),
    PRINT_TEMPLATE("print_template", "printing-management"),
    LEDGER_ENTRY("ledger_entry", "payment-collection"),
    PARTY_BALANCE("party_balance", "payment-collection"),
    PAYMENT_VOUCHER("payment_voucher", "payment-collection"),
    PAYMENT_ALLOCATION("payment_allocation", "payment-collection"),
    ADJUSTMENT("adjustment", "inventory-management"),
    DEMAND_FORECAST("demand_forecast", "inventory-management"),
    MODULE("module"),
    NOTIFICATION("notification_log"),
    // cb_* — maintenance build (customer-specific)
    CB_EMPLOYEE("cb_employee", "cb-employee"),
    CB_ZONAL_OFFICE("cb_zonal_office", "cb-store"),
    CB_STORE("cb_store", "cb-store"),
    CB_PM_SCHEDULE("cb_pm_schedule", "cb-maintenance"),
    CB_PM_ENTRY("cb_pm_entry", "cb-maintenance"),
    CB_TICKET("cb_ticket", "cb-maintenance"),
    CB_ASSET_ALIAS("cb_asset_alias", "cb-maintenance"),
    CB_TICKET_BUCKET("cb_ticket_bucket", "cb-maintenance");

    companion object {
        fun fromEntityType(type: String): SyncEntity? =
            entries.firstOrNull { it.entityType.equals(type, ignoreCase = true) }
    }
}
