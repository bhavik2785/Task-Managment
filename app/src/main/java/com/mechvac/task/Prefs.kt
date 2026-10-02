package com.mechvac.task

import android.content.Context

/** Small saved settings: website address, this phone's token and setup progress. */
class Prefs(ctx: Context) {
    private val sp = ctx.applicationContext.getSharedPreferences("mechvac_task", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = (sp.getString("base_url", null) ?: BuildConfig.APP_URL).trimEnd('/')
        set(v) = sp.edit().putString("base_url", v.trim().trimEnd('/')).apply()

    var token: String?
        get() = sp.getString("token", null)
        set(v) = sp.edit().putString("token", v).apply()

    var uid: Int
        get() = sp.getInt("uid", 0)
        set(v) = sp.edit().putInt("uid", v).apply()

    var permsAsked: Boolean
        get() = sp.getBoolean("perms_asked", false)
        set(v) = sp.edit().putBoolean("perms_asked", v).apply()

    var notifAskedAtLaunch: Boolean
        get() = sp.getBoolean("notif_asked_launch", false)
        set(v) = sp.edit().putBoolean("notif_asked_launch", v).apply()

    var autostartShown: Boolean
        get() = sp.getBoolean("autostart_shown", false)
        set(v) = sp.edit().putBoolean("autostart_shown", v).apply()

    var lastSync: Long
        get() = sp.getLong("last_sync", 0L)
        set(v) = sp.edit().putLong("last_sync", v).apply()

    /** Seconds between checks, decided by the server (Company settings > Live update speed). */
    var pollSeconds: Int
        get() = sp.getInt("poll", 60)
        set(v) = sp.edit().putInt("poll", v).apply()

    /** When the hosting's security check last answered with a web page instead of data. */
    var serverBlockedAt: Long
        get() = sp.getLong("blocked_at", 0L)
        set(v) = sp.edit().putLong("blocked_at", v).apply()

    /** Firebase settings sent by the website (Company settings > Phone app). */
    var fcmConfig: String?
        get() = sp.getString("fcm_config", null)
        set(v) = sp.edit().putString("fcm_config", v).apply()

    /** Which Firebase token was last given to the website, for which login ("fcmToken|deviceToken"). */
    var fcmSent: String?
        get() = sp.getString("fcm_sent", null)
        set(v) = sp.edit().putString("fcm_sent", v).apply()

    /** Admin chose the always-on check (shows a small permanent notification). */
    var keepAlive: Boolean
        get() = sp.getBoolean("keepalive", false)
        set(v) = sp.edit().putBoolean("keepalive", v).apply()

    /** Alerts already shown, so the same one never appears twice. */
    var shownTags: String
        get() = sp.getString("shown", "") ?: ""
        set(v) = sp.edit().putString("shown", v).apply()

    fun clearLink() {
        sp.edit().remove("token").remove("uid").remove("fcm_sent").apply()
    }
}
