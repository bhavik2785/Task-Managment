package com.mechvac.task

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** After a phone restart or app update, start checking for notifications again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Prefs(context).token == null) return
        SyncJob.schedule(context)
        LiveService.start(context)   // only if the admin switched on the always-on check
        Fcm.init(context)
    }
}
