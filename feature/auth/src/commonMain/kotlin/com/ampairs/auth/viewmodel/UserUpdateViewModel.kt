package com.ampairs.auth.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.api.model.UserUpdateRequest
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.model.UiState
import com.ampairs.common.model.onError
import com.ampairs.common.model.onSuccess
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.size
import com.ampairs.common.di.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.ampairs.auth.db.UserRepository

sealed interface UserUpdateNavEvent {
    data class LoginSuccess(val userInfo: UserInfo?) : UserUpdateNavEvent
    data class NavigateToWorkspace(val userInfo: UserInfo?) : UserUpdateNavEvent
}

@ContributesIntoMap(AppScope::class)
@ViewModelKey
@Inject
class UserUpdateViewModel(
    private val authApi: AuthApi,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val userWorkspaceRepository: UserWorkspaceRepository,
) : ViewModel() {

    private val _navEvent = MutableSharedFlow<UserUpdateNavEvent>()
    val navEvent: SharedFlow<UserUpdateNavEvent> = _navEvent.asSharedFlow()

    companion object {
        private val SUPPORTED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L // 5MB for profile pictures
    }

    var firstName by mutableStateOf("")
        private set

    var lastName by mutableStateOf("")
        private set

    var profilePictureUrl by mutableStateOf<String?>(null)
        private set

    var selectedImageData by mutableStateOf<ByteArray?>(null)
        private set

    var selectedImageFileName by mutableStateOf<String?>(null)
        private set

    var selectedImageContentType by mutableStateOf<String?>(null)
        private set

    var userState by mutableStateOf<UiState<UserApiModel>>(UiState.Empty)
        private set

    var updateUserState by mutableStateOf<UiState<UserApiModel>>(UiState.Empty)
        private set

    var uploadPictureState by mutableStateOf<UiState<UserApiModel>>(UiState.Empty)
        private set

    var displayMessage by mutableStateOf("")

    init {
        loadUserDetails()
    }

    fun updateFirstName(newFirstName: String) {
        firstName = newFirstName
    }

    fun updateLastName(newLastName: String) {
        lastName = newLastName
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            userState = UiState.Loading(null)
            authApi.getUser().onSuccess {
                this@UserUpdateViewModel.firstName = this.firstName
                this@UserUpdateViewModel.lastName = this.lastName
                // Use API endpoint URL for profile picture, not the raw storage path
                this@UserUpdateViewModel.profilePictureUrl = if (!this.profilePictureUrl.isNullOrBlank()) {
                    ApiUrlBuilder.currentUserPictureUrl()
                } else {
                    null
                }
                userState = UiState.Success(this)
            }.onError {
                userState = UiState.Error(this.message.ifEmpty { "Failed to load user details" })
            }
        }
    }

    fun pickProfilePicture() {
        viewModelScope.launch {
            try {
                val file = FileKit.openFilePicker(
                    type = FileKitType.Image
                )

                if (file == null) {
                    return@launch
                }

                val fileName = file.name
                val fileSize = file.size()

                // Validate file extension
                val extension = fileName.substringAfterLast(".", "").lowercase()
                if (extension !in SUPPORTED_IMAGE_EXTENSIONS) {
                    displayMessage = "Unsupported file type. Please select JPG, PNG, or WebP."
                    return@launch
                }

                // Validate file size
                if (fileSize > MAX_FILE_SIZE) {
                    displayMessage = "Image too large. Maximum size is 5MB."
                    return@launch
                }

                // Determine content type
                val contentType = when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/*"
                }

                // Read file data
                val imageData = file.readBytes()

                selectedImageData = imageData
                selectedImageFileName = fileName
                selectedImageContentType = contentType

            } catch (e: Exception) {
                displayMessage = "Failed to select image"
            }
        }
    }

    fun clearSelectedImage() {
        selectedImageData = null
        selectedImageFileName = null
        selectedImageContentType = null
    }

    fun uploadProfilePicture(onSuccess: () -> Unit = {}) {
        val imageData = selectedImageData
        val fileName = selectedImageFileName
        val contentType = selectedImageContentType

        if (imageData == null || fileName == null || contentType == null) {
            displayMessage = "No image selected"
            return
        }

        viewModelScope.launch {
            uploadPictureState = UiState.Loading(null)
            val response = authApi.uploadProfilePicture(imageData, fileName, contentType)

            if (response.data != null) {
                val updatedUser = response.data!!
                // Save updated user (with new profile picture) to local database
                userRepository.saveUser(updatedUser)
                uploadPictureState = UiState.Success(updatedUser)
                // Use API endpoint URL for profile picture, not the raw storage path
                this@UserUpdateViewModel.profilePictureUrl = if (!updatedUser.profilePictureUrl.isNullOrBlank()) {
                    // Add random param to bust cache after upload
                    "${ApiUrlBuilder.currentUserPictureUrl()}?t=${Random.nextLong()}"
                } else {
                    null
                }
                clearSelectedImage()
                displayMessage = "Profile picture updated successfully"
                onSuccess()
            } else {
                val errorMessage = response.error?.message.orEmpty().ifEmpty { "Failed to upload profile picture" }
                uploadPictureState = UiState.Error(errorMessage)
                displayMessage = errorMessage
            }
        }
    }

    fun updateUser(onSuccess: () -> Unit) {
        if (firstName.isBlank()) {
            displayMessage = "First name is required"
            return
        }

        viewModelScope.launch {
            updateUserState = UiState.Loading(null)
            val request = UserUpdateRequest(
                firstName = firstName.trim(),
                lastName = lastName.trim()
            )
            val response = authApi.updateUser(request)

            if (response.data != null) {
                val updatedUser = response.data!!
                // Save updated user to local database
                userRepository.saveUser(updatedUser)
                updateUserState = UiState.Success(updatedUser)
                displayMessage = "Profile updated successfully"
                onSuccess()
            } else {
                val errorMessage = response.error?.message.orEmpty().ifEmpty { "Failed to update user details" }
                updateUserState = UiState.Error(errorMessage)
                displayMessage = errorMessage
            }
        }
    }

    fun handleUpdateSuccess() {
        viewModelScope.launch {
            val accessToken = tokenRepository.getAccessToken()
            val refreshToken = tokenRepository.getRefreshToken()
            if (!accessToken.isNullOrBlank()) {
                val currentUserId = tokenRepository.getCurrentUserId() ?: return@launch
                tokenRepository.addAuthenticatedUser(
                    userId = currentUserId,
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )

                var userInfo: UserInfo? = null
                userRepository.getUserById(currentUserId)?.let { userEntity ->
                    val profilePictureUrl = userEntity.profile_picture_url?.let { url ->
                        if (url.isNotBlank() && !url.startsWith("http")) ApiUrlBuilder.currentUserPictureUrl()
                        else url.takeIf { it.isNotBlank() }
                    }
                    val profilePictureThumbnailUrl = userEntity.profile_picture_thumbnail_url?.let { url ->
                        if (url.isNotBlank() && !url.startsWith("http")) ApiUrlBuilder.currentUserPictureThumbnailUrl()
                        else url.takeIf { it.isNotBlank() }
                    }
                    userInfo = UserInfo(
                        id = userEntity.id,
                        firstName = userEntity.first_name,
                        lastName = userEntity.last_name,
                        userName = userEntity.user_name,
                        countryCode = userEntity.country_code,
                        phone = userEntity.phone,
                        profilePictureUrl = profilePictureUrl,
                        profilePictureThumbnailUrl = profilePictureThumbnailUrl,
                        lastLogin = 0L,
                        loginCount = 0,
                        isAuthenticated = true,
                        hasSelectedWorkspace = true
                    )
                }

                val hasWorkspace = userWorkspaceRepository.getWorkspaceIdForUser(currentUserId).isNotBlank()
                if (hasWorkspace) {
                    _navEvent.emit(UserUpdateNavEvent.LoginSuccess(userInfo))
                } else {
                    _navEvent.emit(UserUpdateNavEvent.NavigateToWorkspace(userInfo))
                }
            }
        }
    }

    fun clearMessage() {
        displayMessage = ""
    }

    val isLoading: Boolean
        get() = userState is UiState.Loading || updateUserState is UiState.Loading || uploadPictureState is UiState.Loading

    val isFormValid: Boolean
        get() = firstName.isNotBlank()

    val hasSelectedImage: Boolean
        get() = selectedImageData != null
}