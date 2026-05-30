package com.ampairs.auth.ui

import ampairsapp.feature.auth.generated.resources.Res
import ampairsapp.feature.auth.generated.resources.auth_cd_change_photo
import ampairsapp.feature.auth.generated.resources.auth_cd_clear_selection
import ampairsapp.feature.auth.generated.resources.auth_cd_default_avatar
import ampairsapp.feature.auth.generated.resources.auth_cd_profile_picture
import ampairsapp.feature.auth.generated.resources.auth_cd_selected_profile_pic
import ampairsapp.feature.auth.generated.resources.auth_tap_to_change_photo
import ampairsapp.feature.auth.generated.resources.auth_upload_photo
import ampairsapp.feature.auth.generated.resources.auth_uploading
import ampairsapp.feature.auth.generated.resources.error_colon
import ampairsapp.feature.auth.generated.resources.first_name
import ampairsapp.feature.auth.generated.resources.last_name
import ampairsapp.feature.auth.generated.resources.loading_user_details
import ampairsapp.feature.auth.generated.resources.update_profile
import ampairsapp.feature.auth.generated.resources.update_your_profile
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.ampairs.auth.domain.UserInfo
import com.ampairs.auth.viewmodel.UserUpdateNavEvent
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.model.UiState
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun UserUpdateScreen(
    onProfilePictureUpdated: ((newThumbnailUrl: String) -> Unit)? = null,
    onLoginSuccess: (UserInfo?) -> Unit = {},
    onNavigateToWorkspace: (UserInfo?) -> Unit = {},
    viewModel: UserUpdateViewModel = metroViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.navEvent.collect { event ->
            when (event) {
                is UserUpdateNavEvent.LoginSuccess -> onLoginSuccess(event.userInfo)
                is UserUpdateNavEvent.NavigateToWorkspace -> onNavigateToWorkspace(event.userInfo)
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    val isLoading = state.userState is UiState.Loading ||
            state.updateUserState is UiState.Loading ||
            state.uploadPictureState is UiState.Loading

    Scaffold(
        modifier = Modifier.imePadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val userState = state.userState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(stringResource(Res.string.loading_user_details))
                }

                is UiState.Error -> {
                    Text(
                        text = stringResource(Res.string.error_colon) + userState.msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                is UiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(max = 400.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(Res.string.update_your_profile),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            ProfilePictureSection(
                                profilePictureUrl = state.profilePictureUrl,
                                selectedImageData = state.selectedImageData,
                                isLoading = state.uploadPictureState is UiState.Loading,
                                onPickImage = { viewModel.pickProfilePicture() },
                                onClearSelection = { viewModel.clearSelectedImage() },
                                onUpload = {
                                    viewModel.uploadProfilePicture {
                                        onProfilePictureUpdated?.invoke(ApiUrlBuilder.currentUserPictureThumbnailUrl())
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = state.firstName,
                                onValueChange = viewModel::updateFirstName,
                                label = { Text(stringResource(Res.string.first_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = state.lastName,
                                onValueChange = viewModel::updateLastName,
                                label = { Text(stringResource(Res.string.last_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !isLoading
                            )
                        }

                        Button(
                            onClick = { viewModel.updateUser() },
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            enabled = state.firstName.isNotBlank() && !isLoading
                        ) {
                            if (state.updateUserState is UiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .progressSemantics()
                                        .size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(Res.string.update_profile))
                            }
                        }
                    }
                }

                is UiState.Empty -> {}
            }
        }
    }
}

@Composable
private fun ProfilePictureSection(
    profilePictureUrl: String?,
    selectedImageData: ByteArray?,
    isLoading: Boolean,
    onPickImage: () -> Unit,
    onClearSelection: () -> Unit,
    onUpload: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable(enabled = !isLoading) { onPickImage() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 3.dp
                        )
                    }
                    selectedImageData != null -> {
                        AsyncImage(
                            model = selectedImageData,
                            contentDescription = stringResource(Res.string.auth_cd_selected_profile_pic),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !profilePictureUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = stringResource(Res.string.auth_cd_profile_picture),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = stringResource(Res.string.auth_cd_default_avatar),
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { onPickImage() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = stringResource(Res.string.auth_cd_change_photo),
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            if (selectedImageData != null && !isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error)
                        .clickable { onClearSelection() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(Res.string.auth_cd_clear_selection),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }

        if (selectedImageData != null) {
            OutlinedButton(onClick = onUpload, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(Res.string.auth_uploading))
                } else {
                    Text(stringResource(Res.string.auth_upload_photo))
                }
            }
        } else {
            Text(
                text = stringResource(Res.string.auth_tap_to_change_photo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
