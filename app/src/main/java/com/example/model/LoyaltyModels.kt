package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage {
  AR,
  FR,
}

@Serializable
enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
}

enum class AppRole {
  CUSTOMER,
  ADMIN,
}

@Serializable
data class BrandConfig(
  val businessName: String = "نشاطك التجاري",
  val businessType: String = "مقهى ومطعم",
  val logoUriOrUrl: String = "",
  val primaryColorHex: String = "#0F766E",
  val secondaryColorHex: String = "#D97706",
  val backgroundColorHex: String = "#FAF7F2",
  val welcomeMessage: String =
    "مرحباً بك في برنامج الولاء! اجمع النقاط مع كل زيارة واستبدلها بمكافآت حصرية.",
  val termsText: String =
    "تُحتسب النقاط عند كل عملية شراء بالدرهم المغربي (MAD). المكافآت صالحة داخل المحل وغير قابلة للتحويل النقدي.",
  val privacyText: String =
    "نلتزم بحماية بياناتك الشخصية وفقاً للقانون 09-08. يُستخدم رقم الهاتف والبريد الإلكتروني حصرياً لإدارة رصيد نقاطك وإرسال العروض بعد موافقتك الصريحة.",
  val pointsPerMad: Int = 10,
  val currency: String = "MAD",
  val appsScriptUrl: String = "",
  val customerPortalUrl: String = "https://walaa-loyalty.example.com/card",
)

@Serializable
data class Customer(
  val id: String,
  val name: String,
  val phone: String,
  val email: String = "",
  val points: Int = 0,
  val registeredAt: String,
  val lastVisit: String,
  val notificationConsent: Boolean = true,
  val notes: String = "",
)

@Serializable
enum class TransactionType {
  ADD,
  REDEEM,
}

@Serializable
data class LoyaltyTransaction(
  val id: String,
  val date: String,
  val customerId: String,
  val customerName: String,
  val customerPhone: String = "",
  val type: TransactionType,
  val points: Int,
  val billAmountMad: Double = 0.0,
  val note: String = "",
)

@Serializable
data class Reward(
  val id: String,
  val title: String,
  val description: String = "",
  val pointsRequired: Int,
  val active: Boolean = true,
)

@Serializable
enum class PromoChannel {
  EMAIL,
  WHATSAPP,
  BOTH,
}

@Serializable
enum class PromoAudience {
  ALL,
  VIP_100,
  ACTIVE_30D,
}

@Serializable
data class Promotion(
  val id: String,
  val date: String,
  val title: String,
  val message: String,
  val channel: PromoChannel = PromoChannel.BOTH,
  val audience: PromoAudience = PromoAudience.ALL,
  val recipientCount: Int = 0,
)

@Serializable
data class WhatsappDispatchLink(
  val customerId: String,
  val name: String,
  val phone: String,
  val url: String,
)

data class DashboardStats(
  val totalCustomers: Int = 0,
  val totalPointsDistributed: Int = 0,
  val activeRewardsCount: Int = 0,
  val remainingEmailQuota: Int = 100,
)

@Serializable
data class DemoDatabaseState(
  val customers: List<Customer> = emptyList(),
  val transactions: List<LoyaltyTransaction> = emptyList(),
  val rewards: List<Reward> = emptyList(),
  val promotions: List<Promotion> = emptyList(),
  val remainingEmailQuota: Int = 100,
)
