package com.ampairs.unit.ui.conversion

import com.ampairs.unit.data.db.dao.UnitConversionDao
import com.ampairs.unit.util.UnitLogger

/**
 * Simple unit conversion engine using direct multiplier approach
 *
 * Formula: derived_quantity = base_quantity * multiplier
 *
 * Example Conversions:
 * - 1 kg = 1000 g: UnitConversion(baseUnitId="kg", derivedUnitId="g", multiplier=1000.0)
 * - 1 BOX = 12 PCS: UnitConversion(baseUnitId="BOX", derivedUnitId="PCS", multiplier=12.0)
 * - 1 m = 100 cm: UnitConversion(baseUnitId="m", derivedUnitId="cm", multiplier=100.0)
 *
 * Features:
 * - Direct conversions (base → derived)
 * - Inverse conversions (derived → base using division)
 * - Product-specific conversions
 * - Fast, predictable calculations
 *
 * Non-Features (by design):
 * - No chain conversions (kg → g → mg)
 * - No complex unit operations (multiplication, division of units)
 * - No automatic unit inference
 */
class UnitConversionEngine(
    private val unitConversionDao: UnitConversionDao
) {

    /**
     * Convert quantity from one unit to another for a specific product
     *
     * Tries two approaches:
     * 1. Direct conversion: base → derived (multiply)
     * 2. Inverse conversion: derived → base (divide)
     *
     * @param quantity The quantity to convert
     * @param fromUnitId Source unit ID
     * @param toUnitId Target unit ID
     * @param productId Product ID for product-specific conversions
     * @return Converted quantity, or null if no conversion exists
     *
     * @example
     * ```kotlin
     * // Convert 2 BOX to PCS for Product A (1 BOX = 12 PCS)
     * convertQuantity(2.0, "BOX", "PCS", "PRD123") // Returns 24.0
     *
     * // Convert 24 PCS to BOX (inverse conversion)
     * convertQuantity(24.0, "PCS", "BOX", "PRD123") // Returns 2.0
     *
     * // Convert 1.5 kg to g (1 kg = 1000 g)
     * convertQuantity(1.5, "kg", "g", "PRD123") // Returns 1500.0
     * ```
     */
    suspend fun convertQuantity(
        quantity: Double,
        fromUnitId: String,
        toUnitId: String,
        productId: String
    ): Double? {
        // Same unit - no conversion needed
        if (fromUnitId == toUnitId) {
            UnitLogger.d("UnitConversionEngine", "Same unit: $fromUnitId, no conversion needed")
            return quantity
        }

        try {
            // Approach 1: Try direct conversion (base → derived)
            val directConversion = unitConversionDao.getUnitConversion(
                productId = productId,
                baseUnitId = fromUnitId,
                derivedUnitId = toUnitId
            )

            if (directConversion != null) {
                val result = quantity * directConversion.multiplier
                UnitLogger.d(
                    "UnitConversionEngine",
                    "Direct conversion: $quantity $fromUnitId × ${directConversion.multiplier} = $result $toUnitId"
                )
                return result
            }

            // Approach 2: Try inverse conversion (derived → base)
            val inverseConversion = unitConversionDao.getUnitConversion(
                productId = productId,
                baseUnitId = toUnitId,
                derivedUnitId = fromUnitId
            )

            if (inverseConversion != null) {
                val result = quantity / inverseConversion.multiplier
                UnitLogger.d(
                    "UnitConversionEngine",
                    "Inverse conversion: $quantity $fromUnitId ÷ ${inverseConversion.multiplier} = $result $toUnitId"
                )
                return result
            }

            // No conversion found
            UnitLogger.w(
                "UnitConversionEngine",
                "No conversion found: $fromUnitId → $toUnitId for product $productId"
            )
            return null

        } catch (e: Exception) {
            UnitLogger.e("UnitConversionEngine", "Conversion failed", e)
            return null
        }
    }

    /**
     * Check if conversion exists between two units for a product
     *
     * @param fromUnitId Source unit ID
     * @param toUnitId Target unit ID
     * @param productId Product ID for product-specific conversions
     * @return true if conversion exists (direct or inverse), false otherwise
     */
    suspend fun hasConversion(
        fromUnitId: String,
        toUnitId: String,
        productId: String
    ): Boolean {
        // Same unit always "convertible"
        if (fromUnitId == toUnitId) return true

        // Check direct conversion
        val directConversion = unitConversionDao.getUnitConversion(
            productId = productId,
            baseUnitId = fromUnitId,
            derivedUnitId = toUnitId
        )

        if (directConversion != null) {
            UnitLogger.d("UnitConversionEngine", "Direct conversion exists: $fromUnitId → $toUnitId")
            return true
        }

        // Check inverse conversion
        val inverseConversion = unitConversionDao.getUnitConversion(
            productId = productId,
            baseUnitId = toUnitId,
            derivedUnitId = fromUnitId
        )

        if (inverseConversion != null) {
            UnitLogger.d("UnitConversionEngine", "Inverse conversion exists: $fromUnitId → $toUnitId")
            return true
        }

        UnitLogger.d("UnitConversionEngine", "No conversion exists: $fromUnitId → $toUnitId")
        return false
    }

    /**
     * Get the conversion multiplier for a specific conversion
     *
     * Useful for displaying conversion information in UI
     *
     * @param fromUnitId Source unit ID
     * @param toUnitId Target unit ID
     * @param productId Product ID
     * @return Multiplier if conversion exists, null otherwise
     *
     * @example
     * ```kotlin
     * // Get multiplier for BOX → PCS
     * getMultiplier("BOX", "PCS", "PRD123") // Returns 12.0
     *
     * // Get multiplier for inverse conversion (returns reciprocal)
     * getMultiplier("PCS", "BOX", "PRD123") // Returns 0.083333 (1/12)
     * ```
     */
    suspend fun getMultiplier(
        fromUnitId: String,
        toUnitId: String,
        productId: String
    ): Double? {
        if (fromUnitId == toUnitId) return 1.0

        // Try direct conversion
        val directConversion = unitConversionDao.getUnitConversion(
            productId = productId,
            baseUnitId = fromUnitId,
            derivedUnitId = toUnitId
        )

        if (directConversion != null) {
            return directConversion.multiplier
        }

        // Try inverse conversion (return reciprocal)
        val inverseConversion = unitConversionDao.getUnitConversion(
            productId = productId,
            baseUnitId = toUnitId,
            derivedUnitId = fromUnitId
        )

        return if (inverseConversion != null) {
            1.0 / inverseConversion.multiplier
        } else {
            null
        }
    }
}
