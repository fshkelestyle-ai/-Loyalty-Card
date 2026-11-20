package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminDashboardBundle
import com.example.data.AppPreferencesStore
import com.example.data.CustomerCardBundle
import com.example.data.LoyaltyRepository
import com.example.model.AppLanguage
import com.example.model.AppRole
import com.example.model.BrandConfig
import com.example.model.Customer
import com.example.model.PromoAudience
import com.example.model.PromoChannel
import com.example.model.Reward
import com.example.model.ThemeMode
import com.example.model.WhatsappDispatchLink
import com.example.nfc.NfcCardReaderHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AdminTab {
  OVERVIEW,
  CUSTOMERS,
  REWARDS,
  PROMOTIONS,
  SETTINGS_QR,
}

enum class CustomerFilter {
  ALL,
  OPTED_IN,
  VIP_100,
}

data class LoyaltyUiState(
  val activeRole: AppRole = AppRole.CUSTOMER,
  val isLoading: Boolean = false,
  val statusMessage: String? = null,
  val errorMessage: String? = null,
  val customerCard: CustomerCardBundle? = null,
  val isAdminAuthenticated: Boolean = false,
  val adminTokenInput: String = "",
  val activeAdminTab: AdminTab = AdminTab.OVERVIEW,
  val adminBundle: AdminDashboardBundle? = null,
  val customerSearchQuery: String = "",
  val customerFilter: CustomerFilter = CustomerFilter.ALL,
  val selectedCustomer: Customer? = null,
  val lastWhatsappLinks: List<WhatsappDispatchLink> = emptyList(),
  val nfcSupported: Boolean = false,
  val nfcEnabled: Boolean = false,
  val isNfcReaderActive: Boolean = false,
)

class LoyaltyViewModel(application: Application) : AndroidViewModel(application) {
  private val prefs = AppPreferencesStore(application)
  private val repo = LoyaltyRepository(prefs)

  val brandConfig: StateFlow<BrandConfig> =
    prefs.brandConfigFlow.stateIn(viewModelScope, SharingStarted.Eagerly, BrandConfig())

