package com.ampairs.business.data.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "business_profile",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["active"]),
        Index(value = ["synced"])  // Performance optimization for sync queries
    ]
)
data class BusinessEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,
    @ColumnInfo(name = "seq_id")
    val seqId: String?,
    val name: String,
    @ColumnInfo(name = "business_type")
    val businessType: String,
    val description: String?,
    @ColumnInfo(name = "owner_name")
    val ownerName: String?,
    @ColumnInfo(name = "address_line1")
    val addressLine1: String?,
    @ColumnInfo(name = "address_line2")
    val addressLine2: String?,
    val city: String?,
    val state: String?,
    @ColumnInfo(name = "postal_code")
    val postalCode: String?,
    val country: String?,
    val latitude: Double?,
    val longitude: Double?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val timezone: String,
    val currency: String,
    val language: String,
    @ColumnInfo(name = "date_format")
    val dateFormat: String,
    @ColumnInfo(name = "time_format")
    val timeFormat: String,
    @ColumnInfo(name = "opening_hours")
    val openingHours: String?,
    @ColumnInfo(name = "closing_hours")
    val closingHours: String?,
    @ColumnInfo(name = "operating_days_json")
    val operatingDaysJson: String?,
    val active: Boolean,
    @ColumnInfo(name = "created_at")
    val createdAt: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: String?,
    @ColumnInfo(name = "created_by")
    val createdBy: String?,
    @ColumnInfo(name = "updated_by")
    val updatedBy: String?,
    @ColumnInfo(name = "custom_attributes_json")
    val customAttributesJson: String?,
    val synced: Boolean,
    @ColumnInfo(name = "last_sync_epoch")
    val lastSyncEpoch: Long,
    @ColumnInfo(name = "local_created_at")
    val localCreatedAt: Long,
    @ColumnInfo(name = "local_updated_at")
    val localUpdatedAt: Long
)
