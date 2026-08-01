package com.ampairs.analytics.data.db.entity

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * Room mirror of a server-generated demand forecast (feature 022, PULL-ONLY).
 *
 * Table: demand_forecast. Forecasts are computed on the backend and pulled read-only; there is no
 * local mutation and no push (the sync delegate's push is a no-op). `updated_at` drives the
 * incremental pull checkpoint.
 */
@Entity(
    tableName = "demand_forecast",
    indices = [
        Index(value = ["uid"], unique = true, name = "demand_forecast_uid_idx"),
        Index(value = ["product_id"], name = "demand_forecast_product_id_idx"),
        Index(value = ["updated_at"], name = "demand_forecast_updated_at_idx"),
    ],
)
data class DemandForecastEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "product_id")
    val productId: String,

    @ColumnInfo(name = "period_start")
    val periodStart: String,

    @ColumnInfo(name = "horizon")
    val horizon: Int,

    @ColumnInfo(name = "mean_qty")
    val meanQty: Double,

    @ColumnInfo(name = "std_dev_qty")
    val stdDevQty: Double,

    @ColumnInfo(name = "method")
    val method: String,

    @ColumnInfo(name = "confidence")
    val confidence: String,

    @ColumnInfo(name = "generated_at")
    val generatedAt: String? = null,

    @ColumnInfo(name = "updated_at")
    val updatedAt: String? = null,
)
