package com.ampairs.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.db.UserRepository
import com.ampairs.auth.db.entity.UserEntity
import com.ampairs.auth.domain.AuthMethod
import com.ampairs.auth.domain.FirebaseAuthResult
import com.ampairs.auth.domain.LoginStatus
import com.ampairs.auth.firebase.FirebaseAuthRepository
import com.ampairs.common.coroutines.DispatcherProvider
import com.ampairs.common.firebase.analytics.AnalyticsEvents
import com.ampairs.common.firebase.analytics.AnalyticsParams
import com.ampairs.common.firebase.analytics.FirebaseAnalytics
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.auth_progress_verifying_recaptcha
import ampairsapp.feature.auth.generated.resources.auth_progress_resend_otp
import ampairsapp.feature.auth.generated.resources.auth_progress_sending_code
import ampairsapp.feature.auth.generated.resources.auth_progress_verifying_code
import ampairsapp.feature.auth.generated.resources.auth_progress_resending_code
import ampairsapp.feature.auth.generated.resources.auth_progress_completing
import ampairsapp.feature.auth.generated.resources.auth_error_authentication_failed
import ampairsapp.feature.auth.generated.resources.auth_error_resend_otp_failed

data class LoginUiState(
    val phoneNumber: String = "",
    val otp: String = "",
    val sessionId: String = "",
    val validPhoneNumber: Boolean = true,
    val loading: Boolean = false,
    val recaptchaLoading: Boolean = false,
    val progressMessage: String = "",
    val firebaseVerificationId: String = "",
    val authMethod: AuthMethod = AuthMethod.BACKEND_API,
    val existingUser: UserEntity? = null,
)

