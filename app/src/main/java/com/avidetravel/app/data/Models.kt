package com.avidetravel.app.data

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

data class Service(
    val id: Long,
    val title: String,
    val description: String = "",
    val shortDescription: String = "",
    val category: String = "",
    val effectivePrice: String? = null,
    val myPrice: String? = null,
    val priceNote: String? = null,
    val currency: String = "USD",
    val location: String = "",
    val duration: String = "",
    val imageUrl: String? = null,
    val imageUrls: List<String> = emptyList(),
    val agentId: Long? = null,
    val slug: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val flightIncluded: Boolean = false,
    val transferIncluded: Boolean = false,
    val allInclusive: Boolean = false,
    val adultsOnly: Boolean = false,
    val familyFriendly: Boolean = false,
    val beachAccess: Boolean = false,
    val wifi: Boolean = false,
    val pool: Boolean = false
) {
    fun displayPrice(): String {
        val raw = effectivePrice ?: myPrice
        val value = raw?.replace(Regex("[^0-9.]"), "")?.toDoubleOrNull()

        if (value == null) return priceNote?.takeIf { it.isNotBlank() } ?: "Ask agent for price"

        val currencyCode = currency.ifBlank { "USD" }
        return runCatching {
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                currency = Currency.getInstance(currencyCode)
                maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
            }.format(value)
        }.getOrElse {
            "$" + if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale.US, value)
        }
    }
}

data class Agent(
    val id: Long,
    val name: String,
    val companyName: String = "",
    val email: String = "",
    val phone: String = "",
    val website: String = "",
    val location: String = ""
)

data class TravelTip(
    val id: Long,
    val topicSlug: String,
    val topicTitle: String,
    val title: String,
    val slug: String,
    val excerpt: String = "",
    val content: String = "",
    val coverImage: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class VideoReview(
    val id: Long,
    val hotelName: String,
    val hotelSlug: String,
    val location: String = "",
    val title: String,
    val channelName: String = "",
    val youtubeVideoId: String,
    val youtubeUrl: String,
    val thumbnailUrl: String,
    val isAvideOriginal: Boolean = false
)

data class ServiceVideoReview(
    val hotelName: String,
    val hotelSlug: String,
    val location: String = "",
    val review: VideoReview
)

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: String,
    val timestamp: Long,
    val senderName: String? = null
)

data class ChatSendResult(
    val conversationId: String,
    val message: ChatMessage?
)

data class UserProfile(
    val id: String,
    val email: String,
    val role: String = "customer",
    val firstName: String = "",
    val lastName: String = "",
    val name: String = "",
    val phone: String = ""
)

data class LoginResult(
    val token: String,
    val user: UserProfile
)

data class InboxItem(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    val refId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
