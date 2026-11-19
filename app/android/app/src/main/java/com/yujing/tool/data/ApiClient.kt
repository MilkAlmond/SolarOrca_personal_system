package com.yujing.tool.data

import com.yujing.tool.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.OffsetDateTime
import java.util.TimeZone
import java.util.concurrent.TimeUnit

@Serializable
private data class Envelope<T>(
    val code: Int = -1,
    val message: String? = null,
    val data: T? = null,
)

class ApiException(
    val httpStatus: Int,
    val code: Int,
    override val message: String,
) : Exception(message) {

    val unauthorized: Boolean get() = code == 2001 || httpStatus == 401
    val versionConflict: Boolean get() = code == 4002
    val frozen: Boolean get() = code == 4005
}

/**
 * Thin HTTP client over the same /api/v1 contract used by the web client.
 * Native clients identify themselves with X-Client-Platform: android and get a
 * bearer token from login/register instead of a cookie.
 */
class ApiClient(private val prefs: Prefs) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private val http = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun offsetMinutes(): Int =
        TimeZone.getDefault().getOffset(System.currentTimeMillis()) / 60000

    private suspend fun <T> call(
        method: String,
        path: String,
        body: String?,
        serializer: KSerializer<T>,
        auth: Boolean = true,
    ): T = withContext(Dispatchers.IO) {
        val url = prefs.serverUrl + "/api/v1" + path
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("X-Client-Platform", "android")
            .header("X-Client-Version", BuildConfig.VERSION_NAME)
            .header("X-Device-Id", prefs.deviceId)
            .header("X-Client-Offset-Minutes", offsetMinutes().toString())
        if (auth) {
            prefs.token?.let { builder.header("Authorization", "Bearer $it") }
        }
        val media = "application/json; charset=utf-8".toMediaType()
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete(body?.toRequestBody(media))
            else -> builder.method(method, body?.toRequestBody(media) ?: "".toRequestBody(media))
        }

        val response = try {
            http.newCall(builder.build()).execute()
        } catch (ex: IOException) {
            throw ApiException(0, -1, "无法连接服务器，请检查网络与服务地址")
        }
        response.use { res ->
            val text = res.body?.string().orEmpty()
            val envelope = try {
                json.decodeFromString(Envelope.serializer(serializer), text)
            } catch (ex: SerializationException) {
                throw ApiException(res.code, -1, "服务器返回异常（HTTP ${res.code}）")
            }
            if (!res.isSuccessful || envelope.code != 0) {
                throw ApiException(
                    res.code,
                    envelope.code,
                    envelope.message ?: "请求失败（${res.code}）",
                )
            }
            @Suppress("UNCHECKED_CAST")
            return@withContext (envelope.data as T?) ?: nullData(serializer)
        }
    }

    /**
     * Endpoints such as DELETE and logout answer with `data: null`. Every other
     * call must return a payload, so a missing payload is a server anomaly.
     */
    private fun <T> nullData(serializer: KSerializer<T>): T {
        if (serializer.descriptor.serialName == "kotlinx.serialization.json.JsonElement") {
            @Suppress("UNCHECKED_CAST")
            return JsonNull as T
        }
        throw ApiException(500, -1, "服务器未返回数据")
    }

    private suspend fun <T> get(path: String, serializer: KSerializer<T>, auth: Boolean = true): T =
        call("GET", path, null, serializer, auth)

    private suspend fun <T> post(path: String, body: String, serializer: KSerializer<T>): T =
        call("POST", path, body, serializer)

    private suspend fun <T> put(path: String, body: String, serializer: KSerializer<T>): T =
        call("PUT", path, body, serializer)

    private suspend fun <T> patch(path: String, body: String, serializer: KSerializer<T>): T =
        call("PATCH", path, body, serializer)

    private suspend fun delete(path: String) {
        call("DELETE", path, null, JsonElement.serializer())
    }

    /* ---------- Auth ---------- */

    suspend fun login(username: String, password: String): AuthPayload {
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }.toString()
        return post("/auth/login", body, AuthPayload.serializer())
    }

    suspend fun register(username: String, password: String): AuthPayload {
        val body = buildJsonObject {
            put("username", username)
            put("password", password)
        }.toString()
        return post("/auth/register", body, AuthPayload.serializer())
    }

    suspend fun me(): User = get("/auth/me", User.serializer())

    suspend fun logout() {
        call("POST", "/auth/logout", "{}", JsonElement.serializer())
    }

    /* ---------- Birthdays ---------- */

    suspend fun birthdays(): List<BirthdayItem> =
        get("/birthdays", ListSerializer(BirthdayItem.serializer()))

    suspend fun createBirthday(
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
    ): BirthdaySaveResult {
        val body = buildJsonObject {
            put("name", name)
            put("category", category)
            put("calendarType", calendarType)
            if (calendarType == "lunar") {
                put("lunarMonth", lunarMonth)
                put("lunarDay", lunarDay)
                put("lunarLeap", lunarLeap)
            } else {
                if (solarDate != null) put("solarDate", solarDate)
            }
            if (!relationTag.isNullOrBlank()) put("relationTag", relationTag)
            put("advanceDays", advanceDays)
            if (!note.isNullOrBlank()) put("note", note)
        }.toString()
        return post("/birthdays", body, BirthdaySaveResult.serializer())
    }

    suspend fun deleteBirthday(id: Long, version: Long) {
        delete("/birthdays/$id?version=$version")
    }

    /* ---------- Courses ---------- */

    suspend fun courses(): List<CourseItem> =
        get("/courses", ListSerializer(CourseItem.serializer()))

    suspend fun createCourse(
        name: String,
        teacher: String?,
        location: String?,
        weekday: Int,
        startTime: String,
        endTime: String,
        repeatType: String,
        courseDate: String?,
        remindMinutes: Int,
    ): CourseSaveResult {
        val body = buildJsonObject {
            put("name", name)
            if (!teacher.isNullOrBlank()) put("teacher", teacher)
            if (!location.isNullOrBlank()) put("location", location)
            put("weekday", weekday)
            put("startTime", startTime)
            put("endTime", endTime)
            put("repeatType", repeatType)
            if (repeatType == "once" && courseDate != null) put("courseDate", courseDate)
            put("remindMinutes", remindMinutes)
        }.toString()
        return post("/courses", body, CourseSaveResult.serializer())
    }

    suspend fun deleteCourse(id: Long, version: Long) {
        delete("/courses/$id?version=$version")
    }

    /* ---------- Tasks ---------- */

    suspend fun todayTasks(): List<TaskItem> =
        get("/tasks/today", ListSerializer(TaskItem.serializer()))

    suspend fun createTask(
        name: String,
        period: String,
        weeklyTarget: Int?,
        startDate: String,
        remindTime: String?,
    ): TaskItem {
        val body = buildJsonObject {
            put("name", name)
            put("period", period)
            if (period == "weekly" && weeklyTarget != null) put("weeklyTarget", weeklyTarget)
            put("startDate", startDate)
            if (!remindTime.isNullOrBlank()) put("remindTime", remindTime)
            put("enabled", true)
        }.toString()
        return post("/tasks", body, TaskItem.serializer())
    }

    suspend fun checkinTask(id: Long, date: String): TaskCheckinResult {
        val payload = buildJsonObject { put("date", date) }.toString()
        return post("/tasks/$id/checkins", payload, TaskCheckinResult.serializer())
    }

    suspend fun undoCheckin(id: Long, date: String) {
        delete("/tasks/$id/checkins?date=$date")
    }

    suspend fun deleteTask(id: Long, version: Long) {
        delete("/tasks/$id?version=$version")
    }

    /* ---------- Notes ---------- */

    suspend fun notes(keyword: String): List<NoteSummaryItem> {
        val encoded = java.net.URLEncoder.encode(keyword, "UTF-8")
        return get(
            "/notes?page=1&size=50&keyword=$encoded",
            PageData.serializer(NoteSummaryItem.serializer()),
        ).items
    }

    suspend fun note(id: Long): NoteItem = get("/notes/$id", NoteItem.serializer())

    suspend fun createNote(title: String, body: String): NoteItem {
        val payload = buildJsonObject {
            put("title", title)
            put("body", body)
        }.toString()
        return post("/notes", payload, NoteItem.serializer())
    }

    suspend fun updateNote(id: Long, title: String, body: String, version: Long): NoteItem {
        val payload = buildJsonObject {
            put("title", title)
            put("body", body)
            put("version", version)
        }.toString()
        return put("/notes/$id", payload, NoteItem.serializer())
    }

    suspend fun deleteNote(id: Long, version: Long) {
        delete("/notes/$id?version=$version")
    }

    /* ---------- Reminders ---------- */

    suspend fun reminders(): List<ReminderItem> =
        get("/reminders", ListSerializer(ReminderItem.serializer()))

    suspend fun dueReminders(): List<ReminderItem> =
        get("/reminders/due", ListSerializer(ReminderItem.serializer()))

    suspend fun createReminder(title: String, triggerAt: OffsetDateTime): ReminderItem {
        val payload = buildJsonObject {
            put("title", title)
            put("triggerAt", triggerAt.toString())
        }.toString()
        return post("/reminders", payload, ReminderItem.serializer())
    }

    suspend fun setReminderEnabled(id: Long, enabled: Boolean, version: Long): ReminderItem {
        val payload = buildJsonObject {
            put("enabled", enabled)
            put("version", version)
        }.toString()
        return patch("/reminders/$id", payload, ReminderItem.serializer())
    }

    suspend fun deleteReminder(id: Long, version: Long) {
        delete("/reminders/$id?version=$version")
    }

    suspend fun receipt(id: Long, result: String): ReminderItem {
        val payload = buildJsonObject {
            put("deviceId", prefs.deviceId)
            put("result", result)
        }.toString()
        return post("/reminders/$id/receipts", payload, ReminderItem.serializer())
    }

    /* ---------- Settings / devices / version ---------- */

    suspend fun settings(): SettingsView = get("/settings", SettingsView.serializer())

    suspend fun updateSettings(enabled: Boolean): SettingsView {
        val payload = buildJsonObject { put("remindersEnabled", enabled) }.toString()
        return patch("/settings", payload, SettingsView.serializer())
    }

    suspend fun registerDevice(notificationsEnabled: Boolean): DeviceView {
        val payload = buildJsonObject {
            put("deviceId", prefs.deviceId)
            put("platform", "android")
            put("appVersion", BuildConfig.VERSION_NAME)
            put("notificationsEnabled", notificationsEnabled)
        }.toString()
        return post("/devices", payload, DeviceView.serializer())
    }

    suspend fun appVersion(): VersionView {
        return get(
            "/app/version?platform=android&current=${BuildConfig.VERSION_NAME}",
            VersionView.serializer(),
            auth = false,
        )
    }
}
