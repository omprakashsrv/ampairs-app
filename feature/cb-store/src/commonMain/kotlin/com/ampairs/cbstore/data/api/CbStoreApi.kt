package com.ampairs.cbstore.data.api

import com.ampairs.cbstore.domain.model.Store
import com.ampairs.cbstore.domain.model.ZonalOffice
import com.ampairs.common.model.PageResponse
import com.ampairs.common.model.Response

/**
 * API for the `cb_store` backend module — outlets and zonal offices, both on the canonical `/sync`
 * contract (`GET`/`POST /cb_store/v1/{resource}/sync`).
 */
interface CbStoreApi {

    suspend fun getStoresSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<Store>

    suspend fun bulkUpdateStores(stores: List<Store>): List<Store>

    suspend fun getStoreById(id: String): Response<Store>

    suspend fun getZonalOfficesSync(
        lastSync: String,
        page: Int = 0,
        size: Int = 100,
        sortBy: String = "updatedAt",
        sortDir: String = "ASC",
    ): PageResponse<ZonalOffice>

    suspend fun bulkUpdateZonalOffices(offices: List<ZonalOffice>): List<ZonalOffice>

    suspend fun getZonalOfficeById(id: String): Response<ZonalOffice>
}
