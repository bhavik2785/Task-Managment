package com.mechvac.task

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONArray

/** Receives alerts from Firebase, even when the app is closed, and shows them right away. */
class PushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val raw = message.data["items"] ?: return
        val items = runCatching { JSONArray(raw) }.getOrNull() ?: return
        for (i in 0 until items.length()) {
            val it = items.optJSONObject(i) ?: continue
            val tag = it.optString("tag")
            // while the app is open on screen, the page shows it itself
            if (MainActivity.visible) Notify.markShown(this, tag)
            else Notify.item(this, tag, it.optString("kind"), it.optString("title"), it.optString("body"), it.optString("url"))
        }
    }

    override fun onNewToken(token: String) {
        val app = applicationContext
        if (Prefs(app).token != null) Thread { Fcm.sendToken(app, token) }.start()
    }
}
