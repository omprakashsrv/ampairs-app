package com.ampairs.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ampairs.auth.viewmodel.UserUpdateViewModel
import com.ampairs.common.model.UiState
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.composeapp.generated.resources.Res
import ampairsapp.composeapp.generated.resources.loading_user_details
import ampairsapp.composeapp.generated.resources.update_your_profile
import ampairsapp.composeapp.generated.resources.first_name
import ampairsapp.composeapp.generated.resources.last_name
import ampairsapp.composeapp.generated.resources.update_profile
import ampairsapp.composeapp.generated.resources.error_colon

@Composable
fun UserUpdateScreen(
    onUpdateSuccess: () -> Unit
) {
    val viewModel: UserUpdateViewModel = koinViewModel()
    val snackbarHostState = remember { SnackbarHostState() }

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
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

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