package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

data class DjezzyOffer(
    val id: Int,
    val label: String,
    val code: String,
    val type: String, // "shake" or "activate-product"
    val name: String,
    val amount: String,
    val priceDzd: Int,
    val duration: String,
    val category: String // "يومي", "أسبوعي", "شهري"
)

object PaidOffers {
    val list = listOf(
        DjezzyOffer(1, "🔖 عرض 70دج [4 جيقا] 24h", "BTLINTSPEEDDAY2Go", "shake", "عرض 70دج 4Go", "4GB", 70, "24 ساعة", "يومي"),
        DjezzyOffer(2, "🎁 عرض 100دج [2 جيقا] 24h", "DOVINTSPEEDDAY1GoPRE", "activate-product", "عرض 100دج 2Go", "2GB", 100, "24 ساعة", "يومي"),
        DjezzyOffer(3, "📦 عرض 300Mo بـ 30دج مدة 24h", "DOVINTSPEEDDAY100MoPRE", "activate-product", "عرض 30دج 300Mo", "300MB", 30, "24 ساعة", "يومي"),
        DjezzyOffer(4, "📦 عرض 600Mo بـ 50دج مدة 24h", "DOVINTSPEEDDAY250MoPRE", "activate-product", "عرض 50دج 600Mo", "600MB", 50, "24 ساعة", "يومي"),
        DjezzyOffer(5, "📶 عرض 4Go بـ 150دج مدة 7 أيام", "DOVINTSPEEDWEEK2GoPRE", "activate-product", "عرض 150دج 4Go", "4GB", 150, "7 أيام", "أسبوعي"),
        DjezzyOffer(6, "📶 عرض 10Go بـ 300دج مدة 7 أيام", "DOVINTSPEEDWEEK3GoPRE", "activate-product", "عرض 300دج 10Go", "10GB", 300, "7 أيام", "أسبوعي"),
        DjezzyOffer(7, "⚡ عرض 10Go بـ 190دج مدة 72h", "BTL4GBDAY", "shake", "عرض 190دج 10Go", "10GB", 190, "72 ساعة", "أسبوعي"),
        DjezzyOffer(8, "📘 عرض 3Go بـ 70دج مدة 3 أيام FB", "1GBFB3DAY", "shake", "عرض 70دج 3Go FB", "3GB", 70, "3 أيام", "أسبوعي"),
        DjezzyOffer(9, "🌙 عرض 12Go بـ 500دج مدة شهر", "DOVINTSPEEDMONTH6GoPRE", "activate-product", "عرض 500دج 12Go", "12GB", 500, "30 يوم", "شهري"),
        DjezzyOffer(10, "🌙 عرض 30Go بـ 1000دج مدة شهر", "DOVINTSPEEDMONTH15GoPRE", "activate-product", "عرض 1000دج 30Go", "30GB", 1000, "30 يوم", "شهري"),
        DjezzyOffer(11, "🌙 عرض 60Go بـ 1500دج مدة شهر", "DOVINTSPEEDMONTH30GoPRE", "activate-product", "عرض 1500دج 60Go", "60GB", 1500, "30 يوم", "شهري"),
        DjezzyOffer(12, "🔥 عرض 100Go بـ 2000دج مدة 30 يوم", "DOVINTSPEEDMONTH100GoPRE5G", "activate-product", "عرض 2000دج 100Go", "100GB", 2000, "30 يوم", "شهري"),
        DjezzyOffer(13, "👑 عرض 200Go بـ 4000دج مدة 30 يوم", "DOVINTSPEEDMONTH220GoPRE5G", "activate-product", "عرض 4000دج 200Go", "200GB", 4000, "30 يوم", "شهري")
    )
}

// Network API DTOs

@JsonClass(generateAdapter = true)
data class ConsentItem(
    @Json(name = "marketing-notifications") val marketingNotifications: Boolean = false
)

@JsonClass(generateAdapter = true)
data class OtpRegistrationBody(
    @Json(name = "consent-agreement") val consentAgreement: List<ConsentItem> = listOf(ConsentItem()),
    @Json(name = "is-consent") val isConsent: Boolean = true
)

@JsonClass(generateAdapter = true)
data class PackageCodeBody(
    @Json(name = "packageCode") val packageCode: String
)

@JsonClass(generateAdapter = true)
data class MgmInvitationBody(
    @Json(name = "msisdnReciever") val msisdnReciever: Long
)

@JsonClass(generateAdapter = true)
data class MigrationExecuteBody(
    @Json(name = "migrationConfigurationId") val migrationConfigurationId: String
)

@JsonClass(generateAdapter = true)
data class ApiBaseResponse(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: Any? = null
)

@JsonClass(generateAdapter = true)
data class BalanceResponse(
    @Json(name = "data") val data: BalanceData? = null
)

@JsonClass(generateAdapter = true)
data class BalanceData(
    @Json(name = "mainBalance") val mainBalance: String? = "0",
    @Json(name = "customerInformations") val customerInformations: CustomerInfo? = null
)

@JsonClass(generateAdapter = true)
data class CustomerInfo(
    @Json(name = "subscriptionType") val subscriptionType: SubscriptionType? = null
)

@JsonClass(generateAdapter = true)
data class SubscriptionType(
    @Json(name = "name") val name: LocalizedText? = null
)

@JsonClass(generateAdapter = true)
data class LocalizedText(
    @Json(name = "ar") val ar: String? = null,
    @Json(name = "fr") val fr: String? = null,
    @Json(name = "en") val en: String? = null
)

@JsonClass(generateAdapter = true)
data class ConnectedProductsResponse(
    @Json(name = "data") val data: ConnectedProductsData? = null
)

@JsonClass(generateAdapter = true)
data class ConnectedProductsData(
    @Json(name = "products") val products: List<ConnectedProduct>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ConnectedProduct(
    @Json(name = "commercialName") val commercialName: LocalizedText? = null,
    @Json(name = "expiryAt") val expiryAt: String? = null,
    @Json(name = "balances") val balances: List<ProductBalance>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class ProductBalance(
    @Json(name = "usageUnit") val usageUnit: String? = "MB",
    @Json(name = "remaining") val remaining: Double? = 0.0
)

@JsonClass(generateAdapter = true)
data class MigrationResponse(
    @Json(name = "data") val data: List<MigrationOption>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class MigrationOption(
    @Json(name = "id") val id: String,
    @Json(name = "subscriptionTypeFrom") val subscriptionTypeFrom: SubscriptionTypeHolder? = null,
    @Json(name = "subscriptionTypeTo") val subscriptionTypeTo: SubscriptionTypeHolder? = null,
    @Json(name = "description") val description: LocalizedText? = null
)

@JsonClass(generateAdapter = true)
data class SubscriptionTypeHolder(
    @Json(name = "name") val name: LocalizedText? = null
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null
)
