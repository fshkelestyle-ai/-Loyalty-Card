package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppLanguage
import com.example.model.BrandConfig
import com.example.model.DemoDatabaseState
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "walaa_loyalty_prefs")

class AppPreferencesStore(private val context: Context) {
  private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
  }

  private object Keys {
    val BRAND_CONFIG = stringPreferencesKey("brand_config_json")
    val CUSTOMER_PHONE = stringPreferencesKey("customer_phone")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ADMIN_TOKEN = stringPreferencesKey("admin_token")
    val DEMO_DB = stringPreferencesKey("demo_db_json")
  }

  val brandConfigFlow: Flow<BrandConfig> =
    context.dataStore.data.map { prefs ->
      val raw = prefs[Keys.BRAND_CONFIG]
      if (raw.isNullOrBlank()) {
        BrandConfig()
      } else {
        runCatching { json.decodeFromString<BrandConfig>(raw) }.getOrDefault(BrandConfig())
      }
    }

  val savedCustomerPhoneFlow: Flow<String?> =
    context.dataStore.data.map { prefs -> prefs[Keys.CUSTOMER_PHONE]?.takeIf { it.isNotBlank() } }

  val languageFlow: Flow<AppLanguage> =
    context.dataStore.data.map { prefs ->
      runCatching { AppLanguage.valueOf(prefs[Keys.LANGUAGE] ?: AppLanguage.AR.name) }
        .getOrDefault(AppLanguage.AR)
    }

  val themeModeFlow: Flow<ThemeMode> =
    context.dataStore.data.map { prefs ->
      runCatching { ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name) }
        .getOrDefault(ThemeMode.SYSTEM)
    }

  val adminTokenFlow: Flow<String> =
    context.dataStore.data.map { prefs -> prefs[Keys.ADMIN_TOKEN] ?: "admin1234" }

  val demoDbFlow: Flow<DemoDatabaseState?> =
    context.dataStore.data.map { prefs ->
      val raw = prefs[Keys.DEMO_DB]
      if (raw.isNullOrBlank()) null
      else runCatching { json.decodeFromString<DemoDatabaseState>(raw) }.getOrNull()
    }

  suspend fun saveBrandConfig(config: BrandConfig) {
    context.dataStore.edit { prefs -> prefs[Keys.BRAND_CONFIG] = json.encodeToString(config) }
  }

  suspend fun saveCustomerPhone(phone: String?) {
    context.dataStore.edit { prefs ->
      if (phone.isNullOrBlank()) prefs.remove(Keys.CUSTOMER_PHONE)
      else prefs[Keys.CUSTOMER_PHONE] = phone
    }
  }

  suspend fun saveLanguage(language: AppLanguage) {
    context.dataStore.edit { prefs -> prefs[Keys.LANGUAGE] = language.name }
  }

  suspend fun saveThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
  }

  suspend fun saveAdminToken(token: String) {
    context.dataStore.edit { prefs -> prefs[Keys.ADMIN_TOKEN] = token }
  }

  suspend fun saveDemoDb(state: DemoDatabaseState) {
    context.dataStore.edit { prefs -> prefs[Keys.DEMO_DB] = json.encodeToString(state) }
  }
}
