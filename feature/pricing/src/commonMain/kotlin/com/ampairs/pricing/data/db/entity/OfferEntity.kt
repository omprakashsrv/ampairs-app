package com.ampairs.pricing.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.pricing.domain.model.Offer
import com.ampairs.pricing.domain.model.OfferStatus
import com.ampairs.pricing.domain.model.SalesChannel

/**
 * Room entity for an offer/promotion. The full [Offer] is stored as JSON in `payload_json`; a few
 * columns are denormalized for querying/sorting. Local-only (no sync) until a backend exists.
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
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
)

fun OfferEntity.toOffer(): Offer =
    runCatching { PricingJson.decodeFromString(Offer.serializer(), payloadJson) }
        .getOrElse {
            Offer(
                uid = id,
                name = name,
                channel = runCatching { SalesChannel.valueOf(channel) }.getOrDefault(SalesChannel.RETAIL),
                status = runCatching { OfferStatus.valueOf(status) }.getOrDefault(OfferStatus.DRAFT),
                active = active,
                updatedAt = updatedAt,
            )
        }

fun Offer.toEntity(): OfferEntity = OfferEntity(
    id = uid,
    name = name,
    channel = channel.name,
    status = status.name,
    active = active,
    updatedAt = updatedAt,
    payloadJson = PricingJson.encodeToString(Offer.serializer(), this),
)
