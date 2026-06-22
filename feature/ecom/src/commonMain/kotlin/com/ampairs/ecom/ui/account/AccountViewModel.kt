package com.ampairs.ecom.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserDataService
import com.ampairs.auth.api.isAuthenticated
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.ecom.data.repository.CartRepository
import com.ampairs.ecom.domain.EcomSession
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountUiState(
    val isLoggedIn: Boolean = false,
    val displayName: String = "",
    val storeName: String = "",
)

sealed interface AccountEvent {
    data object NavigateLogin : AccountEvent
    data object LoggedOut : AccountEvent
}

@Inject
@ContributesIntoMap(WorkspaceScope::class)
@ViewModelKey
class AccountViewModel(
    private val tokenRepository: TokenRepository,
    private val userDataService: UserDataService,
    private val session: EcomSession,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<AccountEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<AccountEvent> = _events.asSharedFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoggedIn = tokenRepository.isAuthenticated(),
                    displayName = userDataService.getUserDisplayName().orEmpty(),
                    storeName = session.active.value?.slug.orEmpty(),
                )
            }
        }
    }

    fun login() {
        _events.tryEmit(AccountEvent.NavigateLogin)
    }

    fun logout() {
        tokenRepository.clearTokens()
        // Wipe local carts so the next user doesn't inherit the previous user's cart.
        viewModelScope.launch { cartRepository.clearAllLocalCarts() }
        session.clear()
        _state.update { it.copy(isLoggedIn = false, displayName = "") }
        _events.tryEmit(AccountEvent.LoggedOut)
    }
}
