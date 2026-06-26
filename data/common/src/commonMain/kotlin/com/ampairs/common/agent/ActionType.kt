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

    // ── Curated business reports (deterministic; the model prefers these over free SQL) ──
    /** Sum of sales/revenue over an optional period (invoice). */
    TOTAL_SALES,

    /** Average invoice value (invoice). */
    AVERAGE_INVOICE,

    /** Total outstanding receivables across parties (payment). */
    OUTSTANDING,

    /** Parties with the largest outstanding balance (payment). */
    TOP_DEBTORS,

    /** Payments received over an optional period (payment). */
    PAYMENTS_RECEIVED,

    /** Best-selling products by sales value over an optional period (invoice). */
    TOP_PRODUCTS,

    /** Highest-revenue customers over an optional period (invoice). */
    TOP_CUSTOMERS,

    /** Items at or below their reorder/low-stock level (product/inventory). */
    LOW_STOCK,

    /** Count of out-of-stock items (product/inventory). */
    OUT_OF_STOCK,

    /** Total stock valuation (inventory). */
    INVENTORY_VALUE,

    /** Append a line item to the conversational document draft (cart). */
    ADD_ITEM,

    /** Set the customer on the conversational document draft (cart). */
    SET_CUSTOMER,

    /** Create an order from the current draft. */
    CREATE_ORDER,

    /** Create an invoice from the current draft. */
    CREATE_INVOICE,
}
