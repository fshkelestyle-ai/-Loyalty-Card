package com.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppLanguage
import com.example.model.AppRole
import com.example.model.ThemeMode
import com.example.nfc.NfcCardReaderHelper
import com.example.ui.LoyaltyViewModel
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.customer.CustomerPortalScreen
import com.example.ui.i18n.stringsFor
import com.example.ui.theme.LoyaltyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { LoyaltyAppRoot() }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyAppRoot(viewModel: LoyaltyViewModel = viewModel()) {
  val brandConfig by viewModel.brandConfig.collectAsStateWithLifecycle()
  val language by viewModel.language.collectAsStateWithLifecycle()
  val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val strings = remember(language) { stringsFor(language) }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val activity = context as? Activity
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(
    lifecycleOwner,
    activity,
    uiState.activeRole,
    uiState.isAdminAuthenticated,
    uiState.isNfcReaderActive,
  ) {
    fun syncReader() {
      val hw = NfcCardReaderHelper.getHardwareStatus(context)
      viewModel.updateNfcHardwareState(hw.isSupported, hw.isEnabled)
      val shouldListen =
        activity != null &&
          hw.isSupported &&
          hw.isEnabled &&
          uiState.activeRole == AppRole.ADMIN &&
          uiState.isAdminAuthenticated &&
          uiState.isNfcReaderActive

      if (shouldListen && activity != null) {
        NfcCardReaderHelper.enableReaderMode(
          activity = activity,
          onPayloadRead = { payload -> viewModel.onNfcTagScanned(payload) },
          onError = { err -> viewModel.onNfcReadError(err) },
        )
      } else if (activity != null) {
        NfcCardReaderHelper.disableReaderMode(activity)
      }
    }

    syncReader()

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> syncReader()
        Lifecycle.Event.ON_PAUSE -> {
          if (activity != null) NfcCardReaderHelper.disableReaderMode(activity)
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      if (activity != null) NfcCardReaderHelper.disableReaderMode(activity)
    }
  }

  LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
    val msg = uiState.errorMessage ?: uiState.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearMessages()
    }
  }

  if (uiState.activeRole == AppRole.ADMIN) {
    BackHandler { viewModel.switchRole(AppRole.CUSTOMER) }
  }

  val layoutDir = if (language == AppLanguage.AR) LayoutDirection.Rtl else LayoutDirection.Ltr

  CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
    LoyaltyTheme(brandConfig = brandConfig, themeMode = themeMode) {
      BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpanded = maxWidth >= 640.dp

        Scaffold(
          contentWindowInsets = WindowInsets.safeDrawing,
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            Column {
              TopAppBar(
                colors =
                  TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                  ),
                title = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLogoAvatar(
                      logoUriOrUrl = brandConfig.logoUriOrUrl,
                      businessName = brandConfig.businessName,
                      size = 38.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                      Text(
                        text = brandConfig.businessName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                      )
                      Surface(
                        color =
                          if (brandConfig.appsScriptUrl.isBlank())
                            MaterialTheme.colorScheme.secondaryContainer
                          else MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(999.dp),
                      ) {
                        Text(
                          text =
                            if (brandConfig.appsScriptUrl.isBlank()) strings.demoModeBadge
                            else strings.liveSheetsBadge,
                          style = MaterialTheme.typography.labelSmall,
                          color =
                            if (brandConfig.appsScriptUrl.isBlank())
                              MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                          maxLines = 1,
                        )
                      }
                    }
                  }
                },
                actions = {
                  IconButton(
                    onClick = { viewModel.toggleLanguage() },
                    modifier = Modifier.size(48.dp).testTag("language_toggle_button"),
                  ) {
                    Text(
                      text = if (language == AppLanguage.AR) "FR" else "AR",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                  IconButton(
                    onClick = { viewModel.cycleThemeMode() },
                    modifier = Modifier.size(48.dp).testTag("theme_toggle_button"),
                  ) {
                    Icon(
                      imageVector =
                        if (themeMode == ThemeMode.DARK) Icons.Filled.LightMode
                        else Icons.Filled.DarkMode,
                      contentDescription = "Theme",
                    )
                  }
                },
              )
              if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
              }
            }
          },
          bottomBar = {
            if (!isExpanded) {
              NavigationBar {
                NavigationBarItem(
                  selected = uiState.activeRole == AppRole.CUSTOMER,
                  onClick = { viewModel.switchRole(AppRole.CUSTOMER) },
                  icon = {
                    Icon(
                      if (uiState.activeRole == AppRole.CUSTOMER) Icons.Filled.CreditCard
                      else Icons.Outlined.CreditCard,
                      contentDescription = strings.customerPortal,
                    )
                  },
                  label = { Text(strings.customerPortal) },
                  modifier = Modifier.testTag("nav_customer"),
                )
                NavigationBarItem(
                  selected = uiState.activeRole == AppRole.ADMIN,
                  onClick = { viewModel.switchRole(AppRole.ADMIN) },
                  icon = {
                    Icon(
                      if (uiState.activeRole == AppRole.ADMIN) Icons.Filled.AdminPanelSettings
                      else Icons.Outlined.AdminPanelSettings,
                      contentDescription = strings.adminPanel,
                    )
                  },
                  label = { Text(strings.adminPanel) },
                  modifier = Modifier.testTag("nav_admin"),
                )
              }
            }
          },
        ) { innerPadding ->
          Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (isExpanded) {
              NavigationRail(modifier = Modifier.fillMaxHeight()) {
                NavigationRailItem(
                  selected = uiState.activeRole == AppRole.CUSTOMER,
                  onClick = { viewModel.switchRole(AppRole.CUSTOMER) },
                  icon = {
                    Icon(
                      if (uiState.activeRole == AppRole.CUSTOMER) Icons.Filled.CreditCard
                      else Icons.Outlined.CreditCard,
                      contentDescription = strings.customerPortal,
                    )
                  },
                  label = { Text(strings.customerPortal) },
                )
                NavigationRailItem(
                  selected = uiState.activeRole == AppRole.ADMIN,
                  onClick = { viewModel.switchRole(AppRole.ADMIN) },
                  icon = {
                    Icon(
                      if (uiState.activeRole == AppRole.ADMIN) Icons.Filled.AdminPanelSettings
                      else Icons.Outlined.AdminPanelSettings,
                      contentDescription = strings.adminPanel,
                    )
                  },
                  label = { Text(strings.adminPanel) },
                )
              }
            }

            Box(modifier = Modifier.weight(1f).fillMaxSize()) {
              when (uiState.activeRole) {
                AppRole.CUSTOMER ->
                  CustomerPortalScreen(
                    config = brandConfig,
                    uiState = uiState,
                    strings = strings,
                    onRegister = { name, phone, email, consent ->
                      viewModel.registerCustomer(name, phone, email, consent)
                    },
                    onLookupPhone = { phone -> viewModel.loadCustomerCard(phone) },
                    onRefreshCard = { viewModel.refreshCurrentCustomerCard() },
                    onSwitchCustomer = { viewModel.switchCustomerAccount() },
                    onShowFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                  )

                AppRole.ADMIN ->
                  AdminDashboardScreen(
                    config = brandConfig,
                    uiState = uiState,
                    strings = strings,
                    onLogin = { viewModel.loginAdmin(it) },
                    onLogout = { viewModel.logoutAdmin() },
                    onSelectTab = { viewModel.selectAdminTab(it) },
                    onSearchChange = { viewModel.updateSearchQuery(it) },
                    onFilterChange = { viewModel.updateCustomerFilter(it) },
                    onSelectCustomer = { viewModel.selectCustomer(it) },
                    onAddPoints = { id, bill, pts, note ->
                      viewModel.addPointsToCustomer(id, bill, pts, note)
                    },
                    onRedeemReward = { id, reward -> viewModel.redeemRewardForCustomer(id, reward) },
                    onSaveReward = { viewModel.saveReward(it) },
                    onDeleteReward = { viewModel.deleteReward(it) },
                    onUpdateMadRate = { rate ->
                      viewModel.saveBrandSettings(brandConfig.copy(pointsPerMad = rate), null)
                    },
                    onSendPromotion = { title, msg, ch, aud ->
                      viewModel.sendPromotion(title, msg, ch, aud)
                    },
                    onSaveSettings = { cfg, pass -> viewModel.saveBrandSettings(cfg, pass) },
                    onTestAppsScript = { url -> viewModel.testAppsScript(url) },
                    onSimulateCustomerScan = { viewModel.switchRole(AppRole.CUSTOMER) },
                    onToggleNfcReader = { enable -> viewModel.setNfcReaderActive(enable) },
                    onOpenNfcSettings = { NfcCardReaderHelper.openNfcSettings(context) },
                    generateConfigJs = { viewModel.generateConfigJs() },
                    onShowFeedback = { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } },
                  )
              }
            }
          }
        }
      }
    }
  }
}
