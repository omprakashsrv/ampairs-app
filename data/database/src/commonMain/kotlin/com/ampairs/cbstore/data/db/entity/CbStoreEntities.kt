package com.ampairs.cbstore.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Room entity for a California Burrito zonal office. `id` is the backend `uid`. */
@Entity(
    tableName = "cb_zonal_offices",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_zonal_office_id_idx"),
    ],
)
data class ZonalOfficeEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "city") val city: String,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)

/** Room entity for a California Burrito outlet. `id` is the backend `uid`. */
@Entity(
    tableName = "cb_stores",
    indices = [
        Index(value = ["id"], unique = true, name = "cb_store_id_idx"),
        Index(value = ["zonal_office_id"], name = "cb_store_zone_idx"),
    ],
)
data class StoreEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "code") val code: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "city") val city: String,
    @ColumnInfo(name = "zonal_office_id") val zonalOfficeId: String,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
)
