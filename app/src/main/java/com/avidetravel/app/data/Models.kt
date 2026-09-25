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
        val value = raw
            ?.replace(Regex("[^0-9.]"), "")
            ?.toDoubleOrNull()

        if (value == null) return priceNote?.takeIf { it.isNotBlank() } ?: "Ask agent for price"

        return runCatching {
            NumberFormat.getCurrencyInstance(Locale.US).apply {
                currency = Currency.getInstance(currency.ifBlank { "USD" })
                maximumFractionDigits = if (value % 1.0 == 0.0) 0 else 2
            }.format(value)
        }.getOrElse {
            "$" + if (value % 1.0 == 0.0) value.toLong().toString() else "%.2f".format(Locale.US, value)
        }
    }

    fun webUrl(): String {
        val safeSlug = slug?.takeIf { it.isNotBlank() } ?: title
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
        return "https://avide.travel/services/$id-$safeSlug"
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
