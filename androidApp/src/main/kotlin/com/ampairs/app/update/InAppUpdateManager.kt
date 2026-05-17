package com.ampairs.app.update

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

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager = AppUpdateManagerFactory.create(activity)
    private var currentType = AppUpdateType.FLEXIBLE

    companion object {
        private const val TAG = "InAppUpdateManager"
        private const val UPDATE_REQUEST_CODE = 1001

        private const val IMMEDIATE_UPDATE_PRIORITY_THRESHOLD = 4
        private const val FLEXIBLE_UPDATE_PRIORITY_THRESHOLD = 2
    }

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

    private fun determineUpdateType(appUpdateInfo: AppUpdateInfo): Int {
        val priority = appUpdateInfo.updatePriority()
        return when {
            priority >= IMMEDIATE_UPDATE_PRIORITY_THRESHOLD -> AppUpdateType.IMMEDIATE
            priority >= FLEXIBLE_UPDATE_PRIORITY_THRESHOLD -> AppUpdateType.FLEXIBLE
            else -> AppUpdateType.FLEXIBLE
        }
    }

    fun startUpdate(appUpdateInfo: AppUpdateInfo, updateType: Int = AppUpdateType.FLEXIBLE): Boolean {
        currentType = updateType
        return try {
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

    fun installProgressFlow(): Flow<Int> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            trySend(state.installStatus())
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> Log.d(TAG, "Update downloaded successfully")
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
        awaitClose { appUpdateManager.unregisterListener(listener) }
    }

    fun completeUpdate() {
        appUpdateManager.completeUpdate()
        Log.d(TAG, "Completing update - App will restart")
    }

    suspend fun checkForInterruptedUpdate(): Boolean {
        return try {
            val appUpdateInfo = appUpdateManager.appUpdateInfo.await()
            when {
                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    Log.d(TAG, "Resuming interrupted immediate update")
                    startUpdate(appUpdateInfo, AppUpdateType.IMMEDIATE)
                    true
                }
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