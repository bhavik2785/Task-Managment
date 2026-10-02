package com.mechvac.task

import android.os.Build
import android.webkit.JavascriptInterface
import org.json.JSONObject

/** Functions the MechVac Task web pages can call inside the app (window.MechVacApp). */
class Bridge(private val activity: MainActivity) {
    private val app get() = activity.applicationContext

    @JavascriptInterface
    fun linkedUser(): Int {
        val p = Prefs(app)
        return if (p.token != null) p.uid else 0
    }

    @JavascriptInterface
    fun model(): String = (Build.MANUFACTURER.replaceFirstChar { it.uppercase() } + " " + Build.MODEL).trim()

    @JavascriptInterface
    fun version(): String = BuildConfig.VERSION_NAME

    /** The website gave this phone its token after sign-in. */
    @JavascriptInterface
    fun link(token: String, uid: Int) {
        if (!Regex("^[a-f0-9]{64}$").matches(token)) return
        val p = Prefs(app)
        val old = p.token
        p.token = token
        p.uid = uid
        if (old != null && old != token) Thread { Api.call(app, "unlink", tokenOverride = old) }.start()
        activity.runOnUiThread { activity.onLinked() }
    }

    /** Signing out: this phone stops getting that person's notifications. */
    @JavascriptInterface
    fun unlink() {
        val ctx = app
        val token = Prefs(ctx).token ?: return
        Prefs(ctx).clearLink()
        LiveService.stop(ctx)
        SyncJob.cancel(ctx)
        Thread { Api.call(ctx, "unlink", tokenOverride = token) }.start()
    }

    @JavascriptInterface
    fun status(): String = JSONObject()
        .put("linked", Prefs(app).token != null)
        .put("notifications", Perms.hasNotifications(app))
        .put("battery", Perms.batteryOk(app))
        .put("live", LiveService.running)
        .put("instant", Prefs(app).fcmSent != null)
        .put("lastSync", Prefs(app).lastSync)
        .put("serverBlocked", Prefs(app).serverBlockedAt > 0)
        .put("version", BuildConfig.VERSION_NAME)
        .toString()

    /** The open page found a new alert (works even without Firebase). Shown once, whichever way it arrives first. */
    @JavascriptInterface
    fun notify(tag: String, kind: String, title: String, body: String, url: String) {
        if (Notify.onScreen(kind, url)) { Notify.markShown(app, tag); return }
        Notify.item(app, tag, kind, title, body, url)
    }

    @JavascriptInterface
    fun testNotification(): Boolean {
        if (!Perms.hasNotifications(app)) { activity.runOnUiThread { activity.askPermissions(force = true) }; return false }
        Notify.test(app)
        return true
    }

    @JavascriptInterface
    fun fixPermissions() {
        activity.runOnUiThread { activity.askPermissions(force = true) }
    }
}