  val language: StateFlow<AppLanguage> =
    prefs.languageFlow.stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.AR)

  val themeMode: StateFlow<ThemeMode> =
    prefs.themeModeFlow.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

  val savedCustomerPhone: StateFlow<String?> =
    prefs.savedCustomerPhoneFlow.stateIn(viewModelScope, SharingStarted.Eagerly, null)

  private val _uiState = MutableStateFlow(LoyaltyUiState())
  val uiState: StateFlow<LoyaltyUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      val phone = prefs.savedCustomerPhoneFlow.first()
      if (!phone.isNullOrBlank()) {
        loadCustomerCard(phone)
      }
    }
  }

  fun clearMessages() {
    _uiState.update { it.copy(statusMessage = null, errorMessage = null) }
  }

  fun switchRole(role: AppRole) {
    _uiState.update { it.copy(activeRole = role, errorMessage = null, statusMessage = null) }
    if (role == AppRole.ADMIN && _uiState.value.isAdminAuthenticated) {
      refreshAdminDashboard()
    }
  }

  fun toggleLanguage() {
    viewModelScope.launch {
      val next = if (language.value == AppLanguage.AR) AppLanguage.FR else AppLanguage.AR
      prefs.saveLanguage(next)
    }
  }

  fun cycleThemeMode() {
    viewModelScope.launch {
      val next =
        when (themeMode.value) {
          ThemeMode.SYSTEM -> ThemeMode.DARK
          ThemeMode.DARK -> ThemeMode.LIGHT
          ThemeMode.LIGHT -> ThemeMode.SYSTEM
        }
      prefs.saveThemeMode(next)
    }
  }

  /* ==================== Customer Actions ==================== */

  fun registerCustomer(name: String, phone: String, email: String, consent: Boolean) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null, statusMessage = null) }
      val result = repo.registerCustomer(brandConfig.value, name, phone, email, consent)
      result.fold(
        onSuccess = { customer ->
          loadCustomerCard(customer.phone)
          _uiState.update { it.copy(statusMessage = "تم إنشاء بطاقتك بنجاح!") }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        },
      )
    }
  }

  fun loadCustomerCard(phone: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val result = repo.getCustomerCard(brandConfig.value, phone)
      result.fold(
        onSuccess = { bundle ->
          _uiState.update { it.copy(isLoading = false, customerCard = bundle) }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(isLoading = false, customerCard = null, errorMessage = err.message)
          }
        },
      )
    }
  }

  fun refreshCurrentCustomerCard() {
    val phone = _uiState.value.customerCard?.customer?.phone ?: savedCustomerPhone.value ?: return
    loadCustomerCard(phone)
  }

  fun switchCustomerAccount() {
    viewModelScope.launch {
      prefs.saveCustomerPhone(null)
      _uiState.update { it.copy(customerCard = null, statusMessage = null, errorMessage = null) }
    }
  }

  /* ==================== Admin Actions ==================== */

  fun loginAdmin(token: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      val res = repo.verifyAdmin(brandConfig.value, token)
      res.fold(
        onSuccess = {
          _uiState.update {
            it.copy(isLoading = false, isAdminAuthenticated = true, adminTokenInput = token.trim())
          }
          refreshAdminDashboard()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        },
      )
    }
  }

  fun logoutAdmin() {
    _uiState.update {
      it.copy(
        isAdminAuthenticated = false,
        adminTokenInput = "",
        adminBundle = null,
        selectedCustomer = null,
        isNfcReaderActive = false,
      )
    }
  }

  fun updateNfcHardwareState(supported: Boolean, enabled: Boolean) {
    _uiState.update {
      it.copy(
        nfcSupported = supported,
        nfcEnabled = enabled,
        isNfcReaderActive = if (supported && enabled) it.isNfcReaderActive else false,
      )
    }
  }

  fun setNfcReaderActive(active: Boolean) {
    val state = _uiState.value
    if (active && (!state.nfcSupported || !state.nfcEnabled)) {
      _uiState.update {
        it.copy(
          isNfcReaderActive = false,
          errorMessage =
            if (!state.nfcSupported) "هذا الجهاز لا يدعم قراءة NFC"
            else "المرجو تفعيل NFC من إعدادات الجهاز أولاً",
        )
      }
      return
    }
    _uiState.update {
      it.copy(
        isNfcReaderActive = active,
        statusMessage =
          if (active) "تم تفعيل قارئ NFC — قرّب بطاقة أو هاتف الزبون الآن"
          else "تم إيقاف قارئ NFC",
      )
    }
  }

  fun onNfcTagScanned(rawPayload: String) {
    val identifier = NfcCardReaderHelper.parseCustomerIdentifier(rawPayload)
    val normalizedPhone = repo.normalizePhone(identifier)
    val customers = _uiState.value.adminBundle?.customers.orEmpty()
    val matched =
      customers.find { c ->
        c.id.equals(identifier, ignoreCase = true) ||
          (normalizedPhone.length >= 8 && repo.normalizePhone(c.phone) == normalizedPhone) ||
          c.phone.equals(identifier, ignoreCase = true)
      }

    if (matched != null) {
      _uiState.update {
        it.copy(
          activeAdminTab = AdminTab.CUSTOMERS,
          selectedCustomer = matched,
          statusMessage = "تم التعرف عبر NFC على: ${matched.name} (${matched.points} نقطة)",
        )
      }
    } else {
      _uiState.update {
        it.copy(
          activeAdminTab = AdminTab.CUSTOMERS,
          customerSearchQuery = identifier,
          errorMessage = "تمت قراءة بطاقة NFC ($identifier) ولم يتم العثور على زبون مطابق",
        )
      }
    }
  }

  fun onNfcReadError(message: String) {
    _uiState.update { it.copy(errorMessage = message) }
  }

  fun selectAdminTab(tab: AdminTab) {
    _uiState.update { it.copy(activeAdminTab = tab) }
  }

  fun refreshAdminDashboard() {
    viewModelScope.launch {
      val token = _uiState.value.adminTokenInput
      if (token.isBlank()) return@launch
      _uiState.update { it.copy(isLoading = true) }
      repo.loadAdminDashboard(brandConfig.value, token).fold(
        onSuccess = { bundle ->
          val currentSelectedId = _uiState.value.selectedCustomer?.id
          val refreshedSelected =
            if (currentSelectedId != null) bundle.customers.find { it.id == currentSelectedId }
            else null
          _uiState.update {
            it.copy(
              isLoading = false,
              adminBundle = bundle,
              selectedCustomer = refreshedSelected,
            )
          }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        },
      )
    }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(customerSearchQuery = query) }
  }

  fun updateCustomerFilter(filter: CustomerFilter) {
    _uiState.update { it.copy(customerFilter = filter) }
  }

  fun selectCustomer(customer: Customer?) {
    _uiState.update { it.copy(selectedCustomer = customer) }
  }

  fun addPointsToCustomer(
    customerId: String,
    billAmountMad: Double,
    manualPoints: Int,
    note: String,
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      repo
        .addPoints(
          config = brandConfig.value,
          token = _uiState.value.adminTokenInput,
          customerId = customerId,
          billAmountMad = billAmountMad,
          manualPoints = manualPoints,
          note = note,
        )
        .fold(
          onSuccess = { msg ->
            _uiState.update { it.copy(statusMessage = msg) }
            refreshAdminDashboard()
          },
          onFailure = { err ->
            _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
          },
        )
    }
  }

  fun redeemRewardForCustomer(customerId: String, reward: Reward) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      repo
        .redeemReward(
          config = brandConfig.value,
          token = _uiState.value.adminTokenInput,
          customerId = customerId,
          reward = reward,
        )
        .fold(
          onSuccess = { msg ->
            _uiState.update { it.copy(statusMessage = msg) }
            refreshAdminDashboard()
          },
          onFailure = { err ->
            _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
          },
        )
    }
  }

  fun saveReward(reward: Reward) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      repo.saveReward(brandConfig.value, _uiState.value.adminTokenInput, reward).fold(
        onSuccess = {
          _uiState.update { it.copy(statusMessage = "تم حفظ المكافأة بنجاح") }
          refreshAdminDashboard()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        },
      )
    }
  }

  fun deleteReward(rewardId: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      repo.deleteReward(brandConfig.value, _uiState.value.adminTokenInput, rewardId).fold(
        onSuccess = {
          _uiState.update { it.copy(statusMessage = "تم حذف المكافأة") }
          refreshAdminDashboard()
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        },
      )
    }
  }

  fun sendPromotion(
    title: String,
    message: String,
    channel: PromoChannel,
    audience: PromoAudience,
  ) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      repo
        .sendPromotion(
          config = brandConfig.value,
          token = _uiState.value.adminTokenInput,
          title = title,
          message = message,
          channel = channel,
          audience = audience,
        )
        .fold(
          onSuccess = { res ->
            _uiState.update {
              it.copy(
                isLoading = false,
                statusMessage = res.message,
                lastWhatsappLinks = res.whatsappLinks,
              )
            }
            refreshAdminDashboard()
          },
          onFailure = { err ->
            _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
          },
        )
    }
  }

  fun saveBrandSettings(newConfig: BrandConfig, newAdminToken: String?) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      if (!newAdminToken.isNullOrBlank() && newAdminToken.trim().length >= 4) {
        prefs.saveAdminToken(newAdminToken.trim())
        _uiState.update { it.copy(adminTokenInput = newAdminToken.trim()) }
      }
      repo.saveBrandSettings(newConfig, _uiState.value.adminTokenInput).fold(
        onSuccess = {
          _uiState.update { it.copy(isLoading = false, statusMessage = "تم حفظ إعدادات الهوية بنجاح") }
        },
        onFailure = { err ->
          _uiState.update {
            it.copy(
              isLoading = false,
              statusMessage = "تم حفظ الإعدادات محلياً (تعذر مزامنة السحابة: ${err.message})",
            )
          }
        },
      )
    }
  }

  fun testAppsScript(url: String) {
    viewModelScope.launch {
      _uiState.update { it.copy(isLoading = true, errorMessage = null) }
      repo.testAppsScriptConnection(url).fold(
        onSuccess = { msg ->
          _uiState.update { it.copy(isLoading = false, statusMessage = msg) }
        },
        onFailure = { err ->
          _uiState.update { it.copy(isLoading = false, errorMessage = err.message) }
        },
      )
    }
  }

  fun generateConfigJs(): String = repo.generateConfigJs(brandConfig.value)
}
