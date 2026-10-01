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

    fun clearLink() {
        sp.edit().remove("token").remove("uid").apply()
    }
}
