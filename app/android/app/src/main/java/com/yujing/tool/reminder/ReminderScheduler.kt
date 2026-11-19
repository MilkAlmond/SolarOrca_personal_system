package com.yujing.tool.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.yujing.tool.data.ReminderItem
import java.time.OffsetDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {

    private const val PERIODIC_WORK = "reminder-check-periodic"
    private const val IMMEDIATE_WORK = "reminder-check-now"
    private const val SCHEDULED_KEY = "scheduled_reminder_ids"

    fun ensureChannel(context: Context) {
        NotificationHelper.ensureChannel(context)
    }

    /** Periodic server check is the fallback when an exact alarm cannot fire. */
    fun enqueuePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderCheckWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun enqueueCheck(context: Context) {
        val request = OneTimeWorkRequestBuilder<ReminderCheckWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun canScheduleExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        val manager = context.getSystemService(AlarmManager::class.java)
        return manager.canScheduleExactAlarms()
    }

    /**
     * Mirrors the server's reminder list into local exact alarms. Called on every
     * launch and after each sync, per plan §4.5.
     */
    fun reschedule(context: Context, reminders: List<ReminderItem>, globalEnabled: Boolean) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val store = context.getSharedPreferences("yujing_reminders", Context.MODE_PRIVATE)
        val previous = store.getStringSet(SCHEDULED_KEY, emptySet()).orEmpty().toMutableSet()
        val current = mutableSetOf<String>()
        val now = System.currentTimeMillis()

        if (globalEnabled) {
            reminders.filter { it.enabled }.forEach { reminder ->
                val millis = parseMillis(reminder.triggerAt) ?: return@forEach
                if (millis <= now) {
                    return@forEach
                }
                schedule(context, alarmManager, reminder.id, reminder.title, millis)
                current.add(reminder.id.toString())
            }
        }

        previous.filterNot { current.contains(it) }.forEach { id ->
            id.toLongOrNull()?.let { cancel(context, it) }
        }
        store.edit().putStringSet(SCHEDULED_KEY, current).apply()
    }

    fun cancelAll(context: Context) {
        reschedule(context, emptyList(), false)
    }

    fun schedule(context: Context, alarmManager: AlarmManager, id: Long, title: String, triggerAtMillis: Long) {
        val pending = pendingIntent(context, id, title, triggerAtMillis)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // Without the exact-alarm grant the reminder may fire late; the OS still
            // delivers it when the device leaves Doze.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pending)
        }
    }

    fun cancel(context: Context, id: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pending = pendingIntent(context, id, "", 0)
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(context: Context, id: Long, title: String, triggerAtMillis: Long): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            putExtra(ReminderAlarmReceiver.EXTRA_ID, id)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_TRIGGER, triggerAtMillis)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode(id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(id: Long): Int = (id % Int.MAX_VALUE).toInt()

    private fun parseMillis(value: String?): Long? {
        if (value.isNullOrBlank()) {
            return null
        }
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (ex: Exception) {
            null
        }
    }
}