sealed interface LoginNavEvent {
    data object LoginSuccess : LoginNavEvent
    data object NavigateToWorkspace : LoginNavEvent
    data object NavigateToUserUpdate : LoginNavEvent
    data object NavigateToAccountRestore : LoginNavEvent
    data object NavigateToAuthRoute : LoginNavEvent
    data object NotLoggedIn : LoginNavEvent
    data class NavigateToOtp(val sessionId: String, val verificationId: String) : LoginNavEvent
    data object AuthComplete : LoginNavEvent
    data object OtpResent : LoginNavEvent
    data object FirebaseOtpResent : LoginNavEvent
}

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class LoginViewModel(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
    private val firebaseAuthRepository: FirebaseAuthRepository,
    private val analytics: FirebaseAnalytics,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginUiState(
            authMethod = if (firebaseAuthRepository.isSupported()) AuthMethod.FIREBASE else AuthMethod.BACKEND_API
        )
    )
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _navEvent = MutableSharedFlow<LoginNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<LoginNavEvent> = _navEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    val firebaseVerificationState = firebaseAuthRepository.verificationState

    fun updatePhoneNumber(phone: String) {
        _state.update { it.copy(phoneNumber = phone) }
    }

    fun updateOtp(otp: String) {
        _state.update { it.copy(otp = otp) }
    }

    fun updateValidPhoneNumber(valid: Boolean) {
        _state.update { it.copy(validPhoneNumber = valid) }
    }

    fun initOtpScreen(sessionId: String, verificationId: String, phoneNumber: String) {
        _state.update {
            it.copy(
                sessionId = sessionId,
                firebaseVerificationId = verificationId,
                phoneNumber = if (phoneNumber.isNotEmpty()) phoneNumber else it.phoneNumber,
            )
        }
    }

    fun checkUserLogin() {
        viewModelScope.launch(DispatcherProvider.io) {
            val token = userRepository.getToken()
            if (token == null || token.refreshToken.isEmpty() || token.accessToken.isEmpty()) {
                _navEvent.emit(LoginNavEvent.NotLoggedIn)
                return@launch
            }
            val userEntity = userRepository.getUser()
            if (userEntity == null) {
                val apiResult = userRepository.getUserApi()
                if (apiResult.data != null && apiResult.error == null) {
                    val userData = apiResult.data!!
                    userRepository.saveUser(userData)
                    tokenRepository.addAuthenticatedUser(userData.id, token.accessToken, token.refreshToken)
                    tokenRepository.setCurrentUser(userData.id)
                    val savedUserEntity = userRepository.getUserById(userData.id)
                    delay(1000)
                    handlePostLoginNavigation(savedUserEntity)
                } else {
                    _navEvent.emit(LoginNavEvent.NavigateToAuthRoute)
                }
            } else {
                tokenRepository.setCurrentUser(userEntity.id)
                handlePostLoginNavigation(userEntity)
            }
        }
    }

    fun checkExistingUser(onNoExistingUser: () -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            val user = userRepository.findExistingUser(countryCode = 91, phone = _state.value.phoneNumber)
            withContext(Dispatchers.Main) {
                if (user != null) {
                    _state.update { it.copy(existingUser = user) }
                } else {
                    _state.update { it.copy(existingUser = null) }
                    onNoExistingUser()
                }
            }
        }
    }

    fun clearExistingUser() {
        _state.update { it.copy(existingUser = null) }
    }

    fun selectExistingUser(userId: String) {
        viewModelScope.launch(DispatcherProvider.io) {
            tokenRepository.setCurrentUser(userId)
            withContext(Dispatchers.Main) {
                handleExistingUserWorkspaceCheck()
            }
        }
    }

    fun authenticate() {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_verifying_recaptcha)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, recaptchaLoading = true, progressMessage = progressMsg) }
            }
            tokenRepository.createDummyUserSession()
            val result = userRepository.initAuth(_state.value.phoneNumber)
            if (result.data != null && result.error == null) {
                val data = result.data!!
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(loading = false, recaptchaLoading = false, progressMessage = "") }
                    if (data.success && data.sessionId != null) {
                        _state.update { it.copy(sessionId = data.sessionId) }
                        _navEvent.emit(LoginNavEvent.NavigateToOtp(data.sessionId, ""))
                    } else {
                        val errMsg = data.error?.message ?: getString(Res.string.auth_error_authentication_failed)
                        _snackbarMessage.emit(errMsg)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(loading = false, recaptchaLoading = false, progressMessage = "") }
                    val errMsg = result.error?.message ?: ""
                    if (errMsg.isNotEmpty()) _snackbarMessage.emit(errMsg)
                }
            }
        }
    }

    fun handlePostLoginNavigation(userEntity: UserEntity?) {
        viewModelScope.launch {
            if (userEntity?.first_name.isNullOrBlank()) {
                _navEvent.emit(LoginNavEvent.NavigateToUserUpdate)
            } else {
                val userId = userEntity!!.id
                val hasWorkspace = userWorkspaceRepository.getWorkspaceIdForUser(userId).isNotBlank()
                _navEvent.emit(if (hasWorkspace) LoginNavEvent.LoginSuccess else LoginNavEvent.NavigateToWorkspace)
            }
        }
    }

    fun handleOtpSuccess() {
        viewModelScope.launch {
            try {
                val deletionStatusResponse = userRepository.getAccountDeletionStatus()
                var navigateToRestore = false
                deletionStatusResponse.onSuccess { navigateToRestore = isDeleted && canRestore }
                _navEvent.emit(
                    if (navigateToRestore) LoginNavEvent.NavigateToAccountRestore
                    else LoginNavEvent.NavigateToUserUpdate
                )
            } catch (_: Exception) {
                _navEvent.emit(LoginNavEvent.NavigateToUserUpdate)
            }
        }
    }

    fun handleExistingUserWorkspaceCheck() {
        viewModelScope.launch {
            val currentUserId = tokenRepository.getCurrentUserId() ?: return@launch
            val hasWorkspace = userWorkspaceRepository.getWorkspaceIdForUser(currentUserId).isNotBlank()
            _navEvent.emit(if (hasWorkspace) LoginNavEvent.LoginSuccess else LoginNavEvent.NavigateToWorkspace)
        }
    }

    fun completeAuthentication() {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_verifying_recaptcha)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, recaptchaLoading = true, progressMessage = progressMsg) }
            }
            val authResult = userRepository.completeAuth(_state.value.sessionId, _state.value.otp)
            if (authResult.data == null || authResult.error != null) {
                withContext(Dispatchers.Main) {
                    val errMsg = authResult.error?.message ?: ""
                    _state.update { it.copy(loading = false, recaptchaLoading = false, progressMessage = "") }
                    if (errMsg.isNotEmpty()) _snackbarMessage.emit(errMsg)
                }
                return@launch
            }
            val authData = authResult.data!!
            tokenRepository.updateToken(authData.accessToken, authData.refreshToken)

            val userResult = userRepository.getUserApi()
            if (userResult.data != null && userResult.error == null) {
                val userData = userResult.data!!
                val isNewUser = userRepository.getUserById(userData.id) == null
                userRepository.saveUser(userData)
                tokenRepository.updateDummySessionWithRealUser(
                    userData.id, authData.accessToken, authData.refreshToken
                )
                analytics.setUserId(userData.id)
                analytics.logEvent(
                    if (isNewUser) AnalyticsEvents.SIGN_UP else AnalyticsEvents.LOGIN,
                    mapOf(AnalyticsParams.METHOD to "backend_api")
                )
            }
            withContext(Dispatchers.Main) {
                delay(1000)
                _state.update { it.copy(loading = false, recaptchaLoading = false, progressMessage = "") }
                _navEvent.emit(LoginNavEvent.AuthComplete)
            }
        }
    }

    fun resendOtp() {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_resend_otp)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, recaptchaLoading = true, progressMessage = progressMsg) }
            }
            val result = userRepository.resendOtp(_state.value.phoneNumber)
            if (result.data != null && result.error == null) {
                val data = result.data!!
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(loading = false, recaptchaLoading = false, progressMessage = "") }
                    if (data.success && data.sessionId != null) {
                        _state.update { it.copy(sessionId = data.sessionId) }
                        _navEvent.emit(LoginNavEvent.OtpResent)
                    } else {
                        val errMsg = data.error?.message ?: getString(Res.string.auth_error_resend_otp_failed)
                        _snackbarMessage.emit(errMsg)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(loading = false, recaptchaLoading = false, progressMessage = "") }
                    val errMsg = result.error?.message ?: ""
                    if (errMsg.isNotEmpty()) _snackbarMessage.emit(errMsg)
                }
            }
        }
    }

    // ========== Firebase Authentication Methods ==========

    fun authenticateWithFirebase() {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_sending_code)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, progressMessage = progressMsg) }
            }
            val countryCode = "91"
            when (val result = firebaseAuthRepository.sendOtp(countryCode, _state.value.phoneNumber)) {
                is FirebaseAuthResult.Success -> {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(firebaseVerificationId = result.data, loading = false, progressMessage = "") }
                        _navEvent.emit(LoginNavEvent.NavigateToOtp("", result.data))
                    }
                }
                is FirebaseAuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(loading = false, progressMessage = "") }
                        _snackbarMessage.emit(result.message)
                    }
                }
                FirebaseAuthResult.Loading -> Unit
            }
        }
    }

    fun completeFirebaseAuthentication() {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_verifying_code)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, progressMessage = progressMsg) }
            }
            tokenRepository.createDummyUserSession()
            when (val result = firebaseAuthRepository.verifyOtp(_state.value.firebaseVerificationId, _state.value.otp)) {
                is FirebaseAuthResult.Success -> {
                    val firebaseIdToken = result.data
                    val backendResult = userRepository.verifyFirebaseAuth(firebaseIdToken, _state.value.phoneNumber)
                    if (backendResult.data == null || backendResult.error != null) {
                        withContext(Dispatchers.Main) {
                            _state.update { it.copy(loading = false, progressMessage = "") }
                            val errMsg = backendResult.error?.message ?: ""
                            if (errMsg.isNotEmpty()) _snackbarMessage.emit(errMsg)
                        }
                        return@launch
                    }
                    val authData = backendResult.data!!
                    tokenRepository.updateToken(authData.accessToken, authData.refreshToken)
                    val userResult = userRepository.getUserApi()
                    if (userResult.data != null && userResult.error == null) {
                        val userData = userResult.data!!
                        val isNewUser = userRepository.getUserById(userData.id) == null
                        userRepository.saveUser(userData)
                        tokenRepository.updateDummySessionWithRealUser(
                            userData.id, authData.accessToken, authData.refreshToken
                        )
                        analytics.setUserId(userData.id)
                        analytics.logEvent(
                            if (isNewUser) AnalyticsEvents.SIGN_UP else AnalyticsEvents.LOGIN,
                            mapOf(AnalyticsParams.METHOD to "firebase_phone")
                        )
                    }
                    withContext(Dispatchers.Main) {
                        delay(1000)
                        _state.update { it.copy(loading = false, progressMessage = "") }
                        _navEvent.emit(LoginNavEvent.AuthComplete)
                    }
                }
                is FirebaseAuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(loading = false, progressMessage = "") }
                        _snackbarMessage.emit(result.message)
                    }
                }
                FirebaseAuthResult.Loading -> Unit
            }
        }
    }

    fun resendFirebaseOtp() {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_resending_code)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, progressMessage = progressMsg) }
            }
            val countryCode = "91"
            when (val result = firebaseAuthRepository.resendOtp(countryCode, _state.value.phoneNumber)) {
                is FirebaseAuthResult.Success -> {
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(firebaseVerificationId = result.data, loading = false, progressMessage = "")
                        }
                        _navEvent.emit(LoginNavEvent.FirebaseOtpResent)
                    }
                }
                is FirebaseAuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(loading = false, progressMessage = "") }
                        _snackbarMessage.emit(result.message)
                    }
                }
                FirebaseAuthResult.Loading -> Unit
            }
        }
    }

    fun completeFirebaseAuthenticationWithToken(firebaseIdToken: String) {
        if (_state.value.loading) return
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_completing)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, progressMessage = progressMsg) }
            }
            tokenRepository.createDummyUserSession()
            val backendResult = userRepository.verifyFirebaseAuth(firebaseIdToken, _state.value.phoneNumber)
            if (backendResult.data == null || backendResult.error != null) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(loading = false, progressMessage = "") }
                    val errMsg = backendResult.error?.message ?: ""
                    if (errMsg.isNotEmpty()) _snackbarMessage.emit(errMsg)
                }
                return@launch
            }
            val authData = backendResult.data!!
            tokenRepository.updateToken(authData.accessToken, authData.refreshToken)
            val userResult = userRepository.getUserApi()
            if (userResult.data != null && userResult.error == null) {
                val userData = userResult.data!!
                val isNewUser = userRepository.getUserById(userData.id) == null
                userRepository.saveUser(userData)
                tokenRepository.updateDummySessionWithRealUser(
                    userData.id, authData.accessToken, authData.refreshToken
                )
                analytics.setUserId(userData.id)
                analytics.logEvent(
                    if (isNewUser) AnalyticsEvents.SIGN_UP else AnalyticsEvents.LOGIN,
                    mapOf(AnalyticsParams.METHOD to "firebase_phone_auto")
                )
            }
            withContext(Dispatchers.Main) {
                delay(1000)
                _state.update { it.copy(loading = false, progressMessage = "") }
                _navEvent.emit(LoginNavEvent.AuthComplete)
            }
        }
    }

    // ========== Desktop Browser Authentication Methods ==========

    fun handleBrowserAuthTokens(
        accessToken: String,
        refreshToken: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch(DispatcherProvider.io) {
            val progressMsg = getString(Res.string.auth_progress_completing)
            withContext(Dispatchers.Main) {
                _state.update { it.copy(loading = true, progressMessage = progressMsg) }
            }
            try {
                tokenRepository.createDummyUserSession()
                tokenRepository.updateToken(accessToken, refreshToken)
                val userResult = userRepository.getUserApi()
                if (userResult.data != null && userResult.error == null) {
                    val userData = userResult.data!!
                    val isNewUser = userRepository.getUserById(userData.id) == null
                    userRepository.saveUser(userData)
                    tokenRepository.updateDummySessionWithRealUser(userData.id, accessToken, refreshToken)
                    analytics.setUserId(userData.id)
                    analytics.logEvent(
                        if (isNewUser) AnalyticsEvents.SIGN_UP else AnalyticsEvents.LOGIN,
                        mapOf(AnalyticsParams.METHOD to "desktop_browser")
                    )
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(loading = false, progressMessage = "") }
                        delay(500)
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _state.update { it.copy(loading = false, progressMessage = "") }
                        onError("Failed to fetch user information: ${userResult.error?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(loading = false, progressMessage = "") }
                    onError("Authentication error: ${e.message}")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _state.update { it.copy(loading = false) }
    }
}
