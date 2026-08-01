package com.ampairs.notification.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.common.di.AppScope
import com.ampairs.common.notification.NotificationPreferencesManager
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Device-local notification preferences (Phase 6). MVI: observes the four preference flows into a
 * single [NotificationSettingsUiState]; intents flip each toggle through
 * [NotificationPreferencesManager].
 *
 * App-scoped because it injects only the app-scoped [NotificationPreferencesManager] (preferences
 * are device-wide, not per-workspace). The announcements topic subscribe/unsubscribe side-effect is
 * driven by `PushTokenRegistrar` (shared), which observes the same preference flow.
 */
data class NotificationSettingsUiState(
    val notificationsEnabled: Boolean = true,
    val orderUpdates: Boolean = true,
    val invoiceUpdates: Boolean = true,
    val announcements: Boolean = true,
)

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class NotificationSettingsViewModel(
    private val preferences: NotificationPreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsUiState())
    val uiState: StateFlow<NotificationSettingsUiState> = _uiState.asStateFlow()

    init {
        combine(
            preferences.notificationsEnabled,
            preferences.notifyOrderUpdates,
            preferences.notifyInvoiceUpdates,
            preferences.notifyAnnouncements,
        ) { master, orders, invoices, announcements ->
            NotificationSettingsUiState(
                notificationsEnabled = master,
                orderUpdates = orders,
                invoiceUpdates = invoices,
                announcements = announcements,
            )
        }
            .onEach { state -> _uiState.value = state }
            .launchIn(viewModelScope)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotificationsEnabled(enabled) }
    }

    fun setOrderUpdates(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotifyOrderUpdates(enabled) }
    }

    fun setInvoiceUpdates(enabled: Boolean) {
        viewModelScope.launch { preferences.setNotifyInvoiceUpdates(enabled) }
    }

    fun setAnnouncements(enabled: Boolean) {
        // Persisting flips the FCM "announcements" topic subscription via PushTokenRegistrar's
        // observer on the same preference flow.
        viewModelScope.launch { preferences.setNotifyAnnouncements(enabled) }
    }
}
