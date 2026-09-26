package com.avidetravel.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject

object SessionStore {
    private const val NORMAL_PREFS = "avide_state"
    private const val SECURE_PREFS = "avide_secure"

    private fun normal(context: Context) =
        context.getSharedPreferences(NORMAL_PREFS, Context.MODE_PRIVATE)

    private fun secure(context: Context) = runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrElse { normal(context) }

    fun saveAuth(context: Context, result: LoginResult) {
        secure(context).edit()
            .putString("token", result.token)
            .putString("user_id", result.user.id)
            .putString("email", result.user.email)
            .putString("role", result.user.role)
            .putString("first_name", result.user.firstName)
            .putString("last_name", result.user.lastName)
            .putString("name", result.user.name)
            .putString("phone", result.user.phone)
            .apply()
        saveGuestIdentity(context, result.user.name, result.user.email)
    }

    fun currentUser(context: Context): UserProfile? {
        val prefs = secure(context)
        val token = prefs.getString("token", null) ?: return null
        if (token.isBlank()) return null
        return UserProfile(
            id = prefs.getString("user_id", "").orEmpty(),
            email = prefs.getString("email", "").orEmpty(),
            role = prefs.getString("role", "customer").orEmpty(),
            firstName = prefs.getString("first_name", "").orEmpty(),
            lastName = prefs.getString("last_name", "").orEmpty(),
            name = prefs.getString("name", "").orEmpty(),
            phone = prefs.getString("phone", "").orEmpty()
        )
    }

    fun authToken(context: Context): String? = secure(context).getString("token", null)

    fun logout(context: Context) {
        secure(context).edit().clear().apply()
    }

    fun saveGuestIdentity(context: Context, name: String, email: String) {
        normal(context).edit()
            .putString("guest_name", name.trim())
            .putString("guest_email", email.trim())
            .apply()
    }

    fun guestName(context: Context): String =
        normal(context).getString("guest_name", "").orEmpty()

    fun guestEmail(context: Context): String =
        normal(context).getString("guest_email", "").orEmpty()

    fun conversationId(context: Context): String? =
        normal(context).getString("conversation_id", null)

    fun saveConversationId(context: Context, id: String) {
        normal(context).edit().putString("conversation_id", id).apply()
    }

    fun clearConversation(context: Context) {
        normal(context).edit().remove("conversation_id").apply()
    }

    fun favoriteIds(context: Context): Set<Long> =
        normal(context).getStringSet("favorites", emptySet())
            .orEmpty()
            .mapNotNull { it.toLongOrNull() }
            .toSet()

    fun toggleFavorite(context: Context, id: Long): Set<Long> {
        val current = favoriteIds(context).toMutableSet()
        if (!current.add(id)) current.remove(id)
        normal(context).edit()
            .putStringSet("favorites", current.map { it.toString() }.toSet())
            .apply()
        return current
    }

    fun notificationsEnabled(context: Context): Boolean =
        normal(context).getBoolean("notifications_enabled", true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        normal(context).edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun lastServiceId(context: Context): Long =
        normal(context).getLong("last_service_id", -1L)

    fun setLastServiceId(context: Context, id: Long) {
        normal(context).edit().putLong("last_service_id", id).apply()
    }

    fun lastTipId(context: Context): Long =
        normal(context).getLong("last_tip_id", -1L)

    fun setLastTipId(context: Context, id: Long) {
        normal(context).edit().putLong("last_tip_id", id).apply()
    }

    fun addInboxItem(context: Context, item: InboxItem) {
        val items = inboxItems(context).toMutableList()
        items.add(0, item)
        val arr = JSONArray()
        items.take(50).forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("body", it.body)
                    .put("type", it.type)
                    .put("refId", it.refId ?: JSONObject.NULL)
                    .put("createdAt", it.createdAt)
                    .put("read", it.read)
            )
        }
        normal(context).edit().putString("notification_inbox", arr.toString()).apply()
    }

    fun inboxItems(context: Context): List<InboxItem> {
        val raw = normal(context).getString("notification_inbox", null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        InboxItem(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            body = o.optString("body"),
                            type = o.optString("type"),
                            refId = o.opt("refId").takeUnless { it == null || it == JSONObject.NULL }?.toString(),
                            createdAt = o.optLong("createdAt"),
                            read = o.optBoolean("read")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun unreadInboxCount(context: Context): Int = inboxItems(context).count { !it.read }

    fun markInboxRead(context: Context) {
        val arr = JSONArray()
        inboxItems(context).forEach {
            arr.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("body", it.body)
                    .put("type", it.type)
                    .put("refId", it.refId ?: JSONObject.NULL)
                    .put("createdAt", it.createdAt)
                    .put("read", true)
            )
        }
        normal(context).edit().putString("notification_inbox", arr.toString()).apply()
    }
}
