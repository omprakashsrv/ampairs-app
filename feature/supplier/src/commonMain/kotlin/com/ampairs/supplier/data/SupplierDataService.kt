package com.ampairs.supplier.data

import com.ampairs.supplier.domain.Supplier
import com.ampairs.supplier.domain.SupplierListItem

/**
 * Public supplier lookup for cross-module consumers (e.g. the purchase editor). Mirrors
 * `CustomerDataService` on the sell side. Implemented by `SupplierRepository`.
 */
interface SupplierDataService {
    suspend fun getById(uid: String): Supplier?

    /**
     * Lightweight supplier lookup for pickers. Blank [query] returns the full list. Returns list
     * items (id + name + contact + gstin) — call [getById] to load the full supplier once chosen.
     */
    suspend fun listSuppliers(query: String = ""): List<SupplierListItem>
}
