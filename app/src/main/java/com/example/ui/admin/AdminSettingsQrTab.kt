package com.example.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.BrandConfig
import com.example.ui.components.BrandLogoAvatar
import com.example.ui.components.QrCodeImage
import com.example.ui.components.SectionHeaderRow
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.BrandPresets
import com.example.ui.theme.DefaultPrimary
import com.example.ui.theme.DefaultSecondary
import com.example.ui.theme.parseHexColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminSettingsQrTabContent(
  config: BrandConfig,
  currentAdminToken: String,
  strings: AppStrings,
  isLoading: Boolean,
  onSaveSettings: (BrandConfig, String?) -> Unit,
  onTestAppsScript: (String) -> Unit,
  onSimulateCustomerScan: () -> Unit,
  generateConfigJs: () -> String,
  onShowFeedback: (String) -> Unit,
) {
  val context = LocalContext.current
  var businessName by remember(config.businessName) { mutableStateOf(config.businessName) }
  var businessType by remember(config.businessType) { mutableStateOf(config.businessType) }
  var logoUrl by remember(config.logoUriOrUrl) { mutableStateOf(config.logoUriOrUrl) }
  var primaryHex by remember(config.primaryColorHex) { mutableStateOf(config.primaryColorHex) }
  var secondaryHex by remember(config.secondaryColorHex) { mutableStateOf(config.secondaryColorHex) }
  var bgHex by remember(config.backgroundColorHex) { mutableStateOf(config.backgroundColorHex) }
  var welcomeMsg by remember(config.welcomeMessage) { mutableStateOf(config.welcomeMessage) }
  var termsMsg by remember(config.termsText) { mutableStateOf(config.termsText) }
  var privacyMsg by remember(config.privacyText) { mutableStateOf(config.privacyText) }
  var scriptUrl by remember(config.appsScriptUrl) { mutableStateOf(config.appsScriptUrl) }
  var portalUrl by remember(config.customerPortalUrl) { mutableStateOf(config.customerPortalUrl) }
  var newAdminPass by remember { mutableStateOf(currentAdminToken) }
  var showCodeExportDialog by remember { mutableStateOf(false) }

  val photoPickerLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
          )
        }
        logoUrl = uri.toString()
      }
    }

  /* ==================== 1. QR Code & NFC Studio ==================== */
  SectionHeaderRow(title = strings.qrAndNfcTitle)
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val effectiveUrl = portalUrl.ifBlank { "https://walaa-loyalty.example.com/card" }
      QrCodeImage(content = effectiveUrl, sizeDp = 190.dp)
      Spacer(Modifier.height(14.dp))
      OutlinedTextField(
        value = portalUrl,
        onValueChange = { portalUrl = it },
        label = { Text(strings.nfcUriLabel) },
        leadingIcon = { Icon(Icons.Filled.Nfc, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("nfc_portal_url_input"),
      )
      Spacer(Modifier.height(10.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            copyToClipboard(context, "NFC Link", effectiveUrl)
            onShowFeedback("تم نسخ رابط NFC و QR بنجاح")
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.copyLink)
        }
        Button(
          onClick = onSimulateCustomerScan,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("simulate_qr_scan_button"),
        ) {
          Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.simulateScan, maxLines = 1)
        }
      }
    }
  }

  Spacer(Modifier.height(18.dp))

  /* ==================== 2. White-label Brand Customizer ==================== */
  SectionHeaderRow(title = strings.brandCustomization)
  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        BrandLogoAvatar(logoUriOrUrl = logoUrl, businessName = businessName, size = 56.dp)
        Spacer(Modifier.width(12.dp))
        OutlinedButton(
          onClick = {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.height(48.dp).testTag("pick_logo_button"),
        ) {
          Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text(strings.pickLogoImage)
        }
      }
      Spacer(Modifier.height(12.dp))
      OutlinedTextField(
        value = logoUrl,
        onValueChange = { logoUrl = it },
        label = { Text(strings.logoUrlOptional) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
        value = businessName,
        onValueChange = { businessName = it },
        label = { Text(strings.businessName) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().testTag("business_name_input"),
      )
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
        value = businessType,
        onValueChange = { businessType = it },
        label = { Text(strings.businessType) },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.height(12.dp))
      Text("لوحات ألوان سريعة", style = MaterialTheme.typography.labelLarge)
      Spacer(Modifier.height(6.dp))
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        BrandPresets.forEach { preset ->
          Row(
            modifier =
              Modifier.clip(RoundedCornerShape(999.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(999.dp))
                .clickable {
                  primaryHex = preset.primaryHex
                  secondaryHex = preset.secondaryHex
                  bgHex = preset.backgroundHex
                }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Box(
              modifier =
                Modifier.size(14.dp)
                  .clip(CircleShape)
                  .background(parseHexColor(preset.primaryHex, DefaultPrimary))
            )
            Spacer(Modifier.width(4.dp))
            Box(
              modifier =
                Modifier.size(14.dp)
                  .clip(CircleShape)
                  .background(parseHexColor(preset.secondaryHex, DefaultSecondary))
            )
            Spacer(Modifier.width(6.dp))
            Text(preset.labelAr, style = MaterialTheme.typography.labelSmall)
          }
        }
      }

      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = primaryHex,
          onValueChange = { primaryHex = it },
          label = { Text(strings.primaryColor) },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = secondaryHex,
          onValueChange = { secondaryHex = it },
          label = { Text(strings.secondaryColor) },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = bgHex,
        onValueChange = { bgHex = it },
        label = { Text(strings.backgroundColor) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(10.dp))
      OutlinedTextField(
        value = welcomeMsg,
        onValueChange = { welcomeMsg = it },
        label = { Text("رسالة الترحيب") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = termsMsg,
        onValueChange = { termsMsg = it },
        label = { Text("شروط الاستخدام") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = privacyMsg,
        onValueChange = { privacyMsg = it },
        label = { Text("سياسة الخصوصية") },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(Modifier.height(14.dp))
      OutlinedTextField(
        value = scriptUrl,
        onValueChange = { scriptUrl = it },
        label = { Text(strings.appsScriptUrlLabel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("apps_script_url_input"),
      )
      Spacer(Modifier.height(8.dp))
      OutlinedTextField(
        value = newAdminPass,
        onValueChange = { newAdminPass = it },
        label = { Text(strings.adminPasswordHint) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(12.dp))
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = { onTestAppsScript(scriptUrl) },
          enabled = scriptUrl.isNotBlank() && !isLoading,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp),
        ) {
          Text(strings.testConnection)
        }
        Button(
          onClick = {
            onSaveSettings(
              config.copy(
                businessName = businessName.trim().ifBlank { config.businessName },
                businessType = businessType.trim().ifBlank { config.businessType },
                logoUriOrUrl = logoUrl.trim(),
                primaryColorHex = primaryHex.trim(),
                secondaryColorHex = secondaryHex.trim(),
                backgroundColorHex = bgHex.trim(),
                welcomeMessage = welcomeMsg.trim(),
                termsText = termsMsg.trim(),
                privacyText = privacyMsg.trim(),
                appsScriptUrl = scriptUrl.trim(),
                customerPortalUrl = portalUrl.trim(),
              ),
              newAdminPass,
            )
          },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).height(48.dp).testTag("save_brand_settings_button"),
        ) {
          Text(strings.saveSettings)
        }
      }
    }
  }

  Spacer(Modifier.height(14.dp))
  OutlinedButton(
    onClick = { showCodeExportDialog = true },
    shape = RoundedCornerShape(14.dp),
    modifier = Modifier.fillMaxWidth().height(50.dp).testTag("export_code_guide_button"),
  ) {
    Icon(Icons.Filled.Code, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text(strings.exportCodeAndGuide)
  }

  if (showCodeExportDialog) {
    CodeAndGuideExportDialog(
      configJsContent = generateConfigJs(),
      strings = strings,
      onDismiss = { showCodeExportDialog = false },
      onCopied = { onShowFeedback(it) },
    )
  }
}

@Composable
private fun CodeAndGuideExportDialog(
  configJsContent: String,
  strings: AppStrings,
  onDismiss: () -> Unit,
  onCopied: (String) -> Unit,
) {
  val context = LocalContext.current
  var selectedDoc by remember { mutableStateOf("config.js") }
  val codeGs = remember { readAssetText(context, "Code.gs") }
  val readmeMd = remember { readAssetText(context, "README.md") }

  val displayedText =
    when (selectedDoc) {
      "config.js" -> configJsContent
      "Code.gs" -> codeGs
      else -> readmeMd
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("تصدير ملفات العميل (White-label)", fontWeight = FontWeight.Bold) },
    text = {
      Column {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("config.js", "Code.gs", "README").forEach { tab ->
            FilterChip(
              selected = selectedDoc == tab,
              onClick = { selectedDoc = tab },
              label = { Text(tab) },
            )
          }
        }
        Spacer(Modifier.height(8.dp))
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(260.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .verticalScroll(rememberScrollState())
              .padding(12.dp)
        ) {
          Text(text = displayedText, style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
          onClick = {
            val shareIntent =
              Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, selectedDoc)
                putExtra(Intent.EXTRA_TEXT, displayedText)
              }
            context.startActivity(Intent.createChooser(shareIntent, selectedDoc))
          }
        ) {
          Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("مشاركة")
        }
        Button(
          onClick = {
            copyToClipboard(context, selectedDoc, displayedText)
            onCopied("تم نسخ $selectedDoc إلى الحافظة")
          }
        ) {
          Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(Modifier.width(4.dp))
          Text("نسخ")
        }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(strings.close) } },
  )
}

private fun readAssetText(context: Context, fileName: String): String {
  return runCatching {
      context.assets.open(fileName).bufferedReader().use { it.readText() }
    }
    .getOrElse { "الملف غير متوفر: $fileName" }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
  val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
