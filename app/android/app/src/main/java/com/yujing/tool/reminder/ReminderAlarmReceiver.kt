package com.yujing.tool.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.yujing.tool.YujingApplication
import java.time.OffsetDateTime

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1)
        if (id <= 0) {
            return
        }
        val app = context.applicationContext as YujingApplication
        val prefs = app.prefs
        // The global switch wins over any locally scheduled alarm (plan §4.4).
        if (!prefs.remindersEnabled) {
            return
        }
        val triggerAt = intent.getLongExtra(EXTRA_TRIGGER, 0)
        val key = "$id@$triggerAt"
        if (prefs.isDelivered(key)) {
            return
        }
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val shown = prefs.notificationsEnabled &&
            NotificationHelper.show(context, id, title, triggerAt)
        prefs.markDelivered(key)
        ReminderReceiptWorker.enqueue(context, id, if (shown) RESULT_SENT else RESULT_FAILED)
    }

    companion object {
        const val EXTRA_ID = "reminderId"
        const val EXTRA_TITLE = "reminderTitle"
        const val EXTRA_TRIGGER = "reminderTrigger"
        const val RESULT_SENT = "sent"
        const val RESULT_FAILED = "failed"
    }
}
