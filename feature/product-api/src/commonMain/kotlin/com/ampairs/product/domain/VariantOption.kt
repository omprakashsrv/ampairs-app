package com.ampairs.product.domain

/**
 * A selectable product variant for the order/invoice line editor (spec 010, design screen —
 * variant picker). [sellingPrice] is null when the variant uses the base product's price.
 */
data class VariantOption(
    val sku: String,
    val label: String,
    val sellingPrice: Double?,
)
