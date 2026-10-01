package com.mechvac.task

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Notify {
    private const val CH_TASKS = "tasks"
    private const val CH_REMINDERS = "reminders"
    private const val CH_ANNOUNCE = "announcements"
    private const val CH_CHAT = "chat"
    const val CH_LIVE = "live"
    private const val BRAND = 0xFF0097B2.toInt()

    fun channels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH_TASKS, "Tasks and approvals", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "New tasks, comments, status changes and approvals"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_REMINDERS, "Deadline reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "A day before, an hour before, and when a task is overdue"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_ANNOUNCE, "Announcements", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Company and team announcements"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_CHAT, "Chat messages", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Messages to you and your team chats"
            enableVibration(true)
        })
        nm.createNotificationChannel(NotificationChannel(CH_LIVE, "Live connection", NotificationManager.IMPORTANCE_MIN).apply {
            description = "Small silent icon that keeps notifications instant. You can hide this category."
            setShowBadge(false)
        })
    }

    fun openApp(ctx: Context, requestCode: Int, path: String?): PendingIntent {
        val open = Intent(ctx, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_PATH, path)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(ctx, requestCode, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** One alert from the website: tag "n123" (notification) or "m45" (chat message). */
    fun item(ctx: Context, tag: String, kind: String, title: String, body: String, url: String) {
        if (!Perms.hasNotifications(ctx)) return
        channels(ctx)
        val num = tag.drop(1).toIntOrNull() ?: tag.hashCode()
        val id = if (tag.startsWith("m")) 500_000_000 + (num % 400_000_000) else 100_000_000 + (num % 400_000_000)
        val channel = when (kind) {
            "reminder" -> CH_REMINDERS
            "announcement" -> CH_ANNOUNCE
            "chat" -> CH_CHAT
            else -> CH_TASKS
        }
        val n = Notification.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(BRAND)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setShowWhen(true)
            .setContentIntent(openApp(ctx, id, "/" + url.trimStart('/')))
            .build()
        try {
            ctx.getSystemService(NotificationManager::class.java)?.notify(id, n)
        } catch (e: SecurityException) {
            // notification permission was removed
        }
    }

    fun live(ctx: Context): Notification {
        channels(ctx)
        return Notification.Builder(ctx, CH_LIVE)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(BRAND)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText("Live: you will be notified about new tasks and messages")
            .setOngoing(true)
            .setContentIntent(openApp(ctx, 1, null))
            .build()
    }

    fun test(ctx: Context) =
        item(ctx, "n0", "task", "Notifications work", "You will get tasks, approvals, reminders, announcements and chat here.", "profile.php")
}
