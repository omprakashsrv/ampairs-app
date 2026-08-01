package com.ampairs.analytics.data.settings

import com.ampairs.analytics.domain.DashboardTile
import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.store.data.repository.StoreSettingRepository
import com.ampairs.store.domain.StoreSettingsProvider
import com.ampairs.store.domain.model.StoreSetting
import com.ampairs.store.util.StoreConstants
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read/write wrapper for the dashboard-layout workspace setting (feature 022, T050), mirroring
 * `InventorySettingsProvider`. Reads decode the CSV via [DashboardTile]; the write persists a single
 * `analytics/dashboard_layout` [StoreSetting] override (reusing the existing row's uid so it overwrites
 * rather than duplicating) and rides the standard `SyncEntity.STORE` push.
 */
@Inject
class AnalyticsDashboardSettings(
    private val settingsProvider: StoreSettingsProvider,
    private val repository: StoreSettingRepository,
) {

    /** The enabled KPI tiles in display order; defaults to the full canonical layout. */
    fun observeLayout(): Flow<List<DashboardTile>> =
        settingsProvider.observeString(MODULE, KEY).map { DashboardTile.decode(it) }

    /** Persist add/remove/reorder as the ordered CSV of enabled tile keys. */
    suspend fun saveLayout(tiles: List<DashboardTile>): Result<Unit> {
        val existing = repository.getByModuleKey(MODULE, KEY)
        val setting = StoreSetting(
            uid = existing?.uid?.takeIf { it.isNotBlank() }
                ?: UidGenerator.generateUid(StoreConstants.SETTING_UID_PREFIX),
            module = MODULE,
            key = KEY,
            value = DashboardTile.encode(tiles),
            valueType = "STRING",
            active = true,
            refId = existing?.refId,
            createdAt = existing?.createdAt,
            updatedAt = existing?.updatedAt,
        )
        return repository.upsert(setting).map { }
    }

    private companion object {
        const val MODULE = "analytics"
        const val KEY = "dashboard_layout"
    }
}
