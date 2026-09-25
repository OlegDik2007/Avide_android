package com.avidetravel.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AvideApi {
    private const val BASE = "https://avide.travel"

    suspend fun getServices(): List<Service> = withContext(Dispatchers.IO) {
        val json = getJson("$BASE/api/services")
        rows(json, "services").mapNotNull(::serviceFromJson)
    }

    suspend fun getAgents(): List<Agent> = withContext(Dispatchers.IO) {
        val json = getJson("$BASE/api/agents")
        rows(json, "agents").mapNotNull(::agentFromJson)
    }

    private fun getJson(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "AvideTravel-Android/1.0")

            val status = connection.responseCode
            if (status !in 200..299) {
                throw IllegalStateException("AvideTravel API returned HTTP $status")
            }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun rows(json: String, key: String): List<JSONObject> {
        val trimmed = json.trim()
        val array = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                root.optJSONArray(key) ?: JSONArray()
            }
            else -> JSONArray()
        }

        return buildList {
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let(::add)
            }
        }
    }

    private fun serviceFromJson(o: JSONObject): Service? {
        val id = o.optLong("id", -1L)
        val title = o.string("title")
        if (id < 0 || title.isBlank()) return null

        return Service(
            id = id,
            title = title,
            description = o.string("description"),
            shortDescription = o.string("short_description"),
            category = o.string("category"),
            effectivePrice = o.anyString("effective_price"),
            myPrice = o.anyString("my_price"),
            priceNote = o.anyString("price_note"),
            currency = o.string("currency").ifBlank { "USD" },
            location = o.string("location"),
            duration = o.string("duration"),
            imageUrl = firstImage(o.opt("image_url")) ?: firstImage(o.opt("images")),
            agentId = o.optLongOrNull("agent_id"),
            slug = o.anyString("slug"),
            startDate = o.anyString("start_date"),
            endDate = o.anyString("end_date"),
            flightIncluded = o.optBooleanFlexible("flight_included"),
            transferIncluded = o.optBooleanFlexible("transfer_included"),
            allInclusive = o.optBooleanFlexible("all_inclusive"),
            adultsOnly = o.optBooleanFlexible("adults_only"),
            familyFriendly = o.optBooleanFlexible("family_friendly"),
            beachAccess = o.optBooleanFlexible("beach_access"),
            wifi = o.optBooleanFlexible("wifi"),
            pool = o.optBooleanFlexible("pool")
        )
    }

    private fun agentFromJson(o: JSONObject): Agent? {
        val id = o.optLong("id", -1L)
        if (id < 0) return null
        val name = o.string("name").ifBlank { o.string("company_name").ifBlank { "AvideTravel Agent" } }
        return Agent(
            id = id,
            name = name,
            companyName = o.string("company_name"),
            email = o.string("email"),
            phone = o.string("phone"),
            website = o.string("website"),
            location = o.string("location")
        )
    }

    private fun firstImage(value: Any?): String? {
        if (value == null || value == JSONObject.NULL) return null
        return when (value) {
            is JSONArray -> if (value.length() == 0) null else firstImage(value.opt(0))
            is JSONObject -> normalizeUrl(value.optString("url"))
            is String -> {
                val s = value.trim()
                when {
                    s.isBlank() -> null
                    s.startsWith("[") -> runCatching { firstImage(JSONArray(s)) }.getOrNull()
                    s.startsWith("{") -> runCatching { firstImage(JSONObject(s)) }.getOrNull()
                    s.contains(",") -> normalizeUrl(s.substringBefore(","))
                    else -> normalizeUrl(s)
                }
            }
            else -> null
        }
    }

    private fun normalizeUrl(input: String?): String? {
        var s = input?.trim()?.trim('"', '\'') ?: return null
        if (s.isBlank()) return null
        if (s.startsWith("//")) s = "https:$s"
        if (s.startsWith("http://")) s = "https://" + s.removePrefix("http://")
        if (s.startsWith("/")) s = "$BASE$s"
        return s
    }
}

private fun JSONObject.string(key: String): String =
    opt(key).takeUnless { it == null || it == JSONObject.NULL }?.toString()?.trim().orEmpty()

private fun JSONObject.anyString(key: String): String? =
    opt(key).takeUnless { it == null || it == JSONObject.NULL }?.toString()?.trim()?.takeIf { it.isNotEmpty() }

private fun JSONObject.optLongOrNull(key: String): Long? {
    val value = opt(key)
    if (value == null || value == JSONObject.NULL) return null
    return when (value) {
        is Number -> value.toLong()
        else -> value.toString().toLongOrNull()
    }
}

private fun JSONObject.optBooleanFlexible(key: String): Boolean {
    val value = opt(key)
    return when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> value.equals("true", true) || value == "1" || value.equals("yes", true)
        else -> false
    }
}
