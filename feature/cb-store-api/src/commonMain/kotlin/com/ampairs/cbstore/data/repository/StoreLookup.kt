package com.ampairs.cbstore.data.repository

import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice

/**
 * Cross-feature read surface for outlets. `cb-maintenance` depends on this
 * (`-api`) interface only — never on the impl module (api/impl split).
 */
interface StoreLookup {
    suspend fun activeStores(): List<Store>
    suspend fun getStore(storeId: String): Store?
    suspend fun zonalOfficeIdFor(storeId: String): String?
    suspend fun activeZonalOffices(): List<ZonalOffice>
}
