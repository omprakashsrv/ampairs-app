package com.ampairs.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.auth.api.AuthApi
import com.ampairs.auth.api.TokenRepository
import com.ampairs.auth.api.UserWorkspaceRepository
import com.ampairs.auth.api.model.UserApiModel
import com.ampairs.auth.api.model.UserUpdateRequest
import com.ampairs.auth.domain.UserInfo
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.filepicker.defaultFileDialogSettings
import com.ampairs.common.model.UiState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.ampairs.auth.db.UserRepository
import org.jetbrains.compose.resources.getString
import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.auth_error_unsupported_file_type
import ampairsapp.feature.auth.generated.resources.auth_error_image_too_large
import ampairsapp.feature.auth.generated.resources.auth_error_failed_select_image
import ampairsapp.feature.auth.generated.resources.auth_error_no_image_selected
import ampairsapp.feature.auth.generated.resources.auth_success_profile_picture_updated
import ampairsapp.feature.auth.generated.resources.auth_error_first_name_required
import ampairsapp.feature.auth.generated.resources.auth_success_profile_updated
import ampairsapp.feature.auth.generated.resources.auth_error_failed_load_user
import ampairsapp.feature.auth.generated.resources.auth_error_failed_update_user
import ampairsapp.feature.auth.generated.resources.auth_error_failed_upload_picture

data class UserUpdateUiState(
    val firstName: String = "",
    val lastName: String = "",
    val profilePictureUrl: String? = null,
    val selectedImageData: ByteArray? = null,
    val selectedImageFileName: String? = null,
    val selectedImageContentType: String? = null,
    val userState: UiState<UserApiModel> = UiState.Empty,
    val updateUserState: UiState<UserApiModel> = UiState.Empty,
    val uploadPictureState: UiState<UserApiModel> = UiState.Empty,
)

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

    private val _state = MutableStateFlow(UserUpdateUiState())
    val state: StateFlow<UserUpdateUiState> = _state.asStateFlow()

    private val _navEvent = MutableSharedFlow<UserUpdateNavEvent>(extraBufferCapacity = 1)
    val navEvent: SharedFlow<UserUpdateNavEvent> = _navEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    companion object {
        private val SUPPORTED_IMAGE_EXTENSIONS = listOf("jpg", "jpeg", "png", "webp")
        private const val MAX_FILE_SIZE = 5 * 1024 * 1024L
    }

    init {
        loadUserDetails()
    }

    fun updateFirstName(newFirstName: String) {
        _state.update { it.copy(firstName = newFirstName) }
    }

    fun updateLastName(newLastName: String) {
        _state.update { it.copy(lastName = newLastName) }
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            _state.update { it.copy(userState = UiState.Loading(null)) }
            val response = authApi.getUser()
            if (response.data != null && response.error == null) {
                val user = response.data!!
                val picUrl = if (!user.profilePictureUrl.isNullOrBlank()) ApiUrlBuilder.currentUserPictureUrl() else null
                _state.update {
                    it.copy(
                        firstName = user.firstName,
                        lastName = user.lastName,
                        profilePictureUrl = picUrl,
                        userState = UiState.Success(user),
                    )
                }
            } else {
                val errMsg = response.error?.message.orEmpty().ifEmpty { getString(Res.string.auth_error_failed_load_user) }
                _state.update { it.copy(userState = UiState.Error(errMsg)) }
            }
        }
    }

    fun pickProfilePicture() {
        viewModelScope.launch {
            try {
                val file = FileKit.openFilePicker(
                    type = FileKitType.Image,
                    dialogSettings = defaultFileDialogSettings(),
                ) ?: return@launch
                val fileName = file.name
                val fileSize = file.size()
                val extension = fileName.substringAfterLast(".", "").lowercase()
                if (extension !in SUPPORTED_IMAGE_EXTENSIONS) {
                    _snackbarMessage.emit(getString(Res.string.auth_error_unsupported_file_type))
                    return@launch
                }
                if (fileSize > MAX_FILE_SIZE) {
                    _snackbarMessage.emit(getString(Res.string.auth_error_image_too_large))
                    return@launch
                }
                val contentType = when (extension) {
                    "jpg", "jpeg" -> "image/jpeg"
                    "png" -> "image/png"
                    "webp" -> "image/webp"
                    else -> "image/*"
                }
                val imageData = file.readBytes()
                _state.update {
                    it.copy(
                        selectedImageData = imageData,
                        selectedImageFileName = fileName,
                        selectedImageContentType = contentType,
                    )
                }
            } catch (_: Exception) {
                _snackbarMessage.emit(getString(Res.string.auth_error_failed_select_image))
            }
        }
    }

    fun clearSelectedImage() {
        _state.update { it.copy(selectedImageData = null, selectedImageFileName = null, selectedImageContentType = null) }
    }

    fun uploadProfilePicture(onSuccess: () -> Unit = {}) {
        val current = _state.value
        if (current.selectedImageData == null || current.selectedImageFileName == null || current.selectedImageContentType == null) {
            viewModelScope.launch { _snackbarMessage.emit(getString(Res.string.auth_error_no_image_selected)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(uploadPictureState = UiState.Loading(null)) }
            val response = authApi.uploadProfilePicture(
                current.selectedImageData, current.selectedImageFileName, current.selectedImageContentType
            )
            if (response.data != null) {
                val updatedUser = response.data!!
                userRepository.saveUser(updatedUser)
                val newPicUrl = if (!updatedUser.profilePictureUrl.isNullOrBlank()) {
                    "${ApiUrlBuilder.currentUserPictureUrl()}?t=${Random.nextLong()}"
                } else null
                _state.update {
                    it.copy(
                        uploadPictureState = UiState.Success(updatedUser),
                        profilePictureUrl = newPicUrl,
                        selectedImageData = null,
                        selectedImageFileName = null,
                        selectedImageContentType = null,
                    )
                }
                _snackbarMessage.emit(getString(Res.string.auth_success_profile_picture_updated))
                onSuccess()
            } else {
                val errMsg = response.error?.message.orEmpty().ifEmpty { getString(Res.string.auth_error_failed_upload_picture) }
                _state.update { it.copy(uploadPictureState = UiState.Error(errMsg)) }
                _snackbarMessage.emit(errMsg)
            }
        }
    }

    fun updateUser() {
        viewModelScope.launch {
            if (_state.value.firstName.isBlank()) {
                _snackbarMessage.emit(getString(Res.string.auth_error_first_name_required))
                return@launch
            }
            _state.update { it.copy(updateUserState = UiState.Loading(null)) }
            val request = UserUpdateRequest(
                firstName = _state.value.firstName.trim(),
                lastName = _state.value.lastName.trim()
            )
            val response = authApi.updateUser(request)
            if (response.data != null) {
                val updatedUser = response.data!!
                userRepository.saveUser(updatedUser)
                _state.update { it.copy(updateUserState = UiState.Success(updatedUser)) }
                _snackbarMessage.emit(getString(Res.string.auth_success_profile_updated))
                handleUpdateSuccess()
            } else {
                val errMsg = response.error?.message.orEmpty().ifEmpty { getString(Res.string.auth_error_failed_update_user) }
                _state.update { it.copy(updateUserState = UiState.Error(errMsg)) }
                _snackbarMessage.emit(errMsg)
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

}
