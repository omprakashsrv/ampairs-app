package com.ampairs.product.domain

import com.ampairs.product.domain.ProductSummary

fun Product.toSummary(): ProductSummary = ProductSummary(
    id = id,
    name = name,
    code = code,
    mrp = mrp,
    dp = dp,
    sellingPrice = sellingPrice,
    stockQuantity = stockQuantity,
    lowStockAlert = lowStockAlert,
    primaryImageUrl = primaryImageUrl,
    taxCode = taxCode,
    productType = productType,
    serviceType = serviceType,
    hasVariants = hasVariants,
    categoryName = categoryName,
    brandName = brandName,
    baseUnitId = baseUnitId,
)
