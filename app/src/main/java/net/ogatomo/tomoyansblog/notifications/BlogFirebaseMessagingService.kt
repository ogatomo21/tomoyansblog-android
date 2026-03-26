package net.ogatomo.tomoyansblog.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BlogFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        BlogPushManager.syncSubscription(applicationContext)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        val title = data["title"]
            ?: data["notificationTitle"]
            ?: data["headline"]
            ?: message.notification?.title
            ?: getString(net.ogatomo.tomoyansblog.R.string.app_name)
        val body = data["body"]
            ?: data["message"]
            ?: data["articleTitle"]
            ?: message.notification?.body
            ?: title
        val url = data["url"]
            ?: data["link"]
            ?: data["articleUrl"]
            ?: data["postUrl"]
            ?: message.notification?.link?.toString()
        val imageUrl = data["image"]
            ?: data["imageUrl"]
            ?: data["thumbnail"]
            ?: message.notification?.imageUrl?.toString()
        val channelId = data["channel"]
            ?: data["channelId"]
            ?: message.notification?.channelId
            ?: if (message.from?.endsWith("notify") == true) BlogPushManager.CHANNEL_NOTIFY else null

        BlogPushManager.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            url = url,
            imageUrl = imageUrl,
            channelId = channelId
        )
    }
}
