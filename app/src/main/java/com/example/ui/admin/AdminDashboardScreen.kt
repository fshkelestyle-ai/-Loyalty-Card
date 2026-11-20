package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.AdminDashboardBundle
import com.example.model.BrandConfig
import com.example.model.Customer
import com.example.model.LoyaltyTransaction
import com.example.model.PromoAudience
import com.example.model.PromoChannel
import com.example.model.Reward
import com.example.model.TransactionType
import com.example.ui.AdminTab
import com.example.ui.CustomerFilter
import com.example.ui.LoyaltyUiState
import com.example.ui.components.EmptyHintCard
import com.example.ui.components.KpiStatCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.i18n.AppStrings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  config: BrandConfig,
  uiState: LoyaltyUiState,
  strings: AppStrings,
  onLogin: (String) -> Unit,
  onLogout: () -> Unit,
  onSelectTab: (AdminTab) -> Unit,
  onSearchChange: (String) -> Unit,
  onFilterChange: (CustomerFilter) -> Unit,
  onSelectCustomer: (Customer?) -> Unit,
  onAddPoints: (customerId: String, billAmountMad: Double, manualPoints: Int, note: String) -> Unit,
  onRedeemReward: (customerId: String, reward: Reward) -> Unit,
  onSaveReward: (Reward) -> Unit,
  onDeleteReward: (String) -> Unit,
  onUpdateMadRate: (Int) -> Unit,
  onSendPromotion: (String, String, PromoChannel, PromoAudience) -> Unit,
  onSaveSettings: (BrandConfig, String?) -> Unit,
  onTestAppsScript: (String) -> Unit,
  onSimulateCustomerScan: () -> Unit,
  onToggleNfcReader: (Boolean) -> Unit,
  onOpenNfcSettings: () -> Unit,
  generateConfigJs: () -> String,
  onShowFeedback: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (!uiState.isAdminAuthenticated) {
    AdminLoginGate(
      strings = strings,
      isLoading = uiState.isLoading,
      onLogin = onLogin,
      modifier = modifier,
    )
    return
  }

  val bundle = uiState.adminBundle

  Column(modifier = modifier.fillMaxSize()) {
    val tabs = AdminTab.entries
    PrimaryScrollableTabRow(
      selectedTabIndex = tabs.indexOf(uiState.activeAdminTab),
      edgePadding = 12.dp,
    ) {
      tabs.forEach { tab ->
        val label =
          when (tab) {
            AdminTab.OVERVIEW -> strings.tabOverview
            AdminTab.CUSTOMERS -> strings.tabCustomers
            AdminTab.REWARDS -> strings.tabRewards
            AdminTab.PROMOTIONS -> strings.tabPromotions
            AdminTab.SETTINGS_QR -> strings.tabSettingsQr
          }
        Tab(
          selected = uiState.activeAdminTab == tab,
          onClick = { onSelectTab(tab) },
          text = { Text(label, fontWeight = FontWeight.Bold) },
          modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}"),
        )
      }
    }

    Column(
      modifier =
        Modifier.fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Column(modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
        when (uiState.activeAdminTab) {
          AdminTab.OVERVIEW ->
            OverviewTabContent(
              config = config,
              bundle = bundle,
              uiState = uiState,
              strings = strings,
              onToggleNfcReader = onToggleNfcReader,
              onOpenNfcSettings = onOpenNfcSettings,
              onSelectCustomer = {
                onSelectCustomer(it)
                onSelectTab(AdminTab.CUSTOMERS)
              },
              onLogout = onLogout,
            )

          AdminTab.CUSTOMERS ->
            CustomersTabContent(
              config = config,
              bundle = bundle,
              uiState = uiState,
              strings = strings,
              onToggleNfcReader = onToggleNfcReader,
              onOpenNfcSettings = onOpenNfcSettings,
              onSearchChange = onSearchChange,
              onFilterChange = onFilterChange,
              onSelectCustomer = onSelectCustomer,
              onAddPoints = onAddPoints,
              onRedeemReward = onRedeemReward,
            )

          AdminTab.REWARDS ->
            AdminRewardsTabContent(
              config = config,
              rewards = bundle?.rewards.orEmpty(),
              strings = strings,
              onSaveReward = onSaveReward,
              onDeleteReward = onDeleteReward,
              onUpdateMadRate = onUpdateMadRate,
            )

          AdminTab.PROMOTIONS ->
            AdminPromotionsTabContent(
              bundle = bundle,
              lastWhatsappLinks = uiState.lastWhatsappLinks,
              strings = strings,
              isLoading = uiState.isLoading,
              onSendPromotion = onSendPromotion,
            )

          AdminTab.SETTINGS_QR ->
            AdminSettingsQrTabContent(
              config = config,
              currentAdminToken = uiState.adminTokenInput,
              strings = strings,
              isLoading = uiState.isLoading,
              onSaveSettings = onSaveSettings,
              onTestAppsScript = onTestAppsScript,
              onSimulateCustomerScan = onSimulateCustomerScan,
              generateConfigJs = generateConfigJs,
              onShowFeedback = onShowFeedback,
            )
        }
        Spacer(Modifier.height(28.dp))
      }
    }
  }
}

