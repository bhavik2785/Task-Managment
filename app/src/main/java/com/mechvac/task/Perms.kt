package com.mechvac.task

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager

object Perms {
    fun hasNotifications(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false
        val nm = ctx.getSystemService(android.app.NotificationManager::class.java) ?: return true
        return nm.areNotificationsEnabled()
    }

    fun batteryOk(ctx: Context): Boolean {
        val pm = ctx.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(ctx.packageName)
    }
}
