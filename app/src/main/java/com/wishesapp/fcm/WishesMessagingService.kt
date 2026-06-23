package com.wishesapp.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.wishesapp.MainActivity
import com.wishesapp.R
import com.wishesapp.data.repository.AuthRepository
import com.wishesapp.widget.WishWidgetWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WishesMessagingService : FirebaseMessagingService() {

    @Inject lateinit var authRepo: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        serviceScope.launch {
            authRepo.updateFcmToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val spaceId = data["spaceId"] ?: return
        val wishId = data["wishId"] ?: return
        val authorName = data["authorName"] ?: "Your partner"
        val text = data["text"] ?: "added a new wish"

        // Trigger widget update via WorkManager (expedited, runs even in background)
        WishWidgetWorker.enqueueForPush(applicationContext)

        // Show a notification
        showNotification(authorName, text)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "wishes_channel"
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel(channelId, "New Wishes", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$title added a wish")
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        nm.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
