package com.ampairs.pricing.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ampairs.pricing.domain.model.GeoZone
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

/** Room entity for a geo zone. `members_json` holds the pincode/range/state list as JSON text. */
@Entity(
    tableName = "geo_zones",
    indices = [Index(value = ["id"], unique = true, name = "geo_zone_id_idx")]
)
data class GeoZoneEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "members_json") val membersJson: String? = null,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)

private fun parseMembers(json: String?): List<String> =
    if (json.isNullOrBlank()) emptyList()
    else runCatching { PricingJson.decodeFromString(ListSerializer(String.serializer()), json) }
        .getOrDefault(emptyList())

private fun encodeMembers(members: List<String>): String? =
    if (members.isEmpty()) null
    else PricingJson.encodeToString(ListSerializer(String.serializer()), members)

fun GeoZoneEntity.toGeoZone(): GeoZone = GeoZone(
    uid = id,
    name = name,
    members = parseMembers(membersJson),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun GeoZone.toEntity(): GeoZoneEntity = GeoZoneEntity(
    id = uid,
    name = name,
    membersJson = encodeMembers(members),
    active = active,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
