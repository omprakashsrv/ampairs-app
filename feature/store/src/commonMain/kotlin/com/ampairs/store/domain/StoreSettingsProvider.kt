package com.ampairs.store.domain

import com.ampairs.store.data.repository.StoreSettingRepository
import com.ampairs.store.domain.definition.StoreSettingDefinitions
import com.ampairs.store.domain.model.StoreSetting
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-side accessor other feature modules use to consume settings without touching Room directly.
 * Resolves the effective value: the synced override if present, otherwise the code-declared default.
 *
 * Inject this into a consuming ViewModel (e.g. invoice/order) and call [getBoolean]/[getString] or
 * observe reactively via [observeBoolean].
 */
@Inject
class StoreSettingsProvider(
    private val repository: StoreSettingRepository,
) {

    suspend fun getString(module: String, key: String): String? {
        val override = repository.getByModuleKey(module, key)?.takeIf { it.active }?.value
        return override ?: StoreSettingDefinitions.find(module, key)?.defaultValue
    }

    suspend fun getBoolean(module: String, key: String): Boolean =
        getString(module, key)?.equals("true", ignoreCase = true) ?: false

    fun observeBoolean(module: String, key: String): Flow<Boolean> =
        repository.observeSettings().map { settings ->
            resolve(settings, module, key)?.equals("true", ignoreCase = true) ?: false
        }

    fun observeString(module: String, key: String): Flow<String?> =
        repository.observeSettings().map { settings -> resolve(settings, module, key) }

    private fun resolve(settings: List<StoreSetting>, module: String, key: String): String? {
        val override = settings.firstOrNull { it.module == module && it.key == key && it.active }?.value
        return override ?: StoreSettingDefinitions.find(module, key)?.defaultValue
    }
}
