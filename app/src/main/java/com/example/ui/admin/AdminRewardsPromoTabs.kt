package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import com.example.data.AdminDashboardBundle
import com.example.model.BrandConfig
import com.example.model.PromoAudience
import com.example.model.PromoChannel
import com.example.model.Reward
import com.example.model.WhatsappDispatchLink
import com.example.ui.components.EmptyHintCard
import com.example.ui.components.SectionHeaderRow
import com.example.ui.i18n.AppStrings

@Composable
fun AdminRewardsTabContent(
  config: BrandConfig,
  rewards: List<Reward>,
  strings: AppStrings,
  onSaveReward: (Reward) -> Unit,
  onDeleteReward: (String) -> Unit,
  onUpdateMadRate: (Int) -> Unit,
) {
  var madInput by remember(config.pointsPerMad) { mutableStateOf(config.pointsPerMad.toString()) }
  var showAddDialog by remember { mutableStateOf(false) }

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(
        text = strings.loyaltyRuleTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedTextField(
          value = madInput,
          onValueChange = { madInput = it },
          label = { Text(strings.madPerPointLabel) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("mad_rate_input"),
        )
        Spacer(Modifier.width(10.dp))
        Button(
          onClick = {
            val rate = madInput.toIntOrNull()?.coerceAtLeast(1) ?: 10
            onUpdateMadRate(rate)
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(54.dp),
        ) {
          Text(strings.saveSettings)
        }
      }
    }
  }

  Spacer(Modifier.height(16.dp))
  SectionHeaderRow(
    title = strings.availableRewards,
    actionLabel = strings.addReward,
    onAction = { showAddDialog = true },
  )

  if (rewards.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    rewards.forEach { reward ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(reward.title, fontWeight = FontWeight.Bold)
            if (reward.description.isNotBlank()) {
              Text(
                reward.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              "${reward.pointsRequired} ${strings.points}",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
              checked = reward.active,
              onCheckedChange = { onSaveReward(reward.copy(active = it)) },
            )
            IconButton(onClick = { onDeleteReward(reward.id) }) {
              Icon(
                Icons.Filled.Delete,
                contentDescription = "Delete reward",
                tint = MaterialTheme.colorScheme.error,
              )
            }
          }
        }
      }
    }
  }

  Spacer(Modifier.height(10.dp))
  Button(
    onClick = { showAddDialog = true },
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("add_reward_button"),
  ) {
    Icon(Icons.Filled.Add, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text(strings.addReward)
  }

  if (showAddDialog) {
    AddRewardDialog(
      strings = strings,
      onDismiss = { showAddDialog = false },
      onConfirm = { title, desc, pts ->
        onSaveReward(Reward(id = "", title = title, description = desc, pointsRequired = pts, active = true))
        showAddDialog = false
      },
    )
  }
}

@Composable
private fun AddRewardDialog(
  strings: AppStrings,
  onDismiss: () -> Unit,
  onConfirm: (String, String, Int) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var points by remember { mutableStateOf("50") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(strings.addReward, fontWeight = FontWeight.Bold) },
    text = {
      Column {
        OutlinedTextField(
          value = title,
          onValueChange = { title = it },
          label = { Text("اسم المكافأة") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("الوصف") },
          modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
          value = points,
          onValueChange = { points = it },
          label = { Text(strings.points) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val pts = points.toIntOrNull() ?: 0
          if (title.isNotBlank() && pts > 0) onConfirm(title, description, pts)
        }
      ) { Text(strings.saveSettings) }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPromotionsTabContent(
  bundle: AdminDashboardBundle?,
  lastWhatsappLinks: List<WhatsappDispatchLink>,
  strings: AppStrings,
  isLoading: Boolean,
  onSendPromotion: (String, String, PromoChannel, PromoAudience) -> Unit,
) {
  val context = LocalContext.current
  var title by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }
  var channel by remember { mutableStateOf(PromoChannel.BOTH) }
  var audience by remember { mutableStateOf(PromoAudience.ALL) }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          Icons.Filled.Email,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = strings.emailQuotaLeft,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = FontWeight.SemiBold,
        )
      }
      Text(
        text = "${bundle?.stats?.remainingEmailQuota ?: 100}",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }

  Spacer(Modifier.height(14.dp))

  Card(
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      OutlinedTextField(
        value = title,
        onValueChange = { title = it },
        label = { Text(strings.campaignTitle) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("promo_title_input"),
      )
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
        value = message,
        onValueChange = { message = it },
        label = { Text(strings.campaignMessage) },
        minLines = 3,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("promo_message_input"),
      )
      Spacer(Modifier.height(10.dp))

      Text("القناة", style = MaterialTheme.typography.labelLarge)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = channel == PromoChannel.BOTH,
          onClick = { channel = PromoChannel.BOTH },
          label = { Text("بريد + واتساب") },
        )
        FilterChip(
          selected = channel == PromoChannel.EMAIL,
          onClick = { channel = PromoChannel.EMAIL },
          label = { Text("البريد فقط") },
        )
        FilterChip(
          selected = channel == PromoChannel.WHATSAPP,
          onClick = { channel = PromoChannel.WHATSAPP },
          label = { Text("واتساب فقط") },
        )
      }

      Spacer(Modifier.height(8.dp))
      Text("الفئة المستهدفة", style = MaterialTheme.typography.labelLarge)
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = audience == PromoAudience.ALL,
          onClick = { audience = PromoAudience.ALL },
          label = { Text("جميع الموافقين") },
        )
        FilterChip(
          selected = audience == PromoAudience.VIP_100,
          onClick = { audience = PromoAudience.VIP_100 },
          label = { Text("VIP (+100 نقطة)") },
        )
        FilterChip(
          selected = audience == PromoAudience.ACTIVE_30D,
          onClick = { audience = PromoAudience.ACTIVE_30D },
          label = { Text("النشطون (30 يوم)") },
        )
      }

      Spacer(Modifier.height(14.dp))
      Button(
        onClick = { onSendPromotion(title, message, channel, audience) },
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("send_promo_button"),
      ) {
        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(strings.sendCampaign)
      }
    }
  }

  if (lastWhatsappLinks.isNotEmpty()) {
    Spacer(Modifier.height(16.dp))
    SectionHeaderRow(title = strings.whatsappReadyLinks)
    lastWhatsappLinks.forEach { link ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(link.name, fontWeight = FontWeight.Bold)
            Text(link.phone, style = MaterialTheme.typography.labelSmall)
          }
          OutlinedButton(
            onClick = {
              runCatching {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
              }
            },
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("واتساب")
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }

  Spacer(Modifier.height(18.dp))
  SectionHeaderRow(title = strings.sentCampaignsLog)
  val promos = bundle?.promotions.orEmpty()
  if (promos.isEmpty()) {
    EmptyHintCard(strings.noDataYet)
  } else {
    promos.forEach { promo ->
      Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
          Icon(Icons.Filled.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
          Spacer(Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(promo.title, fontWeight = FontWeight.Bold)
            Text(
              promo.message,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
              "${promo.date.take(10)} • ${promo.recipientCount} زبون",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        }
      }
    }
  }
}
