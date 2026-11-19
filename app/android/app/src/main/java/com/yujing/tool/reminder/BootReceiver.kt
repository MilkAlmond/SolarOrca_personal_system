package com.yujing.tool.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-syncs local schedules after reboot or app update (plan §4.5). */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            ReminderScheduler.enqueueCheck(context)
        }
    }
}
