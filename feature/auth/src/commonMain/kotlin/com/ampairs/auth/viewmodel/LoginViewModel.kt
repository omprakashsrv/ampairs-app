package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface LoginNavEvent {
    object LoginSuccess : LoginNavEvent
    object NavigateToWorkspace : LoginNavEvent
    object NavigateToUserUpdate : LoginNavEvent
    object NavigateToAccountRestore : LoginNavEvent
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

    private val _navEvent = MutableSharedFlow<LoginNavEvent>()
    val navEvent: SharedFlow<LoginNavEvent> = _navEvent.asSharedFlow()
    var phoneNumber by mutableStateOf("")
    var otp by mutableStateOf("")
    var sessionId by mutableStateOf("")
    var validPhoneNumber by mutableStateOf(true)
    var displayMessage by mutableStateOf("")
    var loading by mutableStateOf(false)
    var recaptchaLoading by mutableStateOf(false)
    var progressMessage by mutableStateOf("")
    var loginStatus by mutableStateOf(LoginStatus.INIT)

    var firebaseVerificationId by mutableStateOf("")

    var authMethod by mutableStateOf(
        if (firebaseAuthRepository.isSupported()) AuthMethod.FIREBASE else AuthMethod.BACKEND_API
    )

    val firebaseVerificationState = firebaseAuthRepository.verificationState

    var existingUser by mutableStateOf<UserEntity?>(null)

    fun checkUserLogin(onLoginStatus: (LoginStatus, userEntity: UserEntity?) -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            val token = userRepository.getToken()
            if (token == null) {
                withContext(Dispatchers.Main) {
                    loginStatus = LoginStatus.NOT_LOGGED_IN
                    onLoginStatus(loginStatus, null)
                }
                return@launch
            }
            if (token.refreshToken.isEmpty() || token.accessToken.isEmpty()) {
                withContext(Dispatchers.Main) {
                    loginStatus = LoginStatus.NOT_LOGGED_IN
                    onLoginStatus(loginStatus, null)
                }
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
                    withContext(Dispatchers.Main) {
                        loginStatus = LoginStatus.LOGGED_IN
                        onLoginStatus(loginStatus, savedUserEntity)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loginStatus = LoginStatus.LOGIN_FAILED
                        onLoginStatus(loginStatus, null)
                    }
                }
            } else {
                tokenRepository.setCurrentUser(userEntity.id)
                withContext(Dispatchers.Main) {
                    loginStatus = LoginStatus.LOGGED_IN
                    onLoginStatus(loginStatus, userEntity)
                }
            }
        }
    }

    fun checkExistingUser(onExistingUserFound: (UserEntity) -> Unit, onNoExistingUser: () -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            val user = userRepository.findExistingUser(countryCode = 91, phone = phoneNumber)
            withContext(Dispatchers.Main) {
                if (user != null) {
                    existingUser = user
                    onExistingUserFound(user)
                } else {
                    existingUser = null
                    onNoExistingUser()
                }
            }
        }
    }

    fun selectExistingUser(userId: String, onUserSelected: () -> Unit) {
        viewModelScope.launch(DispatcherProvider.io) {
            tokenRepository.setCurrentUser(userId)
            withContext(Dispatchers.Main) { onUserSelected() }
        }
    }

    fun authenticate(onAuthSuccess: (String) -> Unit) {
        if (loading) return
        loading = true
        recaptchaLoading = true
        progressMessage = "Verifying reCAPTCHA..."

        viewModelScope.launch(DispatcherProvider.io) {
            tokenRepository.createDummyUserSession()
            val result = userRepository.initAuth(phoneNumber)
            if (result.data != null && result.error == null) {
                val data = result.data!!
                withContext(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                    if (data.success && data.sessionId != null) {
                        this@LoginViewModel.sessionId = data.sessionId
                        onAuthSuccess(data.sessionId)
                    } else {
                        displayMessage = data.error?.message ?: "Authentication failed"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    displayMessage = result.error?.message ?: ""
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                }
            }
        }
    }

    fun handlePostLoginNavigation(userEntity: UserEntity?) {
        viewModelScope.launch {
            if (userEntity?.first_name.isNullOrBlank()) {
                _navEvent.emit(LoginNavEvent.NavigateToUserUpdate)
            } else {
                val userId = userEntity.id
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

    override fun onCleared() {
        super.onCleared()
        loading = false
    }

    fun completeAuthentication(onAuthComplete: () -> Unit) {
        if (loading) return
        loading = true
        recaptchaLoading = true
        progressMessage = "Verifying reCAPTCHA..."

        viewModelScope.launch(DispatcherProvider.io) {
            val authResult = userRepository.completeAuth(sessionId, otp)
            if (authResult.data == null || authResult.error != null) {
                withContext(Dispatchers.Main) {
                    displayMessage = authResult.error?.message ?: ""
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
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
            // Even if user fetch fails, complete auth — user fetched later in checkUserLogin()
            withContext(Dispatchers.Main) {
                delay(1000)
                onAuthComplete()
                loading = false
                recaptchaLoading = false
                progressMessage = ""
            }
        }
    }

    fun resendOtp(onResendSuccess: (String) -> Unit) {
        if (loading) return
        loading = true
        recaptchaLoading = true
        progressMessage = "Preparing to resend OTP..."

        viewModelScope.launch(DispatcherProvider.io) {
            val result = userRepository.resendOtp(phoneNumber)
            if (result.data != null && result.error == null) {
                val data = result.data!!
                withContext(Dispatchers.Main) {
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                    if (data.success && data.sessionId != null) {
                        this@LoginViewModel.sessionId = data.sessionId
                        onResendSuccess(data.sessionId)
                    } else {
                        displayMessage = data.error?.message ?: "Failed to resend OTP"
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    displayMessage = result.error?.message ?: ""
                    loading = false
                    recaptchaLoading = false
                    progressMessage = ""
                }
            }
        }
    }

    // ========== Firebase Authentication Methods ==========

    fun authenticateWithFirebase(onAuthSuccess: (String) -> Unit) {
        if (loading) return
        loading = true
        progressMessage = "Sending verification code..."

        viewModelScope.launch(DispatcherProvider.io) {
            val countryCode = "91"
            when (val result = firebaseAuthRepository.sendOtp(countryCode, phoneNumber)) {
                is FirebaseAuthResult.Success -> {
                    withContext(Dispatchers.Main) {
                        firebaseVerificationId = result.data
                        loading = false
                        progressMessage = ""
                        onAuthSuccess(result.data)
                    }
                }
                is FirebaseAuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        displayMessage = result.message
                    }
                }
                FirebaseAuthResult.Loading -> Unit
            }
        }
    }

    fun completeFirebaseAuthentication(onAuthComplete: () -> Unit) {
        if (loading) return
        loading = true
        progressMessage = "Verifying code..."

        viewModelScope.launch(DispatcherProvider.io) {
            tokenRepository.createDummyUserSession()
            when (val result = firebaseAuthRepository.verifyOtp(firebaseVerificationId, otp)) {
                is FirebaseAuthResult.Success -> {
                    val firebaseIdToken = result.data
                    val verifiedPhoneNumber = phoneNumber
                    val backendResult = userRepository.verifyFirebaseAuth(firebaseIdToken, verifiedPhoneNumber)
                    if (backendResult.data == null || backendResult.error != null) {
                        withContext(Dispatchers.Main) {
                            displayMessage = backendResult.error?.message ?: ""
                            loading = false
                            progressMessage = ""
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
                        onAuthComplete()
                        loading = false
                        progressMessage = ""
                    }
                }
                is FirebaseAuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        displayMessage = result.message
                    }
                }
                FirebaseAuthResult.Loading -> Unit
            }
        }
    }

    fun resendFirebaseOtp(onResendSuccess: (String) -> Unit) {
        if (loading) return
        loading = true
        progressMessage = "Resending verification code..."

        viewModelScope.launch(DispatcherProvider.io) {
            val countryCode = "91"
            when (val result = firebaseAuthRepository.resendOtp(countryCode, phoneNumber)) {
                is FirebaseAuthResult.Success -> {
                    withContext(Dispatchers.Main) {
                        firebaseVerificationId = result.data
                        loading = false
                        progressMessage = ""
                        onResendSuccess(result.data)
                    }
                }
                is FirebaseAuthResult.Error -> {
                    withContext(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        displayMessage = result.message
                    }
                }
                FirebaseAuthResult.Loading -> Unit
            }
        }
    }

    fun completeFirebaseAuthenticationWithToken(firebaseIdToken: String, onAuthComplete: () -> Unit) {
        if (loading) return
        loading = true
        progressMessage = "Completing authentication..."

        viewModelScope.launch(DispatcherProvider.io) {
            tokenRepository.createDummyUserSession()
            val verifiedPhoneNumber = phoneNumber
            val backendResult = userRepository.verifyFirebaseAuth(firebaseIdToken, verifiedPhoneNumber)
            if (backendResult.data == null || backendResult.error != null) {
                withContext(Dispatchers.Main) {
                    displayMessage = backendResult.error?.message ?: ""
                    loading = false
                    progressMessage = ""
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
                onAuthComplete()
                loading = false
                progressMessage = ""
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
        loading = true
        progressMessage = "Completing authentication..."

        viewModelScope.launch(DispatcherProvider.io) {
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
                        loading = false
                        progressMessage = ""
                        delay(500)
                        onSuccess()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        loading = false
                        progressMessage = ""
                        onError("Failed to fetch user information: ${userResult.error?.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading = false
                    progressMessage = ""
                    onError("Authentication error: ${e.message}")
                }
            }
        }
    }
}
