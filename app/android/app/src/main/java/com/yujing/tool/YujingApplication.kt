package com.yujing.tool

import android.app.Application
import com.yujing.tool.data.Prefs
import com.yujing.tool.reminder.ReminderScheduler

class YujingApplication : Application() {

    lateinit var prefs: Prefs
        private set

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        ReminderScheduler.ensureChannel(this)
        ReminderScheduler.enqueuePeriodic(this)
    }
}
