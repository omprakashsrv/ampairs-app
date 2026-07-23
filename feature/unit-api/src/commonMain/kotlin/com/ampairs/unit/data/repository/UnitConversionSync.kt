package com.ampairs.unit.data.repository

/**
 * Plain cross-module representation of a product-scoped unit conversion (no Room/DI types), so
 * `feature/product` can transport conversions on the product `/sync` DTO without depending on the
 * full `feature/unit` module. Mirrors the fields of `UnitConversionEntity`.
 *
 * Semantics: `derived_quantity = base_quantity * multiplier` (e.g. 1 Box = 12 Pcs → base = Box,
 * derived = Pcs, multiplier = 12).
 */
data class UnitConversionData(
    val uid: String,
    val productId: String,
    val baseUnitId: String,
    val derivedUnitId: String,
    val multiplier: Double,
    val active: Boolean = true,
)

/**
 * Cross-module bridge that lets [com.ampairs.product]'s sync delegate carry product-scoped unit
 * conversions on the product `/sync` feed — the only server transport for them. Implemented in
 * `feature/unit` over the conversion DAO; lives here so consumers needn't depend on the full module.
 */
interface UnitConversionSync {

    /** Active conversions for each of [productIds]; products with none are absent from the map. */
    suspend fun conversionsForProducts(productIds: List<String>): Map<String, List<UnitConversionData>>

    /**
     * Distinct product ids that have at least one locally-unsynced conversion. The product delegate
     * pushes these (carrying their conversions) even when the product row itself is already synced.
     */
    suspend fun productIdsWithUnsyncedConversions(): List<String>

    /** Marks every conversion of [productId] synced after a successful product push. */
    suspend fun markConversionsSynced(productId: String)

    /**
     * Replaces the local conversions for [productId] with the server set (stored synced), unless the
     * local copy has unsynced edits (local wins until pushed). Server rows flagged inactive are
     * dropped. No-op reconciliation when [conversions] is empty and there is nothing local to clear.
     */
    suspend fun replaceConversionsFromServer(productId: String, conversions: List<UnitConversionData>)
}
