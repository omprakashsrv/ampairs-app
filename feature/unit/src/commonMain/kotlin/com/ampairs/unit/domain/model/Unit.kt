package com.ampairs.unit.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unit domain model representing a unit of measurement
 *
 * Examples: Kilogram (kg), Meter (m), Liter (L), Piece (pcs), Box, Dozen
 */
@Serializable
data class Unit(
    val uid: String = "",
    val name: String = "",
    @SerialName("short_name")
    val shortName: String = "",
    @SerialName("decimal_places")
    val decimalPlaces: Int = 2,
    val description: String? = null,
    val category: String? = null, // Weight, Volume, Length, Count, etc.
    val active: Boolean = true,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Lightweight unit representation for UI lists
 */
data class UnitListItem(
    val id: String,
    val name: String,
    val shortName: String,
    val decimalPlaces: Int,
    val description: String?,
    val category: String?,
    val active: Boolean
)

/**
 * Convert domain Unit to UI list item
 */
fun Unit.toListItem(): UnitListItem = UnitListItem(
    id = uid,
    name = name,
    shortName = shortName,
    decimalPlaces = decimalPlaces,
    description = description,
    category = category,
    active = active
)
