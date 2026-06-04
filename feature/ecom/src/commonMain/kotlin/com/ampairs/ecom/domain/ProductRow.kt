package com.ampairs.ecom.domain

import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.data.db.entity.ListedProductEntity

/** A catalog product joined with its current quantity in the cart (for quick-add state). */
data class ProductRow(
    val product: ListedProductEntity,
    val qtyInCart: Int,
)

/** Quantity-in-cart keyed by listed product id. */
fun List<CartItemEntity>.qtyByProduct(): Map<String, Int> =
    associate { it.listed_product_id to it.quantity }

fun List<ListedProductEntity>.withCartQty(cartItems: List<CartItemEntity>): List<ProductRow> {
    val qty = cartItems.qtyByProduct()
    return map { ProductRow(it, qty[it.uid] ?: 0) }
}
