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
        val root = requestJson("$BASE/api/services")
        rows(root, "services").mapNotNull(::serviceFromJson)
    }

    suspend fun getAgents(): List<Agent> = withContext(Dispatchers.IO) {
        val root = requestJson("$BASE/api/agents")
        rows(root, "agents").mapNotNull(::agentFromJson)
    }

    suspend fun getTravelTips(): List<TravelTip> = withContext(Dispatchers.IO) {
        val root = requestJson("$BASE/api/travel-tips")
        val topics = root.optJSONArray("topics") ?: JSONArray()
        buildList {
            for (i in 0 until topics.length()) {
                val topic = topics.optJSONObject(i) ?: continue
                val topicSlug = topic.optString("slug")
                val topicTitle = topic.optString("title")
                val tips = topic.optJSONArray("tips") ?: JSONArray()
                for (j in 0 until tips.length()) {
                    val tip = tips.optJSONObject(j) ?: continue
                    val id = tip.optLong("id", -1L)
                    val title = tip.optString("title").trim()
                    if (id < 0 || title.isBlank()) continue
                    add(
                        TravelTip(
                            id = id,
                            topicSlug = topicSlug,
                            topicTitle = topicTitle,
                            title = title,
                            slug = tip.optString("slug"),
                            excerpt = cleanHtml(tip.optString("excerpt")),
                            content = cleanHtml(tip.optString("content")),
                            coverImage = normalizeUrl(tip.optString("coverImage")).takeIf { !it.isNullOrBlank() },
                            createdAt = tip.optNullableString("createdAt"),
                            updatedAt = tip.optNullableString("updatedAt")
                        )
                    )
                }
            }
        }.sortedByDescending { it.id }
    }

    suspend fun getVideoReviews(): List<VideoReview> = withContext(Dispatchers.IO) {
        val root = requestJson("$BASE/api/hotel-reviews")
        val hotels = root.optJSONArray("hotels") ?: JSONArray()
        buildList {
            for (i in 0 until hotels.length()) {
                val hotel = hotels.optJSONObject(i) ?: continue
                val featured = hotel.optJSONObject("featuredVideo") ?: continue
                val videoId = featured.optString("youtubeVideoId").trim()
                if (videoId.isBlank()) continue
                val youtubeUrl = featured.optString("youtubeUrl").ifBlank {
                    "https://www.youtube.com/watch?v=$videoId"
                }
                add(
                    VideoReview(
                        id = featured.optLong("id", i.toLong()),
                        hotelName = hotel.optString("hotelName"),
                        hotelSlug = hotel.optString("hotelSlug"),
                        location = hotel.optString("location"),
                        title = featured.optString("title").ifBlank {
                            "${hotel.optString("hotelName")} video review"
                        },
                        channelName = featured.optString("channelName"),
                        youtubeVideoId = videoId,
                        youtubeUrl = youtubeUrl,
                        thumbnailUrl = normalizeUrl(hotel.optString("coverImageUrl"))
                            ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                        isAvideOriginal = featured.optBooleanFlexible("isAvideOriginal")
                    )
                )
            }
        }
    }

    suspend fun getReviewForService(serviceId: Long): ServiceVideoReview? = withContext(Dispatchers.IO) {
        val root = requestJson("$BASE/api/hotel-reviews?service_id=$serviceId")
        val hotel = root.optJSONObject("hotel") ?: return@withContext null
        val review = hotel.optJSONObject("review") ?: return@withContext null
        val videoId = review.optString("youtubeVideoId").trim()
        if (videoId.isBlank()) return@withContext null

        ServiceVideoReview(
            hotelName = hotel.optString("hotelName"),
            hotelSlug = hotel.optString("hotelSlug"),
            location = hotel.optString("location"),
            review = VideoReview(
                id = review.optLong("id", 0L),
                hotelName = hotel.optString("hotelName"),
                hotelSlug = hotel.optString("hotelSlug"),
                location = hotel.optString("location"),
                title = review.optString("title").ifBlank { "Resort video review" },
                channelName = review.optString("channelName"),
                youtubeVideoId = videoId,
                youtubeUrl = "https://www.youtube.com/watch?v=$videoId",
                thumbnailUrl = normalizeUrl(review.optString("thumbnailUrl"))
                    ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
                isAvideOriginal = review.optBooleanFlexible("isAvideOriginal")
            )
        )
    }

    suspend fun getChatMessages(conversationId: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val root = requestJson(
            "$BASE/api/website-chat/messages?conversationId=" +
                java.net.URLEncoder.encode(conversationId, "UTF-8")
        )
        val arr = root.optJSONArray("messages") ?: JSONArray()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    ChatMessage(
                        id = o.optString("id", i.toString()),
                        text = o.optString("text"),
                        sender = o.optString("sender"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        senderName = o.optNullableString("userName")
                    )
                )
            }
        }
    }

    suspend fun sendChat(
        conversationId: String?,
        userName: String,
        userEmail: String,
        message: String,
        service: Service? = null
    ): ChatSendResult = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("conversationId", conversationId ?: "")
            .put("userName", userName)
            .put("userEmail", userEmail)
            .put("message", message)
            .put("serviceId", service?.id ?: JSONObject.NULL)
            .put("serviceTitle", service?.title ?: "")
            .put("pageUrl", if (service != null) "android://deal/${service.id}" else "android://chat")

        val root = requestJson("$BASE/api/website-chat", "POST", payload)
        val id = root.optString("conversationId")
        if (id.isBlank()) throw IllegalStateException(root.optString("error", "Chat failed"))
        val msg = root.optJSONObject("message")?.let {
            ChatMessage(
                id = it.opt("id")?.toString() ?: System.currentTimeMillis().toString(),
                text = it.optString("message_text", message),
                sender = "user",
                timestamp = System.currentTimeMillis(),
                senderName = userName
            )
        }
        ChatSendResult(id, msg)
    }

    suspend fun exchangeGoogleCode(code: String): LoginResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("code", code)
        val root = requestJson("$BASE/api/mobile/auth/google/exchange", "POST", payload)
        if (!root.optBoolean("ok")) {
            throw IllegalStateException(root.optString("error", "Google sign in failed"))
        }
        val user = root.optJSONObject("user") ?: throw IllegalStateException("Missing user profile")
        LoginResult(
            token = root.optString("token"),
            user = UserProfile(
                id = user.optString("id"),
                email = user.optString("email"),
                role = user.optString("role", "customer"),
                firstName = user.optString("firstName"),
                lastName = user.optString("lastName"),
                name = user.optString("name"),
                phone = user.optString("phone")
            )
        )
    }

    suspend fun login(email: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("email", email).put("password", password)
        val root = requestJson("$BASE/api/mobile/auth/login", "POST", payload)
        if (!root.optBoolean("ok")) {
            throw IllegalStateException(root.optString("error", "Unable to sign in"))
        }
        val user = root.optJSONObject("user") ?: throw IllegalStateException("Missing user profile")
        LoginResult(
            token = root.optString("token"),
            user = UserProfile(
                id = user.optString("id"),
                email = user.optString("email"),
                role = user.optString("role", "customer"),
                firstName = user.optString("firstName"),
                lastName = user.optString("lastName"),
                name = user.optString("name"),
                phone = user.optString("phone")
            )
        )
    }

    suspend fun signup(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        password: String,
        consentEmail: Boolean,
        consentSms: Boolean
    ) = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("first_name", firstName)
            .put("last_name", lastName)
            .put("email", email)
            .put("phone", phone)
            .put("password", password)
            .put("consent_email", consentEmail)
            .put("consent_sms", consentSms)

        val root = requestJson("$BASE/api/auth/signup", "POST", payload)
        if (!root.optBoolean("ok")) {
            throw IllegalStateException(root.optString("error", "Unable to create account"))
        }
    }

    private fun requestJson(
        url: String,
        method: String = "GET",
        body: JSONObject? = null
    ): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "AvideTravel-Android/2.0")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter().use { it.write(body.toString()) }
            }

            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            val root = if (text.trim().startsWith("{")) JSONObject(text) else JSONObject()
            if (status !in 200..299) {
                throw IllegalStateException(
                    root.optString("error").ifBlank { "AvideTravel API returned HTTP $status" }
                )
            }
            root
        } finally {
            connection.disconnect()
        }
    }

    private fun rows(root: JSONObject, key: String): List<JSONObject> {
        val array = root.optJSONArray(key) ?: JSONArray()
        return buildList {
            for (i in 0 until array.length()) array.optJSONObject(i)?.let(::add)
        }
    }

    private fun serviceFromJson(o: JSONObject): Service? {
        val id = o.optLong("id", -1L)
        val title = o.optString("title").trim()
        if (id < 0 || title.isBlank()) return null

        val images = (
            imageList(o.opt("image_url")) +
            imageList(o.opt("images")) +
            imageList(o.opt("place_image_google_urls"))
        ).distinct().take(12)

        return Service(
            id = id,
            title = title,
            description = cleanHtml(o.optString("description")),
            shortDescription = cleanHtml(o.optString("short_description")),
            category = o.optString("category"),
            effectivePrice = o.optNullableString("effective_price"),
            myPrice = o.optNullableString("my_price"),
            priceNote = o.optNullableString("price_note"),
            currency = o.optString("currency").ifBlank { "USD" },
            location = o.optString("location"),
            duration = o.optString("duration"),
            imageUrl = images.firstOrNull(),
            imageUrls = images,
            agentId = o.optLongOrNull("agent_id"),
            slug = o.optNullableString("slug"),
            startDate = o.optNullableString("start_date"),
            endDate = o.optNullableString("end_date"),
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
        val name = o.optString("name").ifBlank {
            o.optString("company_name").ifBlank { "AvideTravel Agent" }
        }
        return Agent(
            id = id,
            name = name,
            companyName = o.optString("company_name"),
            email = o.optString("email"),
            phone = o.optString("phone"),
            website = o.optString("website"),
            location = o.optString("location")
        )
    }

    private fun imageList(value: Any?): List<String> {
        if (value == null || value == JSONObject.NULL) return emptyList()
        return when (value) {
            is JSONArray -> buildList {
                for (i in 0 until value.length()) {
                    val item = value.opt(i)
                    when (item) {
                        is String -> normalizeUrl(item)?.let(::add)
                        is JSONObject -> normalizeUrl(item.optString("url"))?.let(::add)
                    }
                }
            }
            is JSONObject -> listOfNotNull(normalizeUrl(value.optString("url")))
            is String -> {
                val s = value.trim()
                when {
                    s.isBlank() -> emptyList()
                    s.startsWith("[") -> runCatching { imageList(JSONArray(s)) }.getOrDefault(emptyList())
                    s.startsWith("{") -> runCatching { imageList(JSONObject(s)) }.getOrDefault(emptyList())
                    s.contains(",") -> s.split(",").mapNotNull(::normalizeUrl)
                    else -> listOfNotNull(normalizeUrl(s))
                }
            }
            else -> emptyList()
        }
    }

    private fun normalizeUrl(input: String?): String? {
        var s = input?.trim()?.trim('"', '\'') ?: return null
        if (s.isBlank() || s == "null") return null
        if (s.startsWith("//")) s = "https:$s"
        if (s.startsWith("http://")) s = "https://" + s.removePrefix("http://")
        if (s.startsWith("/")) s = "$BASE$s"
        return s.takeIf { it.startsWith("https://") }
    }

    private fun cleanHtml(value: String): String = value
        .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .trim()
}

private fun JSONObject.optNullableString(key: String): String? =
    opt(key).takeUnless { it == null || it == JSONObject.NULL }
        ?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

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
