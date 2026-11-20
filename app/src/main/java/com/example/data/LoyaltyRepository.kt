package com.example.data

import com.example.model.BrandConfig
import com.example.model.Customer
import com.example.model.DashboardStats
import com.example.model.DemoDatabaseState
import com.example.model.LoyaltyTransaction
import com.example.model.PromoAudience
import com.example.model.PromoChannel
import com.example.model.Promotion
import com.example.model.Reward
import com.example.model.TransactionType
import com.example.model.WhatsappDispatchLink
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

data class CustomerCardBundle(
  val customer: Customer,
  val transactions: List<LoyaltyTransaction>,
  val rewards: List<Reward>,
  val promotions: List<Promotion>,
)

data class AdminDashboardBundle(
  val stats: DashboardStats,
  val customers: List<Customer>,
  val topCustomers: List<Customer>,
  val recentTransactions: List<LoyaltyTransaction>,
  val rewards: List<Reward>,
  val promotions: List<Promotion>,
)

data class PromotionDispatchResult(
  val message: String,
  val emailsSent: Int,
  val remainingQuota: Int,
  val whatsappLinks: List<WhatsappDispatchLink>,
)

class LoyaltyRepository(
  private val prefs: AppPreferencesStore,
  private val api: AppsScriptClient = AppsScriptClient(),
) {
  private fun nowIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    return sdf.format(Date())
  }

  fun normalizePhone(phone: String): String {
    var digits = phone.replace(Regex("[^\\d+]"), "")
    if (digits.startsWith("+212")) digits = "0" + digits.drop(4)
    if (digits.startsWith("212") && digits.length >= 12) digits = "0" + digits.drop(3)
    return digits
  }

  fun toWhatsappNumber(phone: String): String {
    val normalized = normalizePhone(phone)
    return if (normalized.startsWith("0")) "212" + normalized.drop(1)
    else normalized.removePrefix("+")
  }

  private fun buildSeedDemoDb(): DemoDatabaseState {
    val customers =
      listOf(
        Customer(
          id = "CUS-101",
          name = "سلمى الإدريسي",
          phone = "0661234567",
          email = "salma@example.com",
          points = 165,
          registeredAt = "2026-09-12T10:30:00",
          lastVisit = "2026-11-13T18:20:00",
          notificationConsent = true,
        ),
        Customer(
          id = "CUS-102",
          name = "يوسف بنعلي",
          phone = "0662345678",
          email = "youssef@example.com",
          points = 85,
          registeredAt = "2026-09-24T14:15:00",
          lastVisit = "2026-11-12T16:45:00",
          notificationConsent = true,
        ),
        Customer(
          id = "CUS-103",
          name = "خديجة العمراني",
          phone = "0663456789",
          email = "khadija@example.com",
          points = 230,
          registeredAt = "2026-08-05T09:00:00",
          lastVisit = "2026-11-14T11:10:00",
          notificationConsent = true,
        ),
        Customer(
          id = "CUS-104",
          name = "أمين التازي",
          phone = "0664567890",
          email = "",
          points = 40,
          registeredAt = "2026-10-19T20:05:00",
          lastVisit = "2026-11-10T21:00:00",
          notificationConsent = false,
        ),
        Customer(
          id = "CUS-105",
          name = "مريم الشاوي",
          phone = "0665678901",
          email = "meriem@example.com",
          points = 115,
          registeredAt = "2026-10-02T12:40:00",
          lastVisit = "2026-11-14T09:30:00",
          notificationConsent = true,
        ),
      )

    val rewards =
      listOf(
        Reward("RWD-1", "قهوة أو عصير طازج", "مشروب ساخن أو بارد من اختيارك", 50, true),
        Reward("RWD-2", "خصم 25 درهم", "خصم فوري على فاتورتك القادمة", 100, true),
        Reward("RWD-3", "فطور أو حلوى مجانية", "هدية خاصة لزبنائنا الأوفياء", 180, true),
        Reward("RWD-4", "باقة VIP للشخصين", "وجبة متكاملة لشخصين", 350, true),
      )

    val transactions =
      listOf(
        LoyaltyTransaction(
          id = "TX-201",
          date = "2026-11-14T11:10:00",
          customerId = "CUS-103",
          customerName = "خديجة العمراني",
          customerPhone = "0663456789",
          type = TransactionType.ADD,
          points = 24,
          billAmountMad = 240.0,
          note = "فاتورة 240 MAD",
        ),
        LoyaltyTransaction(
          id = "TX-202",
          date = "2026-11-14T09:30:00",
          customerId = "CUS-105",
          customerName = "مريم الشاوي",
          customerPhone = "0665678901",
          type = TransactionType.ADD,
          points = 12,
          billAmountMad = 120.0,
          note = "فاتورة 120 MAD",
        ),
        LoyaltyTransaction(
          id = "TX-203",
          date = "2026-11-13T18:20:00",
          customerId = "CUS-101",
          customerName = "سلمى الإدريسي",
          customerPhone = "0661234567",
          type = TransactionType.REDEEM,
          points = 50,
          billAmountMad = 0.0,
          note = "استبدال: قهوة أو عصير طازج",
        ),
        LoyaltyTransaction(
          id = "TX-204",
          date = "2026-11-12T16:45:00",
          customerId = "CUS-102",
          customerName = "يوسف بنعلي",
          customerPhone = "0662345678",
          type = TransactionType.ADD,
          points = 18,
          billAmountMad = 185.0,
          note = "فاتورة 185 MAD",
        ),
      )

    val promotions =
      listOf(
        Promotion(
          id = "PRM-1",
          date = "2026-11-10T10:00:00",
          title = "نقاط مضاعفة نهاية الأسبوع",
          message = "استفد من نقطتين مقابل كل 10 درهم طيلة أيام الجمعة والسبت!",
          channel = PromoChannel.BOTH,
          audience = PromoAudience.ALL,
          recipientCount = 4,
        ),
        Promotion(
          id = "PRM-2",
          date = "2026-11-01T15:00:00",
          title = "عرض أعضاء VIP",
          message = "خصم إضافي 15% لحاملي أكثر من 100 نقطة هذا الأسبوع.",
          channel = PromoChannel.WHATSAPP,
          audience = PromoAudience.VIP_100,
          recipientCount = 3,
        ),
      )

    return DemoDatabaseState(
      customers = customers,
      transactions = transactions,
      rewards = rewards,
      promotions = promotions,
      remainingEmailQuota = 96,
    )
  }

  private suspend fun getOrInitDemoDb(): DemoDatabaseState {
    val existing = prefs.demoDbFlow.first()
    if (existing != null) return existing
    val seed = buildSeedDemoDb()
    prefs.saveDemoDb(seed)
    return seed
  }

  suspend fun testAppsScriptConnection(url: String): Result<String> {
    return api.postAction(url, "ping").map { it.optString("message", "تم الاتصال بنجاح") }
  }

  suspend fun registerCustomer(
    config: BrandConfig,
    name: String,
    phoneRaw: String,
    email: String,
    consent: Boolean,
  ): Result<Customer> {
    val phone = normalizePhone(phoneRaw)
    if (name.trim().length < 2) return Result.failure(IllegalArgumentException("المرجو إدخال الاسم الكامل"))
    if (phone.length < 8) return Result.failure(IllegalArgumentException("رقم الهاتف غير صالح"))
    if (!consent) {
      return Result.failure(IllegalArgumentException("يجب الموافقة الصريحة على استقبال العروض وشروط الخصوصية"))
    }

    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(
          config.appsScriptUrl,
          "registerCustomer",
          mapOf(
            "name" to name.trim(),
            "phone" to phone,
            "email" to email.trim(),
            "notificationConsent" to true,
          ),
        )
        .map { json ->
          val c = parseCustomer(json.getJSONObject("customer"))
          prefs.saveCustomerPhone(c.phone)
          c
        }
    }

    val db = getOrInitDemoDb()
    if (db.customers.any { normalizePhone(it.phone) == phone }) {
      return Result.failure(
        IllegalStateException("رقم الهاتف مسجّل مسبقاً. يمكنك فتح بطاقتك مباشرة من قسم الدخول.")
      )
    }
    val now = nowIso()
    val created =
      Customer(
        id = "CUS-${(106..999).random()}",
        name = name.trim(),
        phone = phone,
        email = email.trim(),
        points = 0,
        registeredAt = now,
        lastVisit = now,
        notificationConsent = true,
      )
    prefs.saveDemoDb(db.copy(customers = listOf(created) + db.customers))
    prefs.saveCustomerPhone(created.phone)
    return Result.success(created)
  }

  suspend fun getCustomerCard(config: BrandConfig, phoneRaw: String): Result<CustomerCardBundle> {
    val phone = normalizePhone(phoneRaw)
    if (phone.length < 8) return Result.failure(IllegalArgumentException("المرجو إدخال رقم هاتف صحيح"))

    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(config.appsScriptUrl, "getCustomerCard", mapOf("phone" to phone))
        .map { json ->
          val customer = parseCustomer(json.getJSONObject("customer"))
          val txs = parseTransactions(json.optJSONArray("transactions"))
          val rewards = parseRewards(json.optJSONArray("rewards"))
          val promos = parsePromotions(json.optJSONArray("promotions"))
          prefs.saveCustomerPhone(customer.phone)
          CustomerCardBundle(customer, txs, rewards, promos)
        }
    }

    val db = getOrInitDemoDb()
    val customer =
      db.customers.find { normalizePhone(it.phone) == phone }
        ?: return Result.failure(IllegalStateException("لم يتم العثور على بطاقة مسجّلة بهذا الرقم"))
    prefs.saveCustomerPhone(customer.phone)
    val txs = db.transactions.filter { it.customerId == customer.id }
    return Result.success(
      CustomerCardBundle(
        customer = customer,
        transactions = txs,
        rewards = db.rewards.filter { it.active },
        promotions = db.promotions.take(5),
      )
    )
  }

  suspend fun verifyAdmin(config: BrandConfig, token: String): Result<Boolean> {
    val clean = token.trim()
    if (clean.isEmpty()) return Result.failure(IllegalArgumentException("المرجو إدخال كلمة سر المدير"))

    if (config.appsScriptUrl.isNotBlank()) {
      return api.postAction(config.appsScriptUrl, "verifyAdmin", mapOf("token" to clean)).map { true }
    }
    val expected = prefs.adminTokenFlow.first()
    return if (clean == expected) Result.success(true)
    else Result.failure(IllegalStateException("كلمة سر المدير غير صحيحة (التجريبية: $expected)"))
  }

  suspend fun loadAdminDashboard(config: BrandConfig, token: String): Result<AdminDashboardBundle> {
    if (config.appsScriptUrl.isNotBlank()) {
      val dashRes = api.postAction(config.appsScriptUrl, "getDashboard", mapOf("token" to token))
      val listRes = api.postAction(config.appsScriptUrl, "listCustomers", mapOf("token" to token))
      if (dashRes.isFailure) return Result.failure(dashRes.exceptionOrNull()!!)
      val dash = dashRes.getOrThrow()
      val statsJson = dash.optJSONObject("stats") ?: JSONObject()
      val customers =
        if (listRes.isSuccess) parseCustomers(listRes.getOrThrow().optJSONArray("customers"))
        else parseCustomers(dash.optJSONArray("topCustomers"))
      return Result.success(
        AdminDashboardBundle(
          stats =
            DashboardStats(
              totalCustomers = statsJson.optInt("totalCustomers", customers.size),
              totalPointsDistributed = statsJson.optInt("totalPointsDistributed", 0),
              activeRewardsCount = statsJson.optInt("activeRewardsCount", 0),
              remainingEmailQuota = statsJson.optInt("remainingEmailQuota", 100),
            ),
          customers = customers,
          topCustomers = parseCustomers(dash.optJSONArray("topCustomers")),
          recentTransactions = parseTransactions(dash.optJSONArray("recentTransactions")),
          rewards = parseRewards(dash.optJSONArray("rewards")),
          promotions = parsePromotions(dash.optJSONArray("promotions")),
        )
      )
    }

    val db = getOrInitDemoDb()
    val totalDistributed = db.transactions.filter { it.type == TransactionType.ADD }.sumOf { it.points }
    val top = db.customers.sortedByDescending { it.points }.take(5)
    return Result.success(
      AdminDashboardBundle(
        stats =
          DashboardStats(
            totalCustomers = db.customers.size,
            totalPointsDistributed = totalDistributed,
            activeRewardsCount = db.rewards.count { it.active },
            remainingEmailQuota = db.remainingEmailQuota,
          ),
        customers = db.customers.sortedByDescending { it.points },
        topCustomers = top,
        recentTransactions = db.transactions.take(20),
        rewards = db.rewards,
        promotions = db.promotions,
      )
    )
  }

  suspend fun addPoints(
    config: BrandConfig,
    token: String,
    customerId: String,
    billAmountMad: Double,
    manualPoints: Int,
    note: String,
  ): Result<String> {
    val rate = config.pointsPerMad.coerceAtLeast(1)
    val pts = if (billAmountMad > 0 && manualPoints <= 0) (billAmountMad / rate).toInt() else manualPoints
    if (pts <= 0) {
      return Result.failure(IllegalArgumentException("عدد النقاط المحتسب يجب أن يكون 1 على الأقل"))
    }

    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(
          config.appsScriptUrl,
          "addPoints",
          mapOf(
            "token" to token,
            "customerId" to customerId,
            "billAmountMad" to billAmountMad,
            "points" to pts,
            "note" to note,
          ),
        )
        .map { it.optString("message", "تمت إضافة النقاط") }
    }

    val db = getOrInitDemoDb()
    val target =
      db.customers.find { it.id == customerId }
        ?: return Result.failure(IllegalStateException("الزبون غير موجود"))
    val now = nowIso()
    val updatedCustomers =
      db.customers.map {
        if (it.id == customerId) it.copy(points = it.points + pts, lastVisit = now) else it
      }
    val txNote =
      note.ifBlank {
        if (billAmountMad > 0) "فاتورة ${billAmountMad.toInt()} ${config.currency}" else "إضافة يدوية"
      }
    val tx =
      LoyaltyTransaction(
        id = "TX-${UUID.randomUUID().toString().take(5).uppercase()}",
        date = now,
        customerId = target.id,
        customerName = target.name,
        customerPhone = target.phone,
        type = TransactionType.ADD,
        points = pts,
        billAmountMad = billAmountMad,
        note = txNote,
      )
    prefs.saveDemoDb(db.copy(customers = updatedCustomers, transactions = listOf(tx) + db.transactions))
    return Result.success("تمت إضافة $pts نقطة لـ ${target.name}")
  }

  suspend fun redeemReward(
    config: BrandConfig,
    token: String,
    customerId: String,
    reward: Reward,
  ): Result<String> {
    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(
          config.appsScriptUrl,
          "redeemPoints",
          mapOf(
            "token" to token,
            "customerId" to customerId,
            "points" to reward.pointsRequired,
            "rewardTitle" to reward.title,
          ),
        )
        .map { it.optString("message", "تم استبدال المكافأة") }
    }

    val db = getOrInitDemoDb()
    val target =
      db.customers.find { it.id == customerId }
        ?: return Result.failure(IllegalStateException("الزبون غير موجود"))
    if (target.points < reward.pointsRequired) {
      return Result.failure(
        IllegalStateException("رصيد الزبون (${target.points}) غير كافٍ لهذه المكافأة (${reward.pointsRequired})")
      )
    }
    val now = nowIso()
    val updatedCustomers =
      db.customers.map {
        if (it.id == customerId) it.copy(points = it.points - reward.pointsRequired, lastVisit = now)
        else it
      }
    val tx =
      LoyaltyTransaction(
        id = "TX-${UUID.randomUUID().toString().take(5).uppercase()}",
        date = now,
        customerId = target.id,
        customerName = target.name,
        customerPhone = target.phone,
        type = TransactionType.REDEEM,
        points = reward.pointsRequired,
        billAmountMad = 0.0,
        note = "استبدال: ${reward.title}",
      )
    prefs.saveDemoDb(db.copy(customers = updatedCustomers, transactions = listOf(tx) + db.transactions))
    return Result.success("تم استبدال '${reward.title}' بنجاح")
  }

  suspend fun saveReward(config: BrandConfig, token: String, reward: Reward): Result<Unit> {
    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(
          config.appsScriptUrl,
          "saveReward",
          mapOf(
            "token" to token,
            "id" to reward.id.takeIf { it.isNotBlank() },
            "title" to reward.title,
            "description" to reward.description,
            "pointsRequired" to reward.pointsRequired,
            "active" to reward.active,
          ),
        )
        .map {}
    }

    val db = getOrInitDemoDb()
    val updated =
      if (reward.id.isBlank()) {
        db.rewards + reward.copy(id = "RWD-${(10..999).random()}")
      } else {
        db.rewards.map { if (it.id == reward.id) reward else it }
      }
    prefs.saveDemoDb(db.copy(rewards = updated))
    return Result.success(Unit)
  }

  suspend fun deleteReward(config: BrandConfig, token: String, rewardId: String): Result<Unit> {
    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(config.appsScriptUrl, "deleteReward", mapOf("token" to token, "id" to rewardId))
        .map {}
    }
    val db = getOrInitDemoDb()
    prefs.saveDemoDb(db.copy(rewards = db.rewards.filterNot { it.id == rewardId }))
    return Result.success(Unit)
  }

  suspend fun saveBrandSettings(config: BrandConfig, token: String): Result<Unit> {
    prefs.saveBrandConfig(config)
    if (config.appsScriptUrl.isNotBlank()) {
      return api
        .postAction(
          config.appsScriptUrl,
          "saveSettings",
          mapOf(
            "token" to token,
            "settings" to
              mapOf(
                "businessName" to config.businessName,
                "businessType" to config.businessType,
                "logoUrl" to config.logoUriOrUrl,
                "primaryColor" to config.primaryColorHex,
                "secondaryColor" to config.secondaryColorHex,
                "backgroundColor" to config.backgroundColorHex,
                "welcomeMessage" to config.welcomeMessage,
                "termsText" to config.termsText,
                "privacyText" to config.privacyText,
                "pointsPerMad" to config.pointsPerMad,
                "currency" to config.currency,
                "customerPortalUrl" to config.customerPortalUrl,
              ),
          ),
        )
        .map {}
    }
    return Result.success(Unit)
  }

  suspend fun sendPromotion(
    config: BrandConfig,
    token: String,
    title: String,
    message: String,
    channel: PromoChannel,
    audience: PromoAudience,
  ): Result<PromotionDispatchResult> {
    if (title.isBlank() || message.isBlank()) {
      return Result.failure(IllegalArgumentException("المرجو إدخال عنوان العرض ونص الرسالة"))
    }

    if (config.appsScriptUrl.isNotBlank()) {
      val ch =
        when (channel) {
          PromoChannel.EMAIL -> "email"
          PromoChannel.WHATSAPP -> "whatsapp"
          PromoChannel.BOTH -> "both"
        }
      val aud =
        when (audience) {
          PromoAudience.ALL -> "all"
          PromoAudience.VIP_100 -> "vip100"
          PromoAudience.ACTIVE_30D -> "active30"
        }
      return api
        .postAction(
          config.appsScriptUrl,
          "sendPromotion",
          mapOf(
            "token" to token,
            "title" to title.trim(),
            "message" to message.trim(),
            "channel" to ch,
            "audience" to aud,
          ),
        )
        .map { json ->
          val waArray = json.optJSONArray("whatsappLinks")
          val links = buildList {
            if (waArray != null) {
              for (i in 0 until waArray.length()) {
                val obj = waArray.getJSONObject(i)
                add(
                  WhatsappDispatchLink(
                    customerId = obj.optString("customerId"),
                    name = obj.optString("name"),
                    phone = obj.optString("phone"),
                    url = obj.optString("url"),
                  )
                )
              }
            }
          }
          PromotionDispatchResult(
            message = json.optString("message", "تم إطلاق الحملة"),
            emailsSent = json.optInt("emailsSent", 0),
            remainingQuota = json.optInt("remainingEmailQuota", 100),
            whatsappLinks = links,
          )
        }
    }

    val db = getOrInitDemoDb()
    val consented = db.customers.filter { it.notificationConsent }
    val targets =
      when (audience) {
        PromoAudience.ALL -> consented
        PromoAudience.VIP_100 -> consented.filter { it.points >= 100 }
        PromoAudience.ACTIVE_30D -> consented
      }
    var quota = db.remainingEmailQuota
    var emailsSent = 0
    val links = mutableListOf<WhatsappDispatchLink>()

    targets.forEach { c ->
      if ((channel == PromoChannel.EMAIL || channel == PromoChannel.BOTH) && c.email.isNotBlank() && quota > 0) {
        emailsSent++
        quota--
      }
      if (channel == PromoChannel.WHATSAPP || channel == PromoChannel.BOTH) {
        val text =
          "مرحباً ${c.name} 👋\n*${config.businessName}*\n$title\n$message\nرصيدك الحالي: ${c.points} نقطة"
        val encoded = URLEncoder.encode(text, "UTF-8")
        links.add(
          WhatsappDispatchLink(
            customerId = c.id,
            name = c.name,
            phone = c.phone,
            url = "https://wa.me/${toWhatsappNumber(c.phone)}?text=$encoded",
          )
        )
      }
    }

    val promo =
      Promotion(
        id = "PRM-${UUID.randomUUID().toString().take(5).uppercase()}",
        date = nowIso(),
        title = title.trim(),
        message = message.trim(),
        channel = channel,
        audience = audience,
        recipientCount = targets.size,
      )
    prefs.saveDemoDb(
      db.copy(promotions = listOf(promo) + db.promotions, remainingEmailQuota = quota)
    )
    return Result.success(
      PromotionDispatchResult(
        message = "تم تجهيز الحملة لـ ${targets.size} زبون (محاكاة إرسال $emailsSent بريد إلكتروني)",
        emailsSent = emailsSent,
        remainingQuota = quota,
        whatsappLinks = links,
      )
    )
  }

  fun generateConfigJs(config: BrandConfig): String {
    return """
window.LOYALTY_CONFIG = {
  businessName: "${config.businessName.replace("\"", "\\\"")}",
  businessType: "${config.businessType.replace("\"", "\\\"")}",
  logoUrl: "${config.logoUriOrUrl.replace("\"", "\\\"")}",
  colors: {
    primary: "${config.primaryColorHex}",
    secondary: "${config.secondaryColorHex}",
    background: "${config.backgroundColorHex}",
  },
  welcomeMessage: "${config.welcomeMessage.replace("\"", "\\\"")}",
  termsText: "${config.termsText.replace("\"", "\\\"")}",
  privacyText: "${config.privacyText.replace("\"", "\\\"")}",
  currency: "${config.currency}",
  pointsPerMad: ${config.pointsPerMad},
  defaultLang: "ar",
  appsScriptUrl: "${config.appsScriptUrl.replace("\"", "\\\"")}",
  customerPortalUrl: "${config.customerPortalUrl.replace("\"", "\\\"")}"
};
""".trimIndent()
  }

  private fun parseCustomer(obj: JSONObject): Customer {
    return Customer(
      id = obj.optString("id"),
      name = obj.optString("name"),
      phone = obj.optString("phone"),
      email = obj.optString("email"),
      points = obj.optInt("points", 0),
      registeredAt = obj.optString("registeredAt"),
      lastVisit = obj.optString("lastVisit"),
      notificationConsent = obj.optBoolean("notificationConsent", true),
    )
  }

  private fun parseCustomers(arr: JSONArray?): List<Customer> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) add(parseCustomer(arr.getJSONObject(i)))
    }
  }

  private fun parseTransactions(arr: JSONArray?): List<LoyaltyTransaction> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        val rawType = o.optString("type")
        val type =
          if (rawType.contains("استبدال") || rawType.equals("REDEEM", true)) TransactionType.REDEEM
          else TransactionType.ADD
        add(
          LoyaltyTransaction(
            id = o.optString("id"),
            date = o.optString("date"),
            customerId = o.optString("customerId"),
            customerName = o.optString("customerName"),
            customerPhone = o.optString("customerPhone"),
            type = type,
            points = o.optInt("points", 0),
            billAmountMad = o.optDouble("billAmountMad", 0.0),
            note = o.optString("note"),
          )
        )
      }
    }
  }

  private fun parseRewards(arr: JSONArray?): List<Reward> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
          Reward(
            id = o.optString("id"),
            title = o.optString("title"),
            description = o.optString("description"),
            pointsRequired = o.optInt("pointsRequired", 50),
            active = o.optBoolean("active", true),
          )
        )
      }
    }
  }

  private fun parsePromotions(arr: JSONArray?): List<Promotion> {
    if (arr == null) return emptyList()
    return buildList {
      for (i in 0 until arr.length()) {
        val o = arr.getJSONObject(i)
        add(
          Promotion(
            id = o.optString("id"),
            date = o.optString("date"),
            title = o.optString("title"),
            message = o.optString("message"),
            recipientCount = o.optInt("recipientCount", 0),
          )
        )
      }
    }
  }
}
