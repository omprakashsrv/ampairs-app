package com.ampairs.product.data

import com.ampairs.product.domain.ProductSummary

interface ProductDataService {
    suspend fun getById(uid: String): ProductSummary?
    suspend fun getByIds(ids: List<String>): List<ProductSummary>
}
