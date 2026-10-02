package com.mechvac.task

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject

/**
 * Firebase Cloud Messaging: the website asks Google to deliver each alert to this phone at once.
 * The Firebase project comes from the website (Company settings > Phone app), not from a file inside the app.
 */
object Fcm {
    /** Starts Firebase with the saved settings. Returns false when it is not set up. */
    @Synchronized
    fun init(ctx: Context): Boolean {
        val raw = Prefs(ctx).fcmConfig ?: return false
        val j = runCatching { JSONObject(raw) }.getOrNull() ?: return false
        return try {
            if (FirebaseApp.getApps(ctx).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(j.getString("apiKey"))
                    .setApplicationId(j.getString("appId"))
                    .setProjectId(j.getString("projectId"))
                    .setGcmSenderId(j.getString("senderId"))
                    .build()
                FirebaseApp.initializeApp(ctx, options)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Called with the Firebase settings from each check-in (null when the admin has not set it up). */
    fun configure(ctx: Context, cfg: JSONObject?) {
        val p = Prefs(ctx)
        if (cfg == null || cfg.optString("appId").isEmpty()) {
            if (p.fcmConfig != null) { p.fcmConfig = null; p.fcmSent = null }
            return
        }
        val raw = cfg.toString()
        if (raw != p.fcmConfig) { p.fcmConfig = raw; p.fcmSent = null }
        if (!init(ctx)) return
        // the token rarely changes; only ask again until the website has it for the current login
        if (p.fcmSent != null && p.fcmSent!!.endsWith("|" + (p.token ?: ""))) return
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                val token = if (task.isSuccessful) task.result else null
                if (!token.isNullOrEmpty()) Thread { sendToken(ctx.applicationContext, token) }.start()
            }
        } catch (e: Exception) {
            // Google Play services missing or Firebase settings wrong: the 15-minute check still works
        }
    }

    /** Gives this phone's Firebase token to the website for the person signed in. */
    fun sendToken(ctx: Context, fcmToken: String) {
        val p = Prefs(ctx)
        val device = p.token ?: return
        val mark = "$fcmToken|$device"
        if (p.fcmSent == mark) return
        val r = Api.call(ctx, "fcm_token", form = mapOf("fcm_token" to fcmToken))
        if (r.code == 200 && r.json?.optBoolean("ok") == true) p.fcmSent = mark
    }
}
