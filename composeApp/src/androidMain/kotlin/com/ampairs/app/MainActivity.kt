package com.ampairs.app

import MainView
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.ampairs.auth.api.TokenRepository
import com.ampairs.common.ActivityProvider
import com.ampairs.common.ImageCacheKeyer
import com.ampairs.common.httpClient
import com.ampairs.common.update.InAppUpdateManager
import com.ampairs.common.update.UpdateCheckResult
import com.ampairs.customer.ui.components.contact.ContactPickerResultHolder
import com.ampairs.customer.ui.components.contact.ContactPickerService
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: InAppUpdateManager
    private val showUpdateDialog = mutableStateOf(false)
    private val showCompleteUpdateDialog = mutableStateOf(false)
    private val currentUpdateInfo = mutableStateOf<UpdateCheckResult.UpdateAvailable?>(null)

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Register activity for Firebase Phone Auth
        ActivityProvider.setActivity(this)

        // Configure edge-to-edge display for Android 15+ (SDK 35) compatibility
        // This ensures proper handling of system bars and display cutouts
        enableEdgeToEdge()

        // For backward compatibility with older Android versions
        // Ensures window decorFitsSystemWindows is properly configured
//        WindowCompat.setDecorFitsSystemWindows(window, false)

        actionBar?.hide()

        // Initialize FileKit for Android platform
        FileKit.init(this)

        // Initialize in-app update manager
        updateManager = InAppUpdateManager(this)

        setContent {
            setSingletonImageLoaderFactory { context ->
                generateImageLoader()
            }

            // Show update available dialog
            if (showUpdateDialog.value && currentUpdateInfo.value != null) {
                val updateInfo = currentUpdateInfo.value!!
                val isImmediate = updateInfo.updateType == AppUpdateType.IMMEDIATE

                AlertDialog(
                    onDismissRequest = {
                        if (!isImmediate) {
                            showUpdateDialog.value = false
                        }
                    },
                    title = { Text("App Update Available") },
                    text = {
                        Text(
                            if (isImmediate) {
                                "A critical update is available. Please update the app to continue."
                            } else {
                                "A new version of the app is available. Would you like to update now?"
                            }
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showUpdateDialog.value = false
                                updateManager.startUpdate(updateInfo.appUpdateInfo, updateInfo.updateType)
                            }
                        ) {
                            Text("Update")
                        }
                    },
                    dismissButton = if (!isImmediate) {
                        {
                            TextButton(onClick = { showUpdateDialog.value = false }) {
                                Text("Later")
                            }
                        }
                    } else null
                )
            }

            // Show update complete dialog
            if (showCompleteUpdateDialog.value) {
                AlertDialog(
                    onDismissRequest = { /* Don't dismiss */ },
                    title = { Text("Update Ready") },
                    text = { Text("An update has been downloaded. The app will restart to complete the installation.") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showCompleteUpdateDialog.value = false
                                updateManager.completeUpdate()
                            }
                        ) {
                            Text("Restart")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCompleteUpdateDialog.value = false }) {
                            Text("Later")
                        }
                    }
                )
            }

            MainView()
        }

        // Check for updates on app launch
        lifecycleScope.launch {
            val result = updateManager.checkForUpdate()
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Log.d(TAG, "Update available - showing dialog")
                    currentUpdateInfo.value = result
                    showUpdateDialog.value = true
                }
                is UpdateCheckResult.NoUpdate -> {
                    Log.d(TAG, "App is up to date")
                }
                is UpdateCheckResult.Error -> {
                    Log.e(TAG, "Error checking for updates", result.exception)
                }
                else -> {
                    Log.d(TAG, "Update check result: $result")
                }
            }
        }

        // Monitor flexible update installation progress
        lifecycleScope.launch {
            updateManager.installProgressFlow().collect { status ->
                when (status) {
                    InstallStatus.DOWNLOADED -> {
                        Log.d(TAG, "Update downloaded - prompting user to restart")
                        showCompleteUpdateDialog.value = true
                    }
                    InstallStatus.INSTALLED -> {
                        Log.d(TAG, "Update installed successfully")
                    }
                    InstallStatus.FAILED -> {
                        Log.e(TAG, "Update installation failed")
                    }
                    else -> {
                        Log.d(TAG, "Update installation status: $status")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Check for interrupted updates
        lifecycleScope.launch {
            val hasInterruptedUpdate = updateManager.checkForInterruptedUpdate()
            if (hasInterruptedUpdate) {
                Log.d(TAG, "Resumed interrupted update")
            }
        }
    }

    // Your shared Ktor client with global auth headers
    private fun generateImageLoader(): ImageLoader {
        val engine = get<HttpClientEngine>()
        val tokenRepository = get<TokenRepository>()
        val client = httpClient(engine, tokenRepository)

        return ImageLoader.Builder(this@MainActivity)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(this@MainActivity, 0.25) // Increased for better on-demand performance
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this@MainActivity.cacheDir.resolve("customer_images_cache").toOkioPath())
                    .maxSizeBytes(100L * 1024 * 1024) // Increased to 100MB for better offline experience
                    .build()
            }
            .components {
                add(KtorNetworkFetcherFactory(client))
                add(ImageCacheKeyer())
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear activity reference to avoid memory leaks
        ActivityProvider.clearActivity()
        // Clear contact picker callbacks
        ContactPickerResultHolder.clearCallbacks()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            ContactPickerService.CONTACT_PICKER_REQUEST_CODE -> {
                val contactUri = data?.data
                ContactPickerResultHolder.onContactResult(contactUri)
            }
        }
    }
}

