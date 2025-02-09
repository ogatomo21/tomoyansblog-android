package net.ogatomo.tomoyansblog

import android.app.Activity
import android.content.Intent
import android.os.Bundle

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val serviceIntent = Intent(this, OpenUrlService::class.java)
        startService(serviceIntent)
        finish()
    }
}
