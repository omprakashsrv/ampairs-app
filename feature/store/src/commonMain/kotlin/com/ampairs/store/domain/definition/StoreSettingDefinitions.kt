package com.ampairs.store.domain.definition

import com.ampairs.store.domain.SettingValueType

/**
 * The catalog of settings the app knows about. Keep in sync with the backend's
 * `SettingDefinitionProvider` declarations — the server validates pushed values against its own copy.
 *
 * The generic settings screen renders one row per definition (grouped/filtered by [StoreSettingDefinition.module]).
 */
object StoreSettingDefinitions {

    val ALL: List<StoreSettingDefinition> = listOf(
        StoreSettingDefinition(
            module = "common",
            key = "prices_include_tax",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "false",
            label = "Prices include tax",
            description = "Treat product prices as tax-inclusive across ordering and invoicing.",
        ),
        StoreSettingDefinition(
            module = "invoice",
            key = "show_discount_options",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Show discount options on invoices",
            description = "Display discount fields while creating an invoice.",
        ),
        StoreSettingDefinition(
            module = "order",
            key = "show_discount_options",
            valueType = SettingValueType.BOOLEAN,
            defaultValue = "true",
            label = "Show discount options on orders",
            description = "Display discount fields while creating an order.",
        ),
    )

    /** Distinct module codes, in declaration order — drives the filter chips. */
    val MODULES: List<String> = ALL.map { it.module }.distinct()

    fun find(module: String, key: String): StoreSettingDefinition? =
        ALL.firstOrNull { it.module == module && it.key == key }
}
