package com.ampairs.store.domain

import com.ampairs.store.data.repository.StoreSettingRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Read-side accessor other feature modules use to consume settings without touching Room directly.
 * Resolves the effective value: the synced override if present, otherwise the caller-supplied
 * default. The caller owns the default (colocated with the call site) so business logic works
 * offline before any override or definition has synced.
 */
@Inject
class StoreSettingsProvider(
    private val repository: StoreSettingRepository,
) {

    suspend fun getString(module: String, key: String, default: String? = null): String? =
        repository.getByModuleKey(module, key)?.takeIf { it.active }?.value ?: default

    suspend fun getBoolean(module: String, key: String, default: Boolean = false): Boolean =
        getString(module, key)?.equals("true", ignoreCase = true) ?: default

    fun observeBoolean(module: String, key: String, default: Boolean = false): Flow<Boolean> =
        repository.observeSettings().map { settings ->
            settings.firstOrNull { it.module == module && it.key == key && it.active }
                ?.value?.equals("true", ignoreCase = true) ?: default
        }

    fun observeString(module: String, key: String, default: String? = null): Flow<String?> =
        repository.observeSettings().map { settings ->
            settings.firstOrNull { it.module == module && it.key == key && it.active }?.value ?: default
        }
}