@Composable
private fun AdminLoginGate(
  strings: AppStrings,
  isLoading: Boolean,
  onLogin: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var password by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxSize().padding(20.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Card(
      modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
      Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.size(64.dp),
        ) {
          Icon(
            imageVector = Icons.Filled.AdminPanelSettings,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(14.dp).fillMaxSize(),
          )
        }
        Spacer(Modifier.height(14.dp))
        Text(
          text = strings.adminLoginTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
          text = strings.demoPasswordTip,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
          value = password,
          onValueChange = { password = it },
          label = { Text(strings.adminPasswordHint) },
          leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
          visualTransformation = PasswordVisualTransformation(),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().testTag("admin_password_input"),
        )
        Spacer(Modifier.height(16.dp))
        Button(
          onClick = { onLogin(password) },
          enabled = !isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_login_button"),
        ) {
          Text(strings.loginButton, style = MaterialTheme.typography.titleMedium)
        }
      }
    }
  }
}

@Composable
private fun OverviewTabContent(
  config: BrandConfig,
  bundle: AdminDashboardBundle?,
  uiState: LoyaltyUiState,
  strings: AppStrings,
  onToggleNfcReader: (Boolean) -> Unit,
  onOpenNfcSettings: () -> Unit,
  onSelectCustomer: (Customer) -> Unit,
  onLogout: () -> Unit,
) {
  NfcReaderBannerCard(
    uiState = uiState,
    strings = strings,
    onToggleNfcReader = onToggleNfcReader,
    onOpenNfcSettings = onOpenNfcSettings,
  )
  Spacer(Modifier.height(14.dp))

  val stats = bundle?.stats
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    KpiStatCard(
      label = strings.totalCustomers,
      value = "${stats?.totalCustomers ?: 0}",
      accentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
    )
    KpiStatCard(
      label = strings.distributedPoints,
      value = "${stats?.totalPointsDistributed ?: 0}",
      accentColor = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.weight(1f),
    )
  }
  Spacer(Modifier.height(10.dp))
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
    KpiStatCard(
      label = strings.activeRewards,
      value = "${stats?.activeRewardsCount ?: 0}",
      accentColor = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f),
    )
    KpiStatCard(
      label = strings.emailQuotaLeft,
      value = "${stats?.remainingEmailQuota ?: 100}",
      accentColor = MaterialTheme.colorScheme.secondary,
      modifier = Modifier.weight(1f),
    )
  }

  Spacer(Modifier.height(18.dp))
  SectionHeaderRow(title = strings.topCustomers)
  val top = bundle?.topCustomers.orEmpty()
  if (top.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        top.forEachIndexed { idx, customer ->
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable { onSelectCustomer(customer) }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(36.dp),
              ) {
                Icon(
                  Icons.Filled.Person,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.padding(7.dp),
                )
              }
              Spacer(Modifier.width(10.dp))
              Column {
                Text(customer.name, fontWeight = FontWeight.Bold)
                Text(
                  customer.phone,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = "${customer.points} ${strings.points}",
              style = MaterialTheme.typography.titleMedium,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
          }
          if (idx < top.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
      }
    }
  }

  Spacer(Modifier.height(18.dp))
  SectionHeaderRow(title = strings.recentTransactions)
  val txs = bundle?.recentTransactions.orEmpty()
  if (txs.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    TransactionListCard(transactions = txs, config = config, strings = strings)
  }

  Spacer(Modifier.height(16.dp))
  OutlinedButton(
    onClick = onLogout,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_logout_button"),
  ) {
    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text(strings.logoutAdmin)
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomersTabContent(
  config: BrandConfig,
  bundle: AdminDashboardBundle?,
  uiState: LoyaltyUiState,
  strings: AppStrings,
  onToggleNfcReader: (Boolean) -> Unit,
  onOpenNfcSettings: () -> Unit,
  onSearchChange: (String) -> Unit,
  onFilterChange: (CustomerFilter) -> Unit,
  onSelectCustomer: (Customer?) -> Unit,
  onAddPoints: (String, Double, Int, String) -> Unit,
  onRedeemReward: (String, Reward) -> Unit,
) {
  NfcReaderBannerCard(
    uiState = uiState,
    strings = strings,
    onToggleNfcReader = onToggleNfcReader,
    onOpenNfcSettings = onOpenNfcSettings,
  )
  Spacer(Modifier.height(12.dp))

  OutlinedTextField(
    value = uiState.customerSearchQuery,
    onValueChange = onSearchChange,
    label = { Text(strings.searchCustomers) },
    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().testTag("customer_search_input"),
  )

  Spacer(Modifier.height(10.dp))

  FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    FilterChip(
      selected = uiState.customerFilter == CustomerFilter.ALL,
      onClick = { onFilterChange(CustomerFilter.ALL) },
      label = { Text(strings.filterAll) },
    )
    FilterChip(
      selected = uiState.customerFilter == CustomerFilter.OPTED_IN,
      onClick = { onFilterChange(CustomerFilter.OPTED_IN) },
      label = { Text(strings.filterOptIn) },
    )
    FilterChip(
      selected = uiState.customerFilter == CustomerFilter.VIP_100,
      onClick = { onFilterChange(CustomerFilter.VIP_100) },
      label = { Text(strings.filterVip) },
    )
  }

  Spacer(Modifier.height(12.dp))

  val q = uiState.customerSearchQuery.trim().lowercase()
  val filtered =
    bundle?.customers.orEmpty().filter { c ->
      val matchesQuery =
        q.isEmpty() ||
          c.name.lowercase().contains(q) ||
          c.phone.lowercase().contains(q) ||
          c.email.lowercase().contains(q)
      val matchesFilter =
        when (uiState.customerFilter) {
          CustomerFilter.ALL -> true
          CustomerFilter.OPTED_IN -> c.notificationConsent
          CustomerFilter.VIP_100 -> c.points >= 100
        }
      matchesQuery && matchesFilter
    }

  if (filtered.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    filtered.forEach { customer ->
      Card(
        modifier =
          Modifier.fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable { onSelectCustomer(customer) }
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(customer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
              "${customer.phone} • ${customer.id}",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(999.dp),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                Icons.Filled.Stars,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text(
                text = "${customer.points} ${strings.points}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }
      }
    }
  }

  val selected = uiState.selectedCustomer
  if (selected != null) {
    val customerTxs =
      bundle?.recentTransactions.orEmpty().filter { it.customerId == selected.id }
    CustomerDetailActionDialog(
      customer = selected,
      config = config,
      rewards = bundle?.rewards.orEmpty().filter { it.active },
      transactions = customerTxs,
      strings = strings,
      onDismiss = { onSelectCustomer(null) },
      onAddPoints = { billMad, manualPts, note ->
        onAddPoints(selected.id, billMad, manualPts, note)
      },
      onRedeemReward = { reward -> onRedeemReward(selected.id, reward) },
    )
  }
}

@Composable
private fun CustomerDetailActionDialog(
  customer: Customer,
  config: BrandConfig,
  rewards: List<Reward>,
  transactions: List<LoyaltyTransaction>,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onAddPoints: (billAmountMad: Double, manualPoints: Int, note: String) -> Unit,
  onRedeemReward: (Reward) -> Unit,
) {
  var useBillMode by remember { mutableStateOf(true) }
  var billInput by remember { mutableStateOf("") }
  var manualPointsInput by remember { mutableStateOf("") }
  var noteInput by remember { mutableStateOf("") }

  val billVal = billInput.toDoubleOrNull() ?: 0.0
  val previewPoints =
    if (useBillMode) (billVal / config.pointsPerMad.coerceAtLeast(1)).toInt()
    else manualPointsInput.toIntOrNull() ?: 0

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Column {
        Text(customer.name, fontWeight = FontWeight.Bold)
        Text(
          "${customer.phone} • ${customer.points} ${strings.points}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(strings.addPoints, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = useBillMode,
            onClick = { useBillMode = true },
            label = { Text(strings.byBillAmount) },
          )
          FilterChip(
            selected = !useBillMode,
            onClick = { useBillMode = false },
            label = { Text(strings.manualPoints) },
          )
        }
        Spacer(Modifier.height(8.dp))
        if (useBillMode) {
          OutlinedTextField(
            value = billInput,
            onValueChange = { billInput = it },
            label = { Text(strings.billAmountLabel) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
          )
        } else {
          OutlinedTextField(
            value = manualPointsInput,
            onValueChange = { manualPointsInput = it },
            label = { Text(strings.points) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("manual_points_input"),
          )
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(
          value = noteInput,
          onValueChange = { noteInput = it },
          label = { Text(strings.noteLabel) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Button(
          onClick = {
            onAddPoints(if (useBillMode) billVal else 0.0, if (useBillMode) 0 else previewPoints, noteInput)
            billInput = ""
            manualPointsInput = ""
            noteInput = ""
          },
          enabled = previewPoints > 0,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().height(48.dp).testTag("confirm_add_points_button"),
        ) {
          Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("${strings.addPoints} (+$previewPoints)")
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider()
        Spacer(Modifier.height(10.dp))

        Text(strings.redeemReward, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        rewards.forEach { reward ->
          val canRedeem = customer.points >= reward.pointsRequired
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(reward.title, fontWeight = FontWeight.SemiBold)
              Text(
                "${reward.pointsRequired} ${strings.points}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            OutlinedButton(
              onClick = { onRedeemReward(reward) },
              enabled = canRedeem,
              shape = RoundedCornerShape(10.dp),
            ) {
              Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(4.dp))
              Text(strings.redeemReward)
            }
          }
        }

        if (transactions.isNotEmpty()) {
          Spacer(Modifier.height(12.dp))
          HorizontalDivider()
          Spacer(Modifier.height(8.dp))
          Text(strings.pointsHistory, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
          transactions.take(6).forEach { tx ->
            Row(
              modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(tx.note.ifBlank { tx.date.take(10) }, modifier = Modifier.weight(1f))
              Text(
                (if (tx.type == TransactionType.ADD) "+" else "-") + "${tx.points}",
                fontWeight = FontWeight.Bold,
                color =
                  if (tx.type == TransactionType.ADD) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.secondary,
              )
            }
          }
        }
      }
    },
    confirmButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
  )
}

@Composable
private fun TransactionListCard(
  transactions: List<LoyaltyTransaction>,
  config: BrandConfig,
  strings: AppStrings,
) {
  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      transactions.take(10).forEachIndexed { idx, tx ->
        val isAdd = tx.type == TransactionType.ADD
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(tx.customerName.ifBlank { tx.customerId }, fontWeight = FontWeight.Bold)
            val detail =
              if (tx.billAmountMad > 0) "${tx.note} (${tx.billAmountMad.toInt()} ${config.currency})"
              else tx.note
            Text(
              detail,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Text(
            text = (if (isAdd) "+" else "-") + "${tx.points} ${strings.points}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color =
              if (isAdd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
          )
        }
        if (idx < transactions.take(10).lastIndex) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        }
      }
    }
  }
}

@Composable
private fun NfcReaderBannerCard(
  uiState: LoyaltyUiState,
  strings: AppStrings,
  onToggleNfcReader: (Boolean) -> Unit,
  onOpenNfcSettings: () -> Unit,
) {
  val isListening = uiState.isNfcReaderActive && uiState.nfcSupported && uiState.nfcEnabled
  val containerColor =
    when {
      isListening -> MaterialTheme.colorScheme.primaryContainer
      !uiState.nfcSupported || !uiState.nfcEnabled -> MaterialTheme.colorScheme.surfaceVariant
      else -> MaterialTheme.colorScheme.surface
    }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
    elevation = CardDefaults.cardElevation(defaultElevation = if (isListening) 4.dp else 2.dp),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color =
              if (isListening) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.size(44.dp),
          ) {
            Icon(
              imageVector = Icons.Filled.Nfc,
              contentDescription = "NFC",
              tint =
                if (isListening) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(10.dp),
            )
          }
          Spacer(Modifier.width(12.dp))
          Column {
            Text(
              text =
                if (isListening) strings.nfcListeningTitle else strings.activateNfcReader,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text =
                when {
                  isListening -> strings.nfcListeningSubtitle
                  !uiState.nfcSupported -> strings.nfcUnsupportedMsg
                  !uiState.nfcEnabled -> strings.nfcDisabledMsg
                  else -> strings.nfcReadyHint
                },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
      Spacer(Modifier.height(10.dp))
      when {
        !uiState.nfcSupported -> Unit
        !uiState.nfcEnabled -> {
          OutlinedButton(
            onClick = onOpenNfcSettings,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("open_nfc_settings_button"),
          ) {
            Icon(Icons.Filled.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.openNfcSettings)
          }
        }
        isListening -> {
          OutlinedButton(
            onClick = { onToggleNfcReader(false) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("stop_nfc_reader_button"),
          ) {
            Text(strings.stopNfcReader)
          }
        }
        else -> {
          Button(
            onClick = { onToggleNfcReader(true) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("activate_nfc_reader_button"),
          ) {
            Icon(Icons.Filled.Nfc, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(strings.activateNfcReader)
          }
        }
      }
    }
  }
}
