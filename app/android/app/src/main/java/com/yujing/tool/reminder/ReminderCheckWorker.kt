package com.yujing.tool.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yujing.tool.YujingApplication
import com.yujing.tool.data.ApiClient
import com.yujing.tool.data.ApiException
import java.time.OffsetDateTime

/**
 * Server-driven reminder sweep. Runs every 15 minutes and on each launch:
 * flushes receipts that failed earlier, shows notifications for due reminders
 * not yet delivered on this device, then rebuilds the exact-alarm schedule.
 */
class ReminderCheckWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = (applicationContext as YujingApplication).prefs
        if (prefs.token == null) {
            return Result.success()
        }
        val api = ApiClient(prefs)
        return try {
            val settings = api.settings()
            prefs.remindersEnabled = settings.remindersEnabled

            prefs.pendingReceipts().forEach { (id, result) ->
                try {
                    api.receipt(id, result)
                    prefs.clearPendingReceipt(id)
                } catch (ex: Exception) {
                    // Keep it queued for the next sweep.
                }
            }

            val due = api.dueReminders()
            for (item in due) {
                val triggerMillis = parseMillis(item.triggerAt) ?: 0L
                val key = "${item.id}@$triggerMillis"
                if (!prefs.isDelivered(key)) {
                    val shown = settings.remindersEnabled && prefs.notificationsEnabled &&
                        NotificationHelper.show(applicationContext, item.id, item.title, triggerMillis)
                    prefs.markDelivered(key)
                    try {
                        api.receipt(
                            item.id,
                            if (shown) ReminderAlarmReceiver.RESULT_SENT
                            else ReminderAlarmReceiver.RESULT_FAILED,
                        )
                    } catch (ex: Exception) {
                        prefs.addPendingReceipt(item.id, ReminderAlarmReceiver.RESULT_FAILED)
                    }
                }
            }

            val reminders = api.reminders()
            ReminderScheduler.reschedule(applicationContext, reminders, settings.remindersEnabled)
            Result.success()
        } catch (ex: ApiException) {
            if (ex.unauthorized) Result.success() else retryOrGiveUp()
        } catch (ex: Exception) {
            retryOrGiveUp()
        }
    }

    private fun retryOrGiveUp(): Result =
        if (runAttemptCount < 3) Result.retry() else Result.success()

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
