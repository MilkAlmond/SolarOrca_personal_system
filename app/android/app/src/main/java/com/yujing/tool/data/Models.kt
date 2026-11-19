package com.yujing.tool.data

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Long = 0,
    val username: String = "",
    val displayName: String? = null,
    val role: String = "user",
)

@Serializable
data class AuthPayload(
    val token: String? = null,
    val user: User = User(),
)

@Serializable
data class BirthdayItem(
    val id: Long,
    val name: String = "",
    val category: String = "birthday",
    val calendarType: String = "solar",
    val solarDate: String? = null,
    val lunarMonth: Int? = null,
    val lunarDay: Int? = null,
    val lunarLeap: Boolean = false,
    val relationTag: String? = null,
    val advanceDays: Int = 0,
    val note: String? = null,
    val nextDate: String? = null,
    val daysUntil: Long = 0,
    val leapAdjusted: Boolean = false,
    val version: Long = 0,
)

@Serializable
data class BirthdaySaveResult(
    val birthday: BirthdayItem,
    val reminderNotice: String? = null,
)

@Serializable
data class CourseItem(
    val id: Long,
    val name: String = "",
    val teacher: String? = null,
    val location: String? = null,
    val weekday: Int = 1,
    val startTime: String? = null,
    val endTime: String? = null,
    val repeatType: String = "weekly",
    val courseDate: String? = null,
    val effectiveFrom: String? = null,
    val effectiveTo: String? = null,
    val remindMinutes: Int = 15,
    val version: Long = 0,
)

@Serializable
data class CourseSaveResult(
    val course: CourseItem,
    val conflicts: List<CourseItem> = emptyList(),
    val reminderNotice: String? = null,
)

@Serializable
data class TaskItem(
    val id: Long,
    val name: String = "",
    val period: String = "daily",
    val weeklyTarget: Int? = null,
    val startDate: String? = null,
    val remindTime: String? = null,
    val enabled: Boolean = true,
    val streakDays: Int = 0,
    val checkedToday: Boolean = false,
    val weekProgress: Int = 0,
    val weekCompleted: Boolean = false,
    val milestones: List<Int> = emptyList(),
    val version: Long = 0,
)

@Serializable
data class TaskCheckinResult(
    val task: TaskItem,
    val newMilestone: Int? = null,
)

@Serializable
data class NoteSummaryItem(
    val id: Long,
    val title: String = "",
    val category: String? = null,
    val version: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class NoteItem(
    val id: Long,
    val title: String = "",
    val body: String = "",
    val category: String? = null,
    val version: Long = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class PageData<T>(
    val items: List<T> = emptyList(),
    val total: Long = 0,
    val page: Int = 1,
    val size: Int = 20,
)

@Serializable
data class ReminderItem(
    val id: Long,
    val source: String = "manual",
    val sourceId: Long? = null,
    val title: String = "",
    val triggerAt: String = "",
    val advanceMinutes: Int = 0,
    val enabled: Boolean = true,
    val deliveryStatus: String = "pending",
    val sentAt: String? = null,
    val failedAt: String? = null,
    val version: Long = 0,
)

@Serializable
data class SettingsView(
    val remindersEnabled: Boolean = true,
    val version: Long = 0,
)

@Serializable
data class VersionView(
    val platform: String = "android",
    val latestVersion: String = "",
    val minSupportedVersion: String = "",
    val downloadUrl: String? = null,
    val notes: String? = null,
    val readOnly: Boolean = false,
)

@Serializable
data class DeviceView(
    val deviceId: String = "",
    val platform: String = "android",
    val appVersion: String? = null,
    val notificationsEnabled: Boolean = true,
    val lastSeenAt: String? = null,
    val version: Long = 0,
)
