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
            val kind = it.optString("kind")
            val url = it.optString("url")
            // every alert becomes a phone notification, even with the app open, except the chat you are reading
            if (Notify.onScreen(kind, url)) Notify.markShown(this, tag)
            else Notify.item(this, tag, kind, it.optString("title"), it.optString("body"), url)
        }
    }

    override fun onNewToken(token: String) {
        val app = applicationContext
        if (Prefs(app).token != null) Thread { Fcm.sendToken(app, token) }.start()
    }
}
