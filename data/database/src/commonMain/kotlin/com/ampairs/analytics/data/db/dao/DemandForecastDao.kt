package com.ampairs.analytics.data.db.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query
import com.ampairs.analytics.data.db.entity.DemandForecastEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the pull-only [DemandForecastEntity] mirror.
 *
 * Reactive Flow queries drive the UI; suspend queries serve the sync delegate. There is no local
 * mutation path (no `synced` flag) — the server is the sole source of truth.
 */
@Dao
interface DemandForecastDao {

    /** All forecasts, newest period first — dashboard list. */
    @Query("SELECT * FROM demand_forecast ORDER BY period_start DESC")
    fun observeAll(): Flow<List<DemandForecastEntity>>

    /** Forecasts for one product, newest period first. */
    @Query("SELECT * FROM demand_forecast WHERE product_id = :productId ORDER BY period_start DESC")
    fun observeForProduct(productId: String): Flow<List<DemandForecastEntity>>

    /**
     * The most recent forecast per product (highest `period_start`), highest expected demand first —
     * backs the dashboard forecast section (feature 022, T045). Bounded by [limit].
     */
    @Query(
        """
        SELECT f.* FROM demand_forecast f
        WHERE f.period_start = (
            SELECT MAX(f2.period_start) FROM demand_forecast f2 WHERE f2.product_id = f.product_id
        )
        ORDER BY f.mean_qty DESC
        LIMIT :limit
        """,
    )
    suspend fun latestPerProduct(limit: Int): List<DemandForecastEntity>

    @Query("SELECT * FROM demand_forecast WHERE uid = :uid")
    suspend fun getByUid(uid: String): DemandForecastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(forecast: DemandForecastEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(forecasts: List<DemandForecastEntity>)

    /** Permanently remove a forecast the server reports as deleted. */
    @Query("DELETE FROM demand_forecast WHERE uid = :uid")
    suspend fun deleteByUid(uid: String)

    /** Highest `updated_at` seen locally — the incremental pull checkpoint fallback. */
    @Query("SELECT MAX(updated_at) FROM demand_forecast")
    suspend fun maxUpdatedAt(): String?

    @Query("DELETE FROM demand_forecast")
    suspend fun clear()
}
