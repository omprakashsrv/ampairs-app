package com.ampairs.network.security

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import ampairsapp.data.common.generated.resources.Res
import ampairsapp.data.common.generated.resources.update_button_exit
import ampairsapp.data.common.generated.resources.update_button_later
import ampairsapp.data.common.generated.resources.update_button_update_now
import ampairsapp.data.common.generated.resources.update_recommended_message
import ampairsapp.data.common.generated.resources.update_recommended_title
import ampairsapp.data.common.generated.resources.update_required_message
import ampairsapp.data.common.generated.resources.update_required_title
import kotlinx.coroutines.suspendCancellableCoroutine
import org.jetbrains.compose.resources.getString
import kotlin.coroutines.resume

class AndroidAppUpdateEnforcer(
    private val context: Context
) : AppUpdateEnforcer {
    
    private var allowNetworkRequests = true
    
    override suspend fun showUpdateDialog(status: AppUpdateStatus) {
        when (status) {
            is AppUpdateStatus.Required -> showRequiredUpdateDialog()
            is AppUpdateStatus.Recommended -> showRecommendedUpdateDialog(status.reason)
            AppUpdateStatus.NotRequired -> { /* No action needed */ }
        }
    }
    
    override suspend fun enforceUpdate() {
        allowNetworkRequests = false
        showRequiredUpdateDialog()
    }
    
    override suspend fun shouldAllowNetworkRequests(): Boolean {
        return allowNetworkRequests
    }
    
    private suspend fun showRequiredUpdateDialog() {
        val title = getString(Res.string.update_required_title)
        val message = getString(Res.string.update_required_message)
        val updateBtn = getString(Res.string.update_button_update_now)
        val exitBtn = getString(Res.string.update_button_exit)

        suspendCancellableCoroutine { continuation ->
            val alertDialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(updateBtn) { dialog, _ ->
                    openAppStore()
                    dialog.dismiss()
                    continuation.resume(Unit)
                }
                .setNegativeButton(exitBtn) { dialog, _ ->
                    dialog.dismiss()
                    android.os.Process.killProcess(android.os.Process.myPid())
                    continuation.resume(Unit)
                }
                .create()

            alertDialog.show()

            continuation.invokeOnCancellation { alertDialog.dismiss() }
        }
    }

    private suspend fun showRecommendedUpdateDialog(reason: String) {
        val title = getString(Res.string.update_recommended_title)
        val message = getString(Res.string.update_recommended_message, reason)
        val updateBtn = getString(Res.string.update_button_update_now)
        val laterBtn = getString(Res.string.update_button_later)

        suspendCancellableCoroutine { continuation ->
            val alertDialog = AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(updateBtn) { dialog, _ ->
                    openAppStore()
                    dialog.dismiss()
                    continuation.resume(Unit)
                }
                .setNegativeButton(laterBtn) { dialog, _ ->
                    dialog.dismiss()
                    continuation.resume(Unit)
                }
                .create()

            alertDialog.show()

            continuation.invokeOnCancellation { alertDialog.dismiss() }
        }
    }
    
    private fun openAppStore() {
        try {
            // Try to open Google Play Store
            val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(playStoreIntent)
        } catch (e: Exception) {
            try {
                // Fallback to web browser
                val webIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
            } catch (e: Exception) {
                // Could not open store
            }
        }
    }
}