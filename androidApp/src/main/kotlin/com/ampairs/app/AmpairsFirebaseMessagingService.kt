package com.ampairs.app

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ampairs.di.AndroidAppGraph
import com.ampairs.di.AppGraphHolder
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android FCM entry point (Phase 4). Android instantiates this service outside Metro, so it reaches
 * the shared [com.ampairs.common.firebase.messaging.FirebaseMessaging] singleton through
 * [AppGraphHolder] (the same root [AndroidAppGraph] created in [MainApp]). It forwards token
 * refreshes and incoming messages into that singleton, and shows a system notification for
 * data-only messages received while the app is foregrounded (the OS auto-displays "notification"
 * messages when backgrounded).
 */
class AmpairsFirebaseMessagingService : FirebaseMessagingService() {

    private val firebaseMessaging
        get() = (AppGraphHolder.graph as? AndroidAppGraph)?.firebaseMessaging

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received")
        // Forwarding into the shared singleton triggers PushTokenRegistrar's refresh listener,
        // which re-registers the token with the backend.
        firebaseMessaging?.onTokenRefreshed(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.messageId}")
        firebaseMessaging?.onMessageReceived(message)

        // The system auto-displays "notification" messages when the app is backgrounded; when the
        // app is foregrounded (or for data-only messages) we surface one ourselves.
        message.notification?.let { showNotification(it.title, it.body) }
    }

    private fun showNotification(title: String?, body: String?) {
        if (title == null && body == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "POST_NOTIFICATIONS not granted; skipping notification display")
            return
        }

        val channelId = getString(R.string.default_notification_channel_id)
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentIntent = launchIntent?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .apply { contentIntent?.let { setContentIntent(it) } }
            .build()

        NotificationManagerCompat.from(this)
            .notify(NOTIFICATION_ID_COUNTER.getAndIncrement(), notification)
    }

    private companion object {
        private const val TAG = "FCM"
        private val NOTIFICATION_ID_COUNTER = AtomicInteger(1000)
    }
}
