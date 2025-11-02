package com.ampairs.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.domain.DeviceSession
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeviceManagementUiState(
    val isLoading: Boolean = false,
    val isLoggingOut: Boolean = false,
    val deviceSessions: List<DeviceSession> = emptyList()
)

class DeviceManagementViewModel(
    private val userRepository: UserRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DeviceManagementUiState())
    val uiState: StateFlow<DeviceManagementUiState> = _uiState.asStateFlow()
    
    private val _errorMessage = MutableSharedFlow<String?>()
    val errorMessage = _errorMessage.asSharedFlow()
    
    fun loadDeviceSessions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            userRepository.getDeviceSessions().onSuccess {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    deviceSessions = this
                )
            }.onError {
                _uiState.value = _uiState.value.copy(isLoading = false)
                viewModelScope.launch {
                    _errorMessage.emit("Failed to load device sessions: ${this@onError.message}")
                }
            }
        }
    }
    
    fun logoutDevice(deviceId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            
            userRepository.logoutDevice(deviceId).onSuccess {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                viewModelScope.launch {
                    _errorMessage.emit("Successfully logged out from device")
                }
                // Refresh the device list
                loadDeviceSessions()
            }.onError {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                viewModelScope.launch {
                    _errorMessage.emit("Failed to logout device: ${this@onError.message}")
                }
            }
        }
    }
    
    fun logoutAllDevices() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoggingOut = true)
            
            userRepository.logoutAllDevices().onSuccess {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                viewModelScope.launch {
                    _errorMessage.emit("Successfully logged out from all devices")
                }
                // This should trigger a logout from the current session as well
                // Clear the device sessions since we're logged out
                _uiState.value = _uiState.value.copy(deviceSessions = emptyList())
            }.onError {
                _uiState.value = _uiState.value.copy(isLoggingOut = false)
                viewModelScope.launch {
                    _errorMessage.emit("Failed to logout from all devices: ${this@onError.message}")
                }
            }
        }
    }
    
    fun clearErrorMessage() {
        viewModelScope.launch {
            _errorMessage.emit(null)
        }
    }
}