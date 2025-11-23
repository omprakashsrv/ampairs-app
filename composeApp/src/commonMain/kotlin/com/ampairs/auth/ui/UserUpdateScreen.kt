package com.ampairs.auth.ui

import androidx.compose.foundation.Image
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import com.ampairs.common.model.UiState
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.loading_user_details
import ampairsapp.composeapp.generated.resources.update_your_profile
import ampairsapp.composeapp.generated.resources.first_name
import ampairsapp.composeapp.generated.resources.last_name
import ampairsapp.composeapp.generated.resources.update_profile
import ampairsapp.composeapp.generated.resources.error_colon
import androidx.compose.foundation.layout.imePadding
import coil3.compose.AsyncImage
import com.ampairs.common.ApiUrlBuilder
import com.ampairs.common.state.AppHeaderStateManager
import kotlin.random.Random

@Composable
fun UserUpdateScreen(
    onUpdateSuccess: () -> Unit
) {
    val viewModel: UserUpdateViewModel = koinViewModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val headerStateManager = remember { AppHeaderStateManager.instance }

    // Show error/success messages
    LaunchedEffect(viewModel.displayMessage) {
        if (viewModel.displayMessage.isNotEmpty()) {
            val result = snackbarHostState.showSnackbar(
                message = viewModel.displayMessage,
                duration = SnackbarDuration.Short
            )
            when (result) {
                SnackbarResult.Dismissed -> viewModel.clearMessage()
                SnackbarResult.ActionPerformed -> viewModel.clearMessage()
            }
        }
    }

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
            when (val userState = viewModel.userState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp)
                    )
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
                        modifier = Modifier
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Content area
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

                            // Profile Picture Section
                            ProfilePictureSection(
                                profilePictureUrl = viewModel.profilePictureUrl,
                                selectedImageData = viewModel.selectedImageData,
                                isLoading = viewModel.uploadPictureState is UiState.Loading,
                                onPickImage = { viewModel.pickProfilePicture() },
                                onClearSelection = { viewModel.clearSelectedImage() },
                                onUpload = {
                                    viewModel.uploadProfilePicture {
                                        // Update header state with new profile picture thumbnail
                                        headerStateManager.headerState.value.currentUser?.let { currentUser ->
                                            headerStateManager.updateUser(
                                                currentUser.copy(
                                                    // Use thumbnail URL with cache bust for header avatar
                                                    profilePictureThumbnailUrl = "${ApiUrlBuilder.currentUserPictureThumbnailUrl()}?t=${Random.nextLong()}"
                                                )
                                            )
                                        }
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            OutlinedTextField(
                                value = viewModel.firstName,
                                onValueChange = viewModel::updateFirstName,
                                label = { Text(stringResource(Res.string.first_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !viewModel.isLoading
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            OutlinedTextField(
                                value = viewModel.lastName,
                                onValueChange = viewModel::updateLastName,
                                label = { Text(stringResource(Res.string.last_name)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                enabled = !viewModel.isLoading
                            )
                        }

                        // Bottom-aligned button
                        Button(
                            onClick = { viewModel.updateUser(onUpdateSuccess) },
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            enabled = viewModel.isFormValid && !viewModel.isLoading
                        ) {
                            if (viewModel.updateUserState is UiState.Loading) {
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

                is UiState.Empty -> {
                    // This shouldn't happen as we load user details in init
                }
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
        // Profile Picture with camera overlay
        Box(
            contentAlignment = Alignment.Center
        ) {
            // Main profile picture
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
                        // Show selected image preview using Coil's memory cache
                        AsyncImage(
                            model = selectedImageData,
                            contentDescription = "Selected profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    !profilePictureUrl.isNullOrBlank() -> {
                        // Show current profile picture from URL
                        AsyncImage(
                            model = profilePictureUrl,
                            contentDescription = "Profile picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        // Default avatar icon
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Default avatar",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Camera button overlay
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
                        contentDescription = "Change photo",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // Clear selection button (only when image is selected)
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
                        contentDescription = "Clear selection",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }

        // Upload button (only when new image is selected)
        if (selectedImageData != null) {
            OutlinedButton(
                onClick = onUpload,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Uploading...")
                } else {
                    Text("Upload Photo")
                }
            }
        } else {
            Text(
                text = "Tap to change photo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}