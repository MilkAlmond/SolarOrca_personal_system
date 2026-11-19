package com.yujing.tool.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yujing.tool.YujingApplication
import com.yujing.tool.data.ApiClient
import com.yujing.tool.data.ApiException
import com.yujing.tool.data.BirthdayItem
import com.yujing.tool.data.CourseItem
import com.yujing.tool.data.NoteItem
import com.yujing.tool.data.NoteSummaryItem
import com.yujing.tool.data.ReminderItem
import com.yujing.tool.data.SettingsView
import com.yujing.tool.data.TaskItem
import com.yujing.tool.data.User
import com.yujing.tool.data.VersionView
import com.yujing.tool.reminder.ReminderScheduler
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as YujingApplication
    private val prefs = app.prefs
    private val api = ApiClient(prefs)

    var session by mutableStateOf<User?>(null)
        private set
    var checkingSession by mutableStateOf(true)
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var serverUrl by mutableStateOf(prefs.serverUrl)
        private set

    var tasks by mutableStateOf<List<TaskItem>>(emptyList())
        private set
    var birthdays by mutableStateOf<List<BirthdayItem>>(emptyList())
        private set
    var courses by mutableStateOf<List<CourseItem>>(emptyList())
        private set
    var notes by mutableStateOf<List<NoteSummaryItem>>(emptyList())
        private set
    var reminders by mutableStateOf<List<ReminderItem>>(emptyList())
        private set
    var settings by mutableStateOf<SettingsView?>(null)
        private set
    var versionInfo by mutableStateOf<VersionView?>(null)
        private set
    var readOnly by mutableStateOf(false)
        private set
    var noteKeyword by mutableStateOf("")
        private set
    var permissionCardDismissed by mutableStateOf(prefs.permissionCardDismissed)
        private set

    init {
        bootstrap()
    }

    fun clearMessage() {
        message = null
    }

    fun bootstrap() {
        viewModelScope.launch {
            if (prefs.token == null) {
                checkingSession = false
                return@launch
            }
            try {
                session = api.me()
                refreshAll()
                ReminderScheduler.enqueueCheck(app)
            } catch (ex: Exception) {
                prefs.token = null
                session = null
            } finally {
                checkingSession = false
            }
        }
    }

    fun login(username: String, password: String, url: String) {
        authenticate(url) { api.login(username.trim(), password) }
    }

    fun register(username: String, password: String, url: String) {
        authenticate(url) { api.register(username.trim(), password) }
    }

    private fun authenticate(url: String, action: suspend () -> com.yujing.tool.data.AuthPayload) {
        if (busy) {
            return
        }
        viewModelScope.launch {
            busy = true
            try {
                prefs.serverUrl = url
                serverUrl = prefs.serverUrl
                val payload = action()
                val token = payload.token
                if (token.isNullOrBlank()) {
                    message = "服务器未返回登录凭据"
                    return@launch
                }
                prefs.token = token
                session = payload.user
                registerDevice()
                refreshAll()
                ReminderScheduler.enqueueCheck(app)
            } catch (ex: ApiException) {
                message = if (ex.frozen) "账号注销冻结中，无法登录" else ex.message
            } catch (ex: Exception) {
                message = "登录失败：${ex.message}"
            } finally {
                busy = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                api.logout()
            } catch (ex: Exception) {
                // Local sign-out always wins.
            }
            prefs.token = null
            session = null
            tasks = emptyList()
            birthdays = emptyList()
            courses = emptyList()
            notes = emptyList()
            reminders = emptyList()
            settings = null
            versionInfo = null
            readOnly = false
            ReminderScheduler.cancelAll(app)
        }
    }

    fun registerDevice() {
        viewModelScope.launch {
            try {
                api.registerDevice(prefs.notificationsEnabled)
            } catch (ex: Exception) {
                // Device registration is best-effort.
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            busy = true
            try {
                tasks = api.todayTasks()
                birthdays = api.birthdays()
                courses = api.courses()
                notes = api.notes(noteKeyword)
                reminders = api.reminders()
                settings = api.settings()
                prefs.remindersEnabled = settings?.remindersEnabled ?: true
                ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
                loadVersion()
            } catch (ex: Exception) {
                handle(ex)
            } finally {
                busy = false
            }
        }
    }

    fun loadVersion() {
        viewModelScope.launch {
            try {
                versionInfo = api.appVersion()
                readOnly = versionInfo?.readOnly == true
            } catch (ex: Exception) {
                versionInfo = null
            }
        }
    }

    fun checkVersion() {
        viewModelScope.launch {
            try {
                val info = api.appVersion()
                versionInfo = info
                readOnly = info.readOnly
                message = if (info.latestVersion == com.yujing.tool.BuildConfig.VERSION_NAME) {
                    "已是最新版本 ${info.latestVersion}"
                } else {
                    "发现新版本 ${info.latestVersion}"
                }
            } catch (ex: Exception) {
                handle(ex)
            }
        }
    }

    /* ---------- Tasks ---------- */

    fun addTask(name: String, period: String, weeklyTarget: Int?, remindTime: String?) {
        if (!writable()) return
        mutate("任务已创建") {
            api.createTask(name.trim(), period, weeklyTarget, com.yujing.tool.data.Formats.today(), remindTime)
            tasks = api.todayTasks()
            reminders = api.reminders()
            ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
        }
    }

    fun checkin(task: TaskItem) {
        if (!writable()) return
        viewModelScope.launch {
            try {
                val result = api.checkinTask(task.id, com.yujing.tool.data.Formats.today())
                tasks = tasks.map { if (it.id == result.task.id) result.task else it }
                val milestone = result.newMilestone
                message = if (milestone != null) {
                    "已连续打卡 $milestone 天，达成里程碑"
                } else {
                    "已打卡，当前连续 ${result.task.streakDays} 天"
                }
            } catch (ex: Exception) {
                handle(ex)
            }
        }
    }

    fun undoCheckin(task: TaskItem) {
        if (!writable()) return
        viewModelScope.launch {
            try {
                val updated = api.undoCheckin(task.id, com.yujing.tool.data.Formats.today())
                tasks = tasks.map { if (it.id == updated.id) updated else it }
                message = "已撤销今日打卡"
            } catch (ex: Exception) {
                handle(ex)
            }
        }
    }

    fun deleteTask(task: TaskItem) {
        if (!writable()) return
        mutate("任务已删除") {
            api.deleteTask(task.id, task.version)
            tasks = api.todayTasks()
        }
    }

    /* ---------- Birthdays ---------- */

    fun addBirthday(
        name: String,
        category: String,
        calendarType: String,
        solarDate: String?,
        lunarMonth: Int?,
        lunarDay: Int?,
        lunarLeap: Boolean,
        relationTag: String?,
        advanceDays: Int,
        note: String?,
    ) {
        if (!writable()) return
        mutate("生日/纪念日已保存") {
            api.createBirthday(
                name.trim(), category, calendarType, solarDate, lunarMonth, lunarDay,
                lunarLeap, relationTag, advanceDays, note,
            )
            birthdays = api.birthdays()
            reminders = api.reminders()
        }
    }

    fun deleteBirthday(item: BirthdayItem) {
        if (!writable()) return
        mutate("记录已删除") {
            api.deleteBirthday(item.id, item.version)
            birthdays = api.birthdays()
            reminders = api.reminders()
            ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
        }
    }

    /* ---------- Courses ---------- */

    fun addCourse(
        name: String,
        teacher: String?,
        location: String?,
        weekday: Int,
        startTime: String,
        endTime: String,
        repeatType: String,
        courseDate: String?,
        remindMinutes: Int,
    ) {
        if (!writable()) return
        mutate("课程已保存") {
            api.createCourse(
                name.trim(), teacher, location, weekday, startTime, endTime,
                repeatType, courseDate, remindMinutes,
            )
            courses = api.courses()
            reminders = api.reminders()
        }
    }

    fun deleteCourse(item: CourseItem) {
        if (!writable()) return
        mutate("课程已删除") {
            api.deleteCourse(item.id, item.version)
            courses = api.courses()
            reminders = api.reminders()
            ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
        }
    }

    /* ---------- Notes ---------- */

    fun searchNotes(keyword: String) {
        noteKeyword = keyword
        viewModelScope.launch {
            try {
                notes = api.notes(keyword)
            } catch (ex: Exception) {
                handle(ex)
            }
        }
    }

    suspend fun loadNote(id: Long): NoteItem = api.note(id)

    suspend fun saveNote(id: Long?, title: String, body: String, version: Long?): NoteItem {
        val saved = if (id == null) {
            api.createNote(title, body)
        } else {
            api.updateNote(id, title, body, version ?: 0)
        }
        notes = api.notes(noteKeyword)
        return saved
    }

    fun deleteNote(item: NoteSummaryItem) {
        if (!writable()) return
        mutate("笔记已删除") {
            api.deleteNote(item.id, item.version)
            notes = api.notes(noteKeyword)
        }
    }

    /* ---------- Reminders ---------- */

    fun addReminder(title: String, triggerAt: OffsetDateTime) {
        if (!writable()) return
        mutate("提醒已创建") {
            api.createReminder(title.trim(), triggerAt)
            reminders = api.reminders()
            ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
        }
    }

    fun setReminderEnabled(item: ReminderItem, enabled: Boolean) {
        if (!writable()) return
        viewModelScope.launch {
            try {
                val updated = api.setReminderEnabled(item.id, enabled, item.version)
                reminders = reminders.map { if (it.id == updated.id) updated else it }
                ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
            } catch (ex: Exception) {
                handle(ex)
            }
        }
    }

    fun deleteReminder(item: ReminderItem) {
        if (!writable()) return
        mutate("提醒已删除") {
            api.deleteReminder(item.id, item.version)
            reminders = api.reminders()
            ReminderScheduler.reschedule(app, reminders, prefs.remindersEnabled)
        }
    }

    fun setRemindersEnabled(enabled: Boolean) {
        if (!writable()) return
        viewModelScope.launch {
            try {
                settings = api.updateSettings(enabled)
                prefs.remindersEnabled = enabled
                ReminderScheduler.reschedule(app, reminders, enabled)
                message = if (enabled) "提醒已开启" else "提醒已关闭"
            } catch (ex: Exception) {
                handle(ex)
            }
        }
    }

    /* ---------- Permissions ---------- */

    fun setNotificationsPreference(enabled: Boolean) {
        prefs.notificationsEnabled = enabled
        registerDevice()
    }

    fun updateServerUrl(url: String) {
        prefs.serverUrl = url
        serverUrl = prefs.serverUrl
        message = "服务地址已保存，同步后生效"
    }

    fun dismissPermissionCard() {
        prefs.permissionCardDismissed = true
        permissionCardDismissed = true
    }

    /* ---------- Helpers ---------- */

    private fun writable(): Boolean {
        if (readOnly) {
            message = "当前版本过低，仅可查看"
            return false
        }
        return true
    }

    private fun mutate(success: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            busy = true
            try {
                action()
                message = success
            } catch (ex: Exception) {
                handle(ex)
            } finally {
                busy = false
            }
        }
    }

    private fun handle(ex: Exception) {
        if (ex is ApiException) {
            if (ex.unauthorized) {
                prefs.token = null
                session = null
                message = "登录已失效，请重新登录"
                return
            }
            if (ex.code == 4009) {
                readOnly = true
            }
            if (ex.versionConflict) {
                message = "数据已被其他设备修改，请刷新后重试"
                refreshAll()
                return
            }
            message = ex.message
        } else {
            message = "操作失败：${ex.message}"
        }
    }
}
