package com.yujing.tool.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID

/**
 * Local preferences. The auth token lives in EncryptedSharedPreferences when the
 * device supports it; everything else is plain preferences.
 */
class Prefs(context: Context) {

    private val plain: SharedPreferences =
        context.getSharedPreferences("yujing", Context.MODE_PRIVATE)

    private val secure: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "yujing_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (ex: Exception) {
        plain
    }

    var serverUrl: String
        get() = plain.getString(KEY_SERVER, DEFAULT_SERVER) ?: DEFAULT_SERVER
        set(value) {
            val cleaned = value.trim().trimEnd('/').ifEmpty { DEFAULT_SERVER }
            plain.edit().putString(KEY_SERVER, cleaned).apply()
        }

    var token: String?
        get() = secure.getString(KEY_TOKEN, null)
        set(value) {
            secure.edit().putString(KEY_TOKEN, value).apply()
        }

    val deviceId: String
        get() {
            val existing = plain.getString(KEY_DEVICE, null)
            if (existing != null) {
                return existing
            }
            val created = "android-" + UUID.randomUUID().toString()
            plain.edit().putString(KEY_DEVICE, created).apply()
            return created
        }

    var remindersEnabled: Boolean
        get() = plain.getBoolean(KEY_REMINDERS, true)
        set(value) = plain.edit().putBoolean(KEY_REMINDERS, value).apply()

    var notificationsEnabled: Boolean
        get() = plain.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = plain.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    var permissionCardDismissed: Boolean
        get() = plain.getBoolean(KEY_PERMISSION_CARD, false)
        set(value) = plain.edit().putBoolean(KEY_PERMISSION_CARD, value).apply()

    fun isDelivered(key: String): Boolean =
        plain.getStringSet(KEY_DELIVERED, emptySet())?.contains(key) == true

    fun markDelivered(key: String) {
        val current = plain.getStringSet(KEY_DELIVERED, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(key)
        // Keep the dedupe set from growing without bound.
        val trimmed = current.toList().takeLast(300).toSet()
        plain.edit().putStringSet(KEY_DELIVERED, trimmed).apply()
    }

    fun addPendingReceipt(reminderId: Long, result: String) {
        val current = plain.getStringSet(KEY_PENDING_RECEIPTS, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        current.removeAll { it.startsWith("$reminderId|") }
        current.add("$reminderId|$result")
        plain.edit().putStringSet(KEY_PENDING_RECEIPTS, current).apply()
    }

    fun pendingReceipts(): List<Pair<Long, String>> =
        plain.getStringSet(KEY_PENDING_RECEIPTS, emptySet()).orEmpty().mapNotNull { entry ->
            val parts = entry.split('|')
            val id = parts.getOrNull(0)?.toLongOrNull()
            val result = parts.getOrNull(1)
            if (id == null || result == null) null else id to result
        }

    fun clearPendingReceipt(reminderId: Long) {
        val current = plain.getStringSet(KEY_PENDING_RECEIPTS, emptySet())?.toMutableSet()
            ?: mutableSetOf()
        current.removeAll { it.startsWith("$reminderId|") }
        plain.edit().putStringSet(KEY_PENDING_RECEIPTS, current).apply()
    }

    companion object {
        /** Android emulator reaches the host machine through 10.0.2.2. */
        const val DEFAULT_SERVER = "http://10.0.2.2:8080"

        private const val KEY_SERVER = "server_url"
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_DEVICE = "device_id"
        private const val KEY_REMINDERS = "reminders_enabled"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_PERMISSION_CARD = "permission_card_dismissed"
        private const val KEY_DELIVERED = "delivered_keys"
        private const val KEY_PENDING_RECEIPTS = "pending_receipts"
    }
}
