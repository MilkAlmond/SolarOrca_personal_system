package com.yujing.tool.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.yujing.tool.MainActivity
import com.yujing.tool.R
import com.yujing.tool.data.Formats
import java.time.OffsetDateTime
import java.time.ZoneId

object NotificationHelper {

    const val CHANNEL_ID = "reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "提醒",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "生日、课程、任务与自定义提醒"
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Notification content is limited to title + time, per plan §4.6. */
    fun show(context: Context, reminderId: Long, title: String, triggerAtMillis: Long): Boolean {
        if (!canPost(context)) {
            return false
        }
        ensureChannel(context)
        val time = try {
            OffsetDateTime.ofInstant(java.time.Instant.ofEpochMilli(triggerAtMillis), ZoneId.systemDefault())
                .toLocalTime()
                .let { "%02d:%02d".format(it.hour, it.minute) }
        } catch (ex: Exception) {
            Formats.dateTime(java.time.Instant.ofEpochMilli(triggerAtMillis).toString())
        }
        val open = PendingIntent.getActivity(
            context,
            10000 + (reminderId % 100000).toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle(title.ifBlank { "提醒" })
            .setContentText("提醒时间 $time")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(reminderId.toInt(), notification)
            true
        } catch (ex: SecurityException) {
            false
        }
    }
}
