package com.ampairs.inventory.domain

import com.ampairs.common.id_generator.UidGenerator
import com.ampairs.inventory.util.InventoryConstants
import com.ampairs.store.data.repository.StoreSettingRepository
import com.ampairs.store.domain.StoreSettingsProvider
import com.ampairs.store.domain.model.StoreSetting
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Read/write accessor for the five inventory behaviour flags, layered on the shared store-settings
 * mechanism (`module = "inventory"`). Reads resolve the synced override or the caller-owned default;
 * writes upsert a [StoreSetting] and ride the automatic `SyncEntity.STORE` push. Order/Invoice can read
 * the same keys directly through [StoreSettingsProvider] for their auto-deduct / block-out-of-stock flows.
 */
@Inject
class InventorySettingsProvider(
    private val settingsProvider: StoreSettingsProvider,
    private val settingRepository: StoreSettingRepository,
) {
    private val module = InventoryConstants.SETTINGS_MODULE

    fun observe(): Flow<InventorySettings> = combine(
        settingsProvider.observeBoolean(module, InventoryConstants.FLAG_AUTO_DEDUCT_ON_SALE, InventoryConstants.DEFAULT_AUTO_DEDUCT_ON_SALE),
        settingsProvider.observeBoolean(module, InventoryConstants.FLAG_BLOCK_ORDERS_WHEN_OUT, InventoryConstants.DEFAULT_BLOCK_ORDERS_WHEN_OUT),
        settingsProvider.observeBoolean(module, InventoryConstants.FLAG_ALLOW_NEGATIVE_STOCK, InventoryConstants.DEFAULT_ALLOW_NEGATIVE_STOCK),
        settingsProvider.observeBoolean(module, InventoryConstants.FLAG_ALLOW_MANUAL_ADJUSTMENTS, InventoryConstants.DEFAULT_ALLOW_MANUAL_ADJUSTMENTS),
        settingsProvider.observeBoolean(module, InventoryConstants.FLAG_LOW_STOCK_ALERTS, InventoryConstants.DEFAULT_LOW_STOCK_ALERTS),
    ) { autoDeduct, blockOut, allowNeg, allowManual, lowAlerts ->
        InventorySettings(
            autoDeductOnSale = autoDeduct,
            blockOrdersWhenOut = blockOut,
            allowNegativeStock = allowNeg,
            allowManualAdjustments = allowManual,
            lowStockAlerts = lowAlerts,
        )
    }

    suspend fun allowNegativeStock(): Boolean =
        settingsProvider.getBoolean(module, InventoryConstants.FLAG_ALLOW_NEGATIVE_STOCK, InventoryConstants.DEFAULT_ALLOW_NEGATIVE_STOCK)

    suspend fun allowManualAdjustments(): Boolean =
        settingsProvider.getBoolean(module, InventoryConstants.FLAG_ALLOW_MANUAL_ADJUSTMENTS, InventoryConstants.DEFAULT_ALLOW_MANUAL_ADJUSTMENTS)

    /** Persist one flag (offline-first upsert through the store-settings push). */
    suspend fun setFlag(key: String, value: Boolean): Result<Unit> {
        val existing = settingRepository.getByModuleKey(module, key)
        val setting = StoreSetting(
            uid = existing?.uid?.takeIf { it.isNotBlank() }
                ?: UidGenerator.generateUid(InventoryConstants.SETTING_UID_PREFIX),
            module = module,
            key = key,
            value = value.toString(),
            valueType = "BOOLEAN",
            active = true,
            refId = existing?.refId,
            createdAt = existing?.createdAt,
            updatedAt = existing?.updatedAt,
        )
        return settingRepository.upsert(setting).map { }
    }
}
