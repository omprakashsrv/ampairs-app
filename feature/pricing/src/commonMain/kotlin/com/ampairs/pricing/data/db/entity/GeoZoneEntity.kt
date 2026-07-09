package com.ampairs.pricing.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.ampairs.pricing.domain.model.GeoZone
import com.ampairs.pricing.domain.model.GeoZoneMembers

/** Room entity for a geo zone. `members_json` holds the structured [GeoZoneMembers] as JSON text. */
@Entity(
    tableName = "geo_zones",
    indices = [Index(value = ["id"], unique = true, name = "geo_zone_id_idx")]
)
data class GeoZoneEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "ref_id") val refId: String? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "members_json") val membersJson: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

private fun parseMembers(json: String?): GeoZoneMembers =
    if (json.isNullOrBlank()) GeoZoneMembers()
    else runCatching { PricingJson.decodeFromString(GeoZoneMembers.serializer(), json) }
        .getOrDefault(GeoZoneMembers())

private fun encodeMembers(members: GeoZoneMembers): String =
    PricingJson.encodeToString(GeoZoneMembers.serializer(), members)

fun GeoZoneEntity.toGeoZone(): GeoZone = GeoZone(
    uid = id,
    refId = refId,
    name = name,
    members = parseMembers(membersJson),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun GeoZone.toEntity(): GeoZoneEntity = GeoZoneEntity(
    id = uid,
    refId = refId,
    name = name,
    membersJson = encodeMembers(members),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
