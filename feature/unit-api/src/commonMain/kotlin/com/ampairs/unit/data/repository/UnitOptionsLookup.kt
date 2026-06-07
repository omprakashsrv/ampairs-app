package com.ampairs.unit.data.repository

/**
 * A sellable unit choice for a product line.
 *
 * [multiplier] expresses how many *base* units one of this unit equals
 * (the base unit always has `multiplier = 1.0`). The line editor multiplies
 * the entered quantity by [multiplier] to derive `base_quantity`, and scales
 * the per-base price up by the same factor for display.
 */
data class UnitOption(
    val unitId: String,
    val name: String,
    val shortName: String,
    val multiplier: Double,
    val isBase: Boolean,
    val decimalPlaces: Int = 2,
)

/**
 * Cross-module lookup that resolves the unit choices available for a product:
 * the product's base unit plus any product-specific derived-unit conversions.
 *
 * Implemented in `feature/unit` over the unit + conversion DAOs and consumed by
 * the order / invoice line editors. Lives in `feature/unit-api` so consumers do
 * not depend on the full unit feature module.
 */
interface UnitOptionsLookup {
    /**
     * Returns the unit options sellable for [productId], base unit first.
     * If [baseUnitId] is blank/unknown or there are no conversions, returns
     * just the base unit (or an empty list when neither is resolvable).
     */
    suspend fun unitsForProduct(productId: String, baseUnitId: String?): List<UnitOption>
}
