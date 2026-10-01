package com.mechvac.task

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build

/** Phone brands that stop background apps unless "Autostart" / "background activity" is allowed. */
object Autostart {
    private val brand get() = Build.MANUFACTURER.lowercase()

    private val screens = listOf(
        "com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity",          // Xiaomi, Redmi, POCO
        "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",     // Oppo, Realme
        "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
        "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",       // Vivo, iQOO
        "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity",
        "com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",  // Huawei, Honor
        "com.huawei.systemmanager" to "com.huawei.systemmanager.optimize.process.ProtectActivity",
        "com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity",        // OnePlus
        "com.letv.android.letvsafe" to "com.letv.android.letvsafe.AutobootManageActivity",
        "com.asus.mobilemanager" to "com.asus.mobilemanager.entry.FunctionActivity",
        "com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity"                   // Samsung battery
    )

    fun needed(): Boolean = listOf("xiaomi", "redmi", "poco", "oppo", "realme", "vivo", "iqoo", "huawei", "honor", "oneplus", "letv", "asus", "samsung").any { brand.contains(it) }

    fun instructions(): String = when {
        brand.contains("samsung") -> "Samsung phones put apps to sleep. On the next screen open Background usage limits and add this app to “Never sleeping apps”."
        brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco") -> "Xiaomi phones stop apps in the background. On the next screen switch ON Autostart for this app. Then in the app’s settings set Battery saver to “No restrictions”."
        brand.contains("vivo") || brand.contains("iqoo") -> "Vivo phones stop apps in the background. On the next screen allow this app to run in the background (Autostart / High background power use)."
        else -> "Your phone stops apps in the background. On the next screen switch ON Autostart (or “Allow background activity”) for this app, so task and chat notifications keep arriving."
    }

    /** Opens the brand's autostart screen. Returns false if none was found. */
    fun open(ctx: Context): Boolean {
        for ((pkg, cls) in screens) {
            val i = Intent().setComponent(ComponentName(pkg, cls)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                if (i.resolveActivity(ctx.packageManager) != null) { ctx.startActivity(i); return true }
            } catch (e: Exception) { }
        }
        return false
    }
}
