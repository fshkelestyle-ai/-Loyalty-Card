package com.example.ui.customer

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AddToHomeScreen
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.data.CustomerCardBundle
import com.example.model.BrandConfig
import com.example.model.TransactionType
import com.example.ui.LoyaltyUiState
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.DigitalLoyaltyCard
import com.example.ui.components.EmptyHintCard
import com.example.ui.components.PrivacyPolicyDialog
import com.example.ui.components.SectionHeaderRow
import com.example.ui.i18n.AppStrings

@Composable
fun CustomerPortalScreen(
  config: BrandConfig,
  uiState: LoyaltyUiState,
  strings: AppStrings,
  onRegister: (name: String, phone: String, email: String, consent: Boolean) -> Unit,
  onLookupPhone: (phone: String) -> Unit,
  onRefreshCard: () -> Unit,
  onSwitchCustomer: () -> Unit,
  onShowFeedback: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showPrivacyDialog by remember { mutableStateOf(false) }

  if (showPrivacyDialog) {
    PrivacyPolicyDialog(
      config = config,
      strings = strings,
      onDismiss = { showPrivacyDialog = false },
    )
  }

  Column(
    modifier =
      modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
      val cardBundle = uiState.customerCard
      if (cardBundle == null) {
        FirstVisitOrLookupView(
          config = config,
          strings = strings,
          isLoading = uiState.isLoading,
          onRegister = onRegister,
          onLookupPhone = onLookupPhone,
          onOpenPrivacy = { showPrivacyDialog = true },
        )
      } else {
        ActiveCustomerCardView(
          config = config,
          bundle = cardBundle,
          strings = strings,
          onRefresh = onRefreshCard,
          onSwitchCustomer = onSwitchCustomer,
          onOpenPrivacy = { showPrivacyDialog = true },
          onShowFeedback = onShowFeedback,
        )
      }
      Spacer(Modifier.height(24.dp))
    }
  }
}

@Composable
private fun FirstVisitOrLookupView(
  config: BrandConfig,
  strings: AppStrings,
  isLoading: Boolean,
  onRegister: (name: String, phone: String, email: String, consent: Boolean) -> Unit,
  onLookupPhone: (phone: String) -> Unit,
  onOpenPrivacy: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var phone by remember { mutableStateOf("") }
  var email by remember { mutableStateOf("") }
  var consent by remember { mutableStateOf(false) }
  var lookupPhone by remember { mutableStateOf("") }

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        BrandLogoAvatar(
          logoUriOrUrl = config.logoUriOrUrl,
          businessName = config.businessName,
          size = 54.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column {
          Text(
            text = config.businessName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
          )
          Text(
            text = config.businessType,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      Spacer(Modifier.height(14.dp))
      Text(
        text = strings.registerTitle,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(6.dp))
      Text(
        text = config.welcomeMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Spacer(Modifier.height(16.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text(strings.fullName) },
        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
      )

      Spacer(Modifier.height(10.dp))

      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text(strings.phoneHint) },
        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
      )

      Spacer(Modifier.height(10.dp))

      OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text(strings.emailOptional) },
        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("customer_email_input"),
      )

      Spacer(Modifier.height(12.dp))

      Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Checkbox(
          checked = consent,
          onCheckedChange = { consent = it },
          modifier = Modifier.testTag("customer_consent_checkbox"),
        )
        Column(modifier = Modifier.padding(top = 6.dp)) {
          Text(text = strings.consentText, style = MaterialTheme.typography.bodyMedium)
          TextButton(onClick = onOpenPrivacy) { Text(strings.viewPrivacyPolicy) }
        }
      }

      Spacer(Modifier.height(10.dp))

      Button(
        onClick = { onRegister(name, phone, email, consent) },
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create_card_button"),
      ) {
        Text(strings.createMyCard, style = MaterialTheme.typography.titleMedium)
      }
    }
  }

  Spacer(Modifier.height(16.dp))

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Text(
        text = strings.alreadyHaveCard,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = lookupPhone,
          onValueChange = { lookupPhone = it },
          label = { Text("06XXXXXXXX") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("lookup_phone_input"),
        )
        Spacer(Modifier.width(10.dp))
        Button(
          onClick = { onLookupPhone(lookupPhone) },
          enabled = !isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(54.dp).testTag("open_my_card_button"),
        ) {
          Text(strings.openMyCard)
        }
      }
      if (config.appsScriptUrl.isBlank()) {
        Spacer(Modifier.height(10.dp))
        AssistChip(
          onClick = {
            lookupPhone = "0661234567"
            onLookupPhone("0661234567")
          },
          label = { Text("تجربة سريعة: سلمى الإدريسي (0661234567)") },
        )
      }
    }
  }
}

