package net.ogatomo.tomoyansblog.notifications

import android.Manifest
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import net.ogatomo.tomoyansblog.MainActivity
import net.ogatomo.tomoyansblog.R
import net.ogatomo.tomoyansblog.data.NotificationPreferences
import java.net.URL

object BlogPushManager {
    const val CHANNEL_BLOG_UPDATES = "blog_updates"
    const val CHANNEL_NOTIFY = "notify"

    private const val TOPIC_BLOG_UPDATES = "blog_updates"
    private const val TOPIC_NOTIFY = "notify"

    fun syncSubscription(context: Context) {
        val messaging = FirebaseMessaging.getInstance()
        if (canReceivePush(context)) {
            messaging.subscribeToTopic(TOPIC_BLOG_UPDATES)
            messaging.subscribeToTopic(TOPIC_NOTIFY)
        } else {
            messaging.unsubscribeFromTopic(TOPIC_BLOG_UPDATES)
            messaging.unsubscribeFromTopic(TOPIC_NOTIFY)
        }
    }

    fun canReceivePush(context: Context): Boolean {
        val preferences = NotificationPreferences(context)
        return preferences.isNotificationsEnabled() &&
            NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            hasNotificationPermission(context)
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showNotification(
        context: Context,
        title: String,
        body: String,
        url: String?,
        imageUrl: String?,
        channelId: String?
    ) {
        if (!canReceivePush(context)) {
            return
        }

        createChannels(context)
        val targetChannelId = resolveChannelId(channelId)

        val articleUrl = url?.takeIf { it.isNotBlank() }
        val intent = Intent(context, MainActivity::class.java).apply {
            data = articleUrl?.let(Uri::parse)
            putExtra(MainActivity.EXTRA_INITIAL_URL, articleUrl)
            putExtra(MainActivity.EXTRA_OPEN_INITIAL_URL_EXTERNALLY, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            articleUrl?.hashCode() ?: 1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, targetChannelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val bigPicture = loadNotificationImage(imageUrl)
        if (bigPicture != null) {
            notificationBuilder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bigPicture)
                    .bigLargeIcon(null as Bitmap?)
                    .setSummaryText(body)
            )
        } else {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(body))
        }

        val notification = notificationBuilder.build()

        val notificationId = System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun loadNotificationImage(imageUrl: String?): Bitmap? {
        val targetUrl = imageUrl?.takeIf { it.isNotBlank() } ?: return null
        return runCatching {
            URL(targetUrl).openStream().use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

    private fun resolveChannelId(channelId: String?): String {
        return when (channelId?.trim()?.lowercase()) {
            CHANNEL_NOTIFY -> CHANNEL_NOTIFY
            else -> CHANNEL_BLOG_UPDATES
        }
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val blogUpdatesChannel = NotificationChannel(
            CHANNEL_BLOG_UPDATES,
            "新着記事",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tomoyan's Blog の新着記事通知"
        }
        val notifyChannel = NotificationChannel(
            CHANNEL_NOTIFY,
            "ブログからのお知らせ",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tomoyan's Blog からのお知らせ"
        }
        manager.createNotificationChannel(blogUpdatesChannel)
        manager.createNotificationChannel(notifyChannel)
    }
}
