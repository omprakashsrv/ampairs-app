package com.ampairs.common.update

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Manager for handling in-app updates using Google Play Core library.
 *
 * Supports both immediate and flexible update flows:
 * - Immediate: Forces user to update before continuing
 * - Flexible: Allows user to continue using app and update in background
 */
class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private var currentType = AppUpdateType.FLEXIBLE

    companion object {
        private const val TAG = "InAppUpdateManager"
        private const val UPDATE_REQUEST_CODE = 1001

        // Priority thresholds - adjust based on your update strategy
        private const val IMMEDIATE_UPDATE_PRIORITY_THRESHOLD = 4 // Critical updates
        private const val FLEXIBLE_UPDATE_PRIORITY_THRESHOLD = 2 // Normal updates
    }

    /**
     * Check for available updates and return update info.
     * Returns null if no update is available.
     */
    suspend fun checkForUpdate(): UpdateCheckResult {
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()

            when {
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                    val updateType = determineUpdateType(appUpdateInfo)
                    Log.d(TAG, "Update available - Type: ${if (updateType == AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"}, " +
                            "Priority: ${appUpdateInfo.updatePriority()}, " +
                            "Available version: ${appUpdateInfo.availableVersionCode()}")
                    UpdateCheckResult.UpdateAvailable(appUpdateInfo, updateType)
                }
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADING ||
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "Update already in progress - Status: ${appUpdateInfo.installStatus()}")
                    UpdateCheckResult.UpdateInProgress(appUpdateInfo)
                }
                else -> {
                    Log.d(TAG, "No update available")
                    UpdateCheckResult.NoUpdate
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            UpdateCheckResult.Error(e)
        }
    }

    /**
     * Determine the appropriate update type based on update priority.
     * Higher priority updates use immediate mode, lower priority use flexible.
     */
    private fun determineUpdateType(appUpdateInfo: AppUpdateInfo): Int {
        val priority = appUpdateInfo.updatePriority()

        return when {
            priority >= IMMEDIATE_UPDATE_PRIORITY_THRESHOLD -> {
                AppUpdateType.IMMEDIATE
            }
            priority >= FLEXIBLE_UPDATE_PRIORITY_THRESHOLD -> {
                AppUpdateType.FLEXIBLE
            }
            else -> {
                // Default to flexible for low priority updates
                AppUpdateType.FLEXIBLE
            }
        }
    }

    /**
     * Start the update flow.
     * For immediate updates, this will block the UI until update completes.
     * For flexible updates, update downloads in background.
     */
    fun startUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int = AppUpdateType.FLEXIBLE): Boolean {
        currentType = updateType

        return try {
            // Verify update is allowed for the requested type
            val isUpdateTypeAllowed = when (updateType) {
                AppUpdateType.IMMEDIATE -> appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                AppUpdateType.FLEXIBLE -> appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                else -> false
            }

            if (!isUpdateTypeAllowed) {
                Log.w(TAG, "Update type not allowed for this update")
                return false
            }

            val updateOptions = AppUpdateOptions.newBuilder(updateType).build()

            appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                updateOptions,
                UPDATE_REQUEST_CODE
            )

            Log.d(TAG, "Update flow started - Type: ${if (updateType == AppUpdateType.IMMEDIATE) "IMMEDIATE" else "FLEXIBLE"}")
            true
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Error starting update flow", e)
            false
        }
    }

    /**
     * Monitor installation progress for flexible updates.
     * Emits installation status updates as a Flow.
     */
    fun installProgressFlow(): Flow<Int> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            trySend(state.installStatus())

            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "Update downloaded successfully")
                }
                InstallStatus.INSTALLED -> {
                    Log.d(TAG, "Update installed successfully")
                    appUpdateManager.unregisterListener(this@callbackFlow as InstallStateUpdatedListener)
                }
                InstallStatus.FAILED -> {
                    Log.e(TAG, "Update installation failed: ${state.installErrorCode()}")
                    appUpdateManager.unregisterListener(this@callbackFlow as InstallStateUpdatedListener)
                }
            }
        }

        appUpdateManager.registerListener(listener)

        awaitClose {
            appUpdateManager.unregisterListener(listener)
        }
    }

    /**
     * Complete the update after flexible download.
     * This will restart the app to apply the update.
     */
    fun completeUpdate() {
        appUpdateManager.completeUpdate()
        Log.d(TAG, "Completing update - App will restart")
    }

    /**
     * Check if an update was interrupted and needs to be resumed.
     * Call this in onResume() to handle interrupted updates.
     */
    suspend fun checkForInterruptedUpdate(): Boolean {
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()

            when {
                // Resume immediate update if interrupted
                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    Log.d(TAG, "Resuming interrupted immediate update")
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                    true
                }
                // Notify about downloaded flexible update
                appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                    Log.d(TAG, "Flexible update already downloaded, ready to install")
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for interrupted update", e)
            false
        }
    }
}

/**
 * Sealed class representing different update check results
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val appUpdateInfo: AppUpdateInfo,
        val updateType: Int
    ) : UpdateCheckResult()

    data class UpdateInProgress(
        val appUpdateInfo: AppUpdateInfo
    ) : UpdateCheckResult()

    data object NoUpdate : UpdateCheckResult()

    data class Error(val exception: Exception) : UpdateCheckResult()
}