@Composable
private fun ActiveCustomerCardView(
  config: BrandConfig,
  bundle: CustomerCardBundle,
  strings: AppStrings,
  onRefresh: () -> Unit,
  onSwitchCustomer: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onShowFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  val customer = bundle.customer
  val nextReward =
    bundle.rewards.sortedBy { it.pointsRequired }.firstOrNull { it.pointsRequired > customer.points }
      ?: bundle.rewards.maxByOrNull { it.pointsRequired }

  DigitalLoyaltyCard(
    config = config,
    customer = customer,
    nextReward = nextReward,
    strings = strings,
  )

  Spacer(Modifier.height(14.dp))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    OutlinedButton(
      onClick = {
        if (ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
          val shortcut =
            ShortcutInfoCompat.Builder(context, "loyalty_card_${customer.id}")
              .setShortLabel(config.businessName.take(16))
              .setLongLabel("${config.businessName} - ${customer.name}")
              .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
              .setIntent(
                Intent(context, MainActivity::class.java).apply {
                  action = Intent.ACTION_VIEW
                }
              )
              .build()
          ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
          onShowFeedback("تم إرسال طلب إضافة البطاقة للشاشة الرئيسية")
        } else {
          onShowFeedback("يمكنك تثبيت تطبيق الويب (PWA) من المتصفح عبر 'إضافة للشاشة الرئيسية'")
        }
      },
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.weight(1f).height(48.dp).testTag("pin_shortcut_button"),
    ) {
      Icon(Icons.Filled.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text(strings.addToHomeScreen, maxLines = 1)
    }

    IconButton(
      onClick = onRefresh,
      modifier = Modifier.size(48.dp).testTag("refresh_customer_button"),
    ) {
      Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
    }
  }

  Spacer(Modifier.height(18.dp))

  SectionHeaderRow(title = strings.availableRewards)
  if (bundle.rewards.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        bundle.rewards.forEachIndexed { idx, reward ->
          val unlocked = customer.points >= reward.pointsRequired
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector =
                  if (unlocked) Icons.Filled.CheckCircle else Icons.Filled.CardGiftcard,
                contentDescription = null,
                tint =
                  if (unlocked) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Spacer(Modifier.width(10.dp))
              Column {
                Text(reward.title, fontWeight = FontWeight.Bold)
                if (reward.description.isNotBlank()) {
                  Text(
                    reward.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            Surface(
              color =
                if (unlocked) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(999.dp),
            ) {
              Text(
                text = "${reward.pointsRequired} ${strings.points}",
                style = MaterialTheme.typography.labelLarge,
                color =
                  if (unlocked) MaterialTheme.colorScheme.onPrimaryContainer
                  else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
              )
            }
          }
          if (idx < bundle.rewards.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
          }
        }
      }
    }
  }

  Spacer(Modifier.height(18.dp))

  SectionHeaderRow(title = strings.currentPromotions)
  if (bundle.promotions.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    bundle.promotions.forEach { promo ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
          Icon(
            Icons.Filled.Campaign,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
          )
          Spacer(Modifier.width(10.dp))
          Column {
            Text(
              promo.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              promo.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    }
  }

  Spacer(Modifier.height(18.dp))

  SectionHeaderRow(title = strings.pointsHistory)
  if (bundle.transactions.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    Card(
      shape = RoundedCornerShape(18.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        bundle.transactions.forEachIndexed { idx, tx ->
          val isAdd = tx.type == TransactionType.ADD
          Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = if (isAdd) Icons.Filled.LocalOffer else Icons.Filled.History,
                contentDescription = null,
                tint =
                  if (isAdd) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.secondary,
              )
              Spacer(Modifier.width(10.dp))
              Column {
                Text(tx.note.ifBlank { tx.type.name }, fontWeight = FontWeight.SemiBold)
                Text(
                  tx.date.take(10),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            Text(
              text = (if (isAdd) "+" else "-") + "${tx.points} ${strings.points}",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color =
                if (isAdd) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondary,
            )
          }
          if (idx < bundle.transactions.lastIndex) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
          }
        }
      }
    }
  }

  Spacer(Modifier.height(16.dp))

  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    TextButton(onClick = onOpenPrivacy) { Text(strings.viewPrivacyPolicy) }
    TextButton(
      onClick = onSwitchCustomer,
      modifier = Modifier.testTag("switch_customer_button"),
    ) {
      Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(6.dp))
      Text(strings.switchAccount)
    }
  }
}
