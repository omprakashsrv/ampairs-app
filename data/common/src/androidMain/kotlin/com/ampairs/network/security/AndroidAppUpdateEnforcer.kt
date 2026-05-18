package com.ampairs.network.security

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.ampairs.data.common.R
import kotlinx.coroutines.suspendCancellableCoroutine
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
    
    private suspend fun showRequiredUpdateDialog() = suspendCancellableCoroutine<Unit> { continuation ->
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.update_required_title))
            .setMessage(context.getString(R.string.update_required_message))
            .setCancelable(false)
            .setPositiveButton(context.getString(R.string.update_button_update_now)) { dialog, _ ->
                openAppStore()
                dialog.dismiss()
                continuation.resume(Unit)
            }
            .setNegativeButton(context.getString(R.string.update_button_exit)) { dialog, _ ->
                dialog.dismiss()
                // Force close the app
                android.os.Process.killProcess(android.os.Process.myPid())
                continuation.resume(Unit)
            }
            .create()
        
        alertDialog.show()
        
        continuation.invokeOnCancellation {
            alertDialog.dismiss()
        }
    }
    
    private suspend fun showRecommendedUpdateDialog(reason: String) = suspendCancellableCoroutine<Unit> { continuation ->
        val alertDialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.update_recommended_title))
            .setMessage(context.getString(R.string.update_recommended_message, reason))
            .setCancelable(true)
            .setPositiveButton(context.getString(R.string.update_button_update_now)) { dialog, _ ->
                openAppStore()
                dialog.dismiss()
                continuation.resume(Unit)
            }
            .setNegativeButton(context.getString(R.string.update_button_later)) { dialog, _ ->
                dialog.dismiss()
                continuation.resume(Unit)
            }
            .create()
        
        alertDialog.show()
        
        continuation.invokeOnCancellation {
            alertDialog.dismiss()
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