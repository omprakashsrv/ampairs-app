package com.ampairs.sfa.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

@Entity(
    tableName = "sfa_field_orders",
    indices = [
        Index(value = ["id"], unique = true, name = "sfa_field_order_id_idx"),
        Index(value = ["customer_uid"], name = "sfa_field_order_customer_idx"),
    ],
)
data class FieldOrderEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "visit_uid") val visitUid: String? = null,
    @ColumnInfo(name = "customer_uid") val customerUid: String,
    @ColumnInfo(name = "rep_member_uid") val repMemberUid: String,
    @ColumnInfo(name = "order_uid") val orderUid: String? = null,
    @ColumnInfo(name = "amount") val amount: Double = 0.0,
    @ColumnInfo(name = "active") val active: Boolean = true,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
)
