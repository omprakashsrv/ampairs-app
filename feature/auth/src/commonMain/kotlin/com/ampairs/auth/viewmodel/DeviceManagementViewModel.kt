package com.ampairs.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.DeviceSession
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.auth_error_failed_load_sessions
import ampairsapp.feature.auth.generated.resources.auth_success_logged_out_device
import ampairsapp.feature.auth.generated.resources.auth_error_failed_logout_device
import ampairsapp.feature.auth.generated.resources.auth_success_logged_out_all
import ampairsapp.feature.auth.generated.resources.auth_error_failed_logout_all

data class DeviceManagementUiState(
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val deviceSessions: List<DeviceSession> = emptyList()
)

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class DeviceManagementViewModel(
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceManagementUiState())
    val uiState: StateFlow<DeviceManagementUiState> = _uiState.asStateFlow()

    private val _errorMessage = MutableSharedFlow<String?>(extraBufferCapacity = 1)
    val errorMessage = _errorMessage.asSharedFlow()

    fun loadDeviceSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = userRepository.getDeviceSessions()
            if (result.data != null && result.error == null) {
                _uiState.value = _uiState.value.copy(isLoading = false, deviceSessions = result.data!!)
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
                _errorMessage.emit(getString(Res.string.auth_error_failed_load_sessions, result.error?.message.orEmpty()))
            }
        }
    }

    fun logoutDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            val result = userRepository.logoutDevice(deviceId)
            if (result.data != null && result.error == null) {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                _errorMessage.emit(getString(Res.string.auth_success_logged_out_device))
                loadDeviceSessions()
            } else {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                _errorMessage.emit(getString(Res.string.auth_error_failed_logout_device, result.error?.message.orEmpty()))
            }
        }
    }

    fun logoutAllDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            val result = userRepository.logoutAllDevices()
            if (result.data != null && result.error == null) {
                _uiState.value = _uiState.value.copy(isLoggingOut = false, deviceSessions = emptyList())
                _errorMessage.emit(getString(Res.string.auth_success_logged_out_all))
            } else {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                _errorMessage.emit(getString(Res.string.auth_error_failed_logout_all, result.error?.message.orEmpty()))
            }
        }
    }

    fun clearErrorMessage() {
        viewModelScope.launch { _errorMessage.emit(null) }
    }
}
