package net.ogatomo.tomoyansblog

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder

class OpenUrlService : Service() {
    override fun onCreate() {
        super.onCreate()
        val url = "https://ogatomo.net/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
