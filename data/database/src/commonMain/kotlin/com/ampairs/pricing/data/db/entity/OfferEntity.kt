package com.ampairs.pricing.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room entity for an offer/promotion. The full Offer is stored as JSON in `payload_json`; a few
 * columns are denormalized for querying/sorting. `synced = false` flags a local write that the
 * OfferSyncDelegate still has to push to the server.
 */
@Entity(
    tableName = "offers",
    indices = [Index(value = ["id"], unique = true, name = "offer_id_idx")]
)
data class OfferEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "channel") val channel: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
)
