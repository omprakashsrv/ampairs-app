package com.ampairs.tax.data.repository

import com.ampairs.tax.domain.model.TaxCode

interface TaxCodeLookup {
    suspend fun searchWorkspaceTaxCodes(query: String, limit: Int = 50): List<TaxCode>
    suspend fun incrementUsageCount(id: String)
}
