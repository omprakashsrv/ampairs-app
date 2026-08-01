package com.ampairs.ecom.ui.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.api.model.AccessMode
import com.ampairs.ecom.api.model.StoreAccessStatus
import com.ampairs.ecom.api.model.StorefrontAccessMode
import com.ampairs.ecom.api.model.StorefrontStatus
import com.ampairs.ecom.data.repository.StoreAccessRepository
import com.ampairs.ecom.data.repository.StorefrontRepository
import com.ampairs.ecom.domain.EcomLogger
import com.ampairs.ecom.domain.EcomSession
import com.ampairs.sync.CentralSyncService
import com.ampairs.sync.SyncEntity
import com.ampairs.sync.SyncEvent
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Resolves the storefront entry: bootstrap branding, then branch on the access model
 * (GUEST_FIRST → shop; LOGIN_FIRST → require auth + store access). See plan §4.
 */
@AssistedInject
class StorefrontGateViewModel(
    @Assisted private val slug: String,
    private val storefrontRepository: StorefrontRepository,
    private val storeAccessRepository: StoreAccessRepository,
    private val tokenRepository: TokenRepository,
    private val session: EcomSession,
    private val syncService: CentralSyncService,
) : ViewModel() {

    private val _state = MutableStateFlow(GateUiState())
    val state: StateFlow<GateUiState> = _state.asStateFlow()

    init { resolve() }

    fun resolve() {
        _state.update { it.copy(phase = GatePhase.Loading, error = null) }
        viewModelScope.launch {
            storefrontRepository.bootstrap(slug).fold(
                onSuccess = { storefront ->
                    if (storefront.status != StorefrontStatus.PUBLISHED.name) {
                        _state.update { it.copy(phase = GatePhase.StoreUnavailable) }
                        return@fold
                    }
                    session.open(slug = storefront.slug, storefrontId = storefront.uid)
                    val mode = parseAccessMode(storefront.accessMode)
                    if (mode == AccessMode.GUEST_FIRST) {
                        enterShop()
                    } else {
                        resolveLoginFirst()
                    }
                },
                onFailure = { e ->
                    EcomLogger.w("Gate", "bootstrap failed", e)
                    _state.update { it.copy(phase = GatePhase.StoreUnavailable, error = e.message) }
                },
            )
        }
    }

    private suspend fun resolveLoginFirst() {
        val token = tokenRepository.getAccessToken()
        if (token.isNullOrBlank()) {
            _state.update { it.copy(phase = GatePhase.RequireLogin) }
            return
        }
        val access = storeAccessRepository.checkAccess(slug).getOrNull()?.status
        when (access) {
            StoreAccessStatus.GRANTED.name -> enterShop()
            StoreAccessStatus.PENDING.name -> _state.update { it.copy(phase = GatePhase.PendingAccess) }
            else -> _state.update { it.copy(phase = GatePhase.RequestAccess) }
        }
    }

    fun requestAccess() {
        viewModelScope.launch {
            storeAccessRepository.requestAccess(slug)
            _state.update { it.copy(phase = GatePhase.PendingAccess) }
        }
    }

    /** Call after the reused auth flow reports success. */
    fun onLoggedIn() {
        viewModelScope.launch { resolveLoginFirst() }
    }

    private fun enterShop() {
        // Kick a background catalog refresh as we enter (contract §9).
        syncService.emit(SyncEvent.TriggerPull(SyncEntity.ECOM_PRODUCT))
        _state.update { it.copy(phase = GatePhase.Shop) }
    }

    // The bootstrap payload's `access_mode` is the same backend field the merchant sets via the
    // storefront management API (StorefrontAccessMode.PUBLIC/RESTRICTED) — not a literal
    // GUEST_FIRST/LOGIN_FIRST value. Map it into the gate's own AccessMode so RESTRICTED actually
    // routes into resolveLoginFirst(). A literal GUEST_FIRST/LOGIN_FIRST is still honored if the
    // backend ever sends that instead.
    private fun parseAccessMode(raw: String?): AccessMode {
        if (raw == null) return AccessMode.GUEST_FIRST
        runCatching { AccessMode.valueOf(raw) }.getOrNull()?.let { return it }
        return when (runCatching { StorefrontAccessMode.valueOf(raw) }.getOrNull()) {
            StorefrontAccessMode.RESTRICTED -> AccessMode.LOGIN_FIRST
            StorefrontAccessMode.PUBLIC, StorefrontAccessMode.UNKNOWN, null -> AccessMode.GUEST_FIRST
        }
    }

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(WorkspaceScope::class)
    fun interface Factory : ManualViewModelAssistedFactory {
        fun create(slug: String): StorefrontGateViewModel
    }
}

enum class GatePhase { Loading, StoreUnavailable, RequireLogin, RequestAccess, PendingAccess, Shop }

data class GateUiState(
    val phase: GatePhase = GatePhase.Loading,
    val error: String? = null,
)
