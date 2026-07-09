package com.ampairs.product.db.model

import androidx.room3.Embedded
import com.ampairs.product.db.entity.ProductEntity

data class ProductModel(
    @Embedded
    val product: ProductEntity,
)