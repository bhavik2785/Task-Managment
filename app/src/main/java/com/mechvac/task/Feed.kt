package com.mechvac.task

import android.content.Context

/** Asks the website what is new for this phone and shows it as notifications. */
object Feed {
    /** Returns how many seconds to wait before the next check. */
    @Synchronized
    fun run(ctx: Context): Int {
        val prefs = Prefs(ctx)
        if (prefs.token == null) return 300
        val r = Api.call(ctx, "feed")
        if (r.code == 401) {
            LiveService.stop(ctx)
            return 900
        }
        val j = r.json ?: return 120   // offline, or the hosting's security check: try again a bit later
        prefs.lastSync = System.currentTimeMillis()
        val poll = j.optInt("poll", 60).coerceIn(20, 900)
        prefs.pollSeconds = poll
        // instant notifications through Firebase, when the admin has set it up
        Fcm.configure(ctx, j.optJSONObject("fcm"))
        // optional always-on check (only if the admin switched it on; it shows a permanent notification)
        val keep = j.optBoolean("keepalive", false)
        if (keep != prefs.keepAlive) prefs.keepAlive = keep
        if (keep && !LiveService.running) LiveService.start(ctx) else if (!keep && LiveService.running) LiveService.stop(ctx)
        val items = j.optJSONArray("items") ?: return poll
        for (i in 0 until items.length()) {
            val it = items.optJSONObject(i) ?: continue
            // while the app is open on screen, the page shows these itself
            if (MainActivity.visible) Notify.markShown(ctx, it.optString("tag"))
            else Notify.item(ctx, it.optString("tag"), it.optString("kind"), it.optString("title"), it.optString("body"), it.optString("url"))
        }
        return poll
    }
}
