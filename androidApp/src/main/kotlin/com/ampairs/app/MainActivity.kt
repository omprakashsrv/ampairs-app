package com.ampairs.app

import MainView
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import coil3.compose.setSingletonImageLoaderFactory
import com.ampairs.app.update.InAppUpdateManager
import com.ampairs.app.update.UpdateCheckResult
import com.ampairs.formwidgets.contact.ContactPickerResultHolder
import com.ampairs.formwidgets.contact.ContactPickerService
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var updateManager: InAppUpdateManager

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: Compose owns the window insets (the app pads via imePadding() and the global
        // layout's systemBars insets). Do NOT also call setDecorFitsSystemWindows(window, true) — that
        // makes the window itself ALSO resize for the keyboard, which double-stacks with Compose's
        // imePadding() and leaves a keyboard-sized gap above the IME on bottom-anchored screens (chat).
        enableEdgeToEdge()

        actionBar?.hide()

        // Initialize FileKit for Android platform
        FileKit.init(this)

        // Initialize in-app update manager
        updateManager = InAppUpdateManager(this)

        val appGraph = (application as MainApp).appGraph
        setContent {
            setSingletonImageLoaderFactory { _ -> appGraph.imageLoader }

            MainView()
        }

        // Check for updates on app launch
        lifecycleScope.launch {
            when (val result = updateManager.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Log.d(TAG, "Update available - showing dialog")
                    showUpdateAvailableDialog(result)
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
                        showUpdateCompleteDialog()
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

    private fun showUpdateAvailableDialog(updateInfo: UpdateCheckResult.UpdateAvailable) {
        val isImmediate = updateInfo.updateType == AppUpdateType.IMMEDIATE
        AlertDialog.Builder(this)
            .setTitle("App Update Available")
            .setMessage(
                if (isImmediate) "A critical update is available. Please update the app to continue."
                else "A new version of the app is available. Would you like to update now?"
            )
            .setCancelable(!isImmediate)
            .setPositiveButton("Update") { dialog, _ ->
                dialog.dismiss()
                updateManager.startUpdate(updateInfo.appUpdateInfo, updateInfo.updateType)
            }
            .apply {
                if (!isImmediate) setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            }
            .show()
    }

    private fun showUpdateCompleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("Update Ready")
            .setMessage("An update has been downloaded. The app will restart to complete the installation.")
            .setCancelable(false)
            .setPositiveButton("Restart") { dialog, _ ->
                dialog.dismiss()
                updateManager.completeUpdate()
            }
            .setNegativeButton("Later") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
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