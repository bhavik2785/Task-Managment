package com.mechvac.task

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder

/**
 * Optional always-on check (every 30-180 seconds), only when the admin switches it on in Company settings.
 * Android requires a permanent notification for this, so normally Firebase is used instead.
 */
class LiveService : Service() {
    @Volatile private var stopped = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(NOTIF_ID, Notify.live(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            else startForeground(NOTIF_ID, Notify.live(this))
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (Prefs(this).token == null || !Prefs(this).keepAlive) {
            stopSelf()
            return START_NOT_STICKY
        }
        running = true
        if (worker?.isAlive != true) {
            stopped = false
            val app = applicationContext
            worker = Thread {
                while (!stopped) {
                    val wait = try { Feed.run(app) } catch (e: Exception) { 120 }
                    if (Prefs(app).token == null || !Prefs(app).keepAlive) break
                    try { Thread.sleep(wait * 1000L) } catch (e: InterruptedException) { }
                }
                stopSelf()
            }.apply { start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopped = true
        running = false
        worker?.interrupt()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 77
        @Volatile var running = false

        fun start(ctx: Context) {
            if (Prefs(ctx).token == null || !Prefs(ctx).keepAlive) return
            try {
                ctx.startForegroundService(Intent(ctx, LiveService::class.java))
            } catch (e: Exception) {
                // Android may refuse from the background; the 15-minute job still runs and retries
            }
        }

        fun stop(ctx: Context) {
            try { ctx.stopService(Intent(ctx, LiveService::class.java)) } catch (e: Exception) { }
        }
    }
}
