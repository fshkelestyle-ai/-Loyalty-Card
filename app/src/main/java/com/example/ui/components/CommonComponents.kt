package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BrandConfig
import com.example.model.Customer
import com.example.model.Reward
import com.example.ui.i18n.AppStrings
import com.example.ui.theme.DefaultPrimary
import com.example.ui.theme.DefaultSecondary
import com.example.ui.theme.parseHexColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun BrandLogoAvatar(
  logoUriOrUrl: String,
  businessName: String,
  size: Dp = 46.dp,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.size(size),
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.primaryContainer,
    tonalElevation = 2.dp,
  ) {
    if (logoUriOrUrl.isNotBlank()) {
      AsyncImage(
        model = logoUriOrUrl,
        contentDescription = businessName,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Crop,
        error = painterResource(id = R.drawable.img_default_brand_logo),
      )
    } else {
      Image(
        painter = painterResource(id = R.drawable.img_default_brand_logo),
        contentDescription = businessName,
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)),
        contentScale = ContentScale.Crop,
      )
    }
  }
}

@Composable
fun DigitalLoyaltyCard(
  config: BrandConfig,
  customer: Customer,
  nextReward: Reward?,
  strings: AppStrings,
  modifier: Modifier = Modifier,
) {
  val primary = parseHexColor(config.primaryColorHex, DefaultPrimary)
  val secondary = parseHexColor(config.secondaryColorHex, DefaultSecondary)
  val darkTone = lerp(primary, Color.Black, 0.42f)

  val rawProgress =
    if (nextReward != null && nextReward.pointsRequired > 0) {
      (customer.points.toFloat() / nextReward.pointsRequired.toFloat()).coerceIn(0f, 1f)
    } else 1f
  val animatedProgress by animateFloatAsState(
    targetValue = rawProgress,
    animationSpec = tween(durationMillis = 500),
    label = "loyalty_progress",
  )

  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .background(Brush.linearGradient(listOf(primary, darkTone)))
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_loyalty_card_pattern),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.matchParentSize().alpha(0.22f),
      )
      Column(modifier = Modifier.fillMaxWidth().padding(22.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            BrandLogoAvatar(
              logoUriOrUrl = config.logoUriOrUrl,
              businessName = config.businessName,
              size = 44.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column {
              Text(
                text = config.businessName,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = config.businessType,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
              )
            }
          }
          Surface(
            color = Color.White.copy(alpha = 0.16f),
            shape = RoundedCornerShape(999.dp),
          ) {
            Text(
              text = customer.id,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }

        Spacer(Modifier.height(22.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Bottom,
        ) {
          Column {
            Text(
              text = customer.name,
              style = MaterialTheme.typography.titleLarge,
              color = Color.White,
            )
            Text(
              text = customer.phone,
              style = MaterialTheme.typography.labelSmall,
              color = Color.White.copy(alpha = 0.82f),
            )
          }
          Row(verticalAlignment = Alignment.Bottom) {
            Icon(
              imageVector = Icons.Filled.Stars,
              contentDescription = null,
              tint = secondary,
              modifier = Modifier.size(28.dp).padding(bottom = 4.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text = "${customer.points}",
              style = MaterialTheme.typography.displaySmall,
              color = Color.White,
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = strings.points,
              style = MaterialTheme.typography.titleMedium,
              color = Color.White.copy(alpha = 0.9f),
              modifier = Modifier.padding(bottom = 4.dp),
            )
          }
        }

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
          progress = { animatedProgress },
          modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
          color = secondary,
          trackColor = Color.White.copy(alpha = 0.24f),
        )

        Spacer(Modifier.height(10.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
              imageVector = Icons.Filled.CardGiftcard,
              contentDescription = null,
              tint = secondary,
              modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
              text =
                if (nextReward != null) {
                  val remaining = (nextReward.pointsRequired - customer.points).coerceAtLeast(0)
                  if (remaining == 0) "${nextReward.title} (${strings.readyToRedeem})"
                  else "${strings.nextReward}: ${nextReward.title} ($remaining ${strings.points})"
                } else strings.allRewardsUnlocked,
              style = MaterialTheme.typography.bodyMedium,
              color = Color.White.copy(alpha = 0.94f),
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          Spacer(Modifier.width(8.dp))
          Text(
            text = "1 ${strings.points} = ${config.pointsPerMad} ${config.currency}",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.85f),
          )
        }
      }
    }
  }
}

@Composable
fun QrCodeImage(
  content: String,
  sizeDp: Dp = 210.dp,
  modifier: Modifier = Modifier,
) {
  val bitmap =
    remember(content) {
      generateQrBitmap(content.ifBlank { "https://example.com" }, 512)
    }

  Surface(
    modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    color = Color.White,
  ) {
    if (bitmap != null) {
      Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code",
        modifier = Modifier.size(sizeDp).padding(12.dp),
      )
    }
  }
}

private fun generateQrBitmap(content: String, sizePx: Int): Bitmap? {
  return runCatching {
      val hints = mapOf(EncodeHintType.MARGIN to 1)
      val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
      val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
      for (x in 0 until sizePx) {
        for (y in 0 until sizePx) {
          bmp.setPixel(x, y, if (matrix[x, y]) AndroidColor.BLACK else AndroidColor.WHITE)
        }
      }
      bmp
    }
    .getOrNull()
}

@Composable
fun PrivacyPolicyDialog(
  config: BrandConfig,
  strings: AppStrings,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
    title = { Text(strings.termsAndPrivacyTitle, fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Text(
          text = config.businessName,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text(text = config.privacyText, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Text(text = config.termsText, style = MaterialTheme.typography.bodyMedium)
      }
    },
    confirmButton = {
      Button(onClick = onDismiss) { Text(strings.close) }
    },
  )
}

@Composable
fun KpiStatCard(
  label: String,
  value: String,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Text(
        text = value,
        style = MaterialTheme.typography.headlineMedium,
        color = accentColor,
        fontWeight = FontWeight.Bold,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = label,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@Composable
fun EmptyHintCard(
  message: String,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(16.dp),
    )
  }
}

@Composable
fun SectionHeaderRow(
  title: String,
  actionLabel: String? = null,
  onAction: (() -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
    )
    if (actionLabel != null && onAction != null) {
      TextButton(onClick = onAction) { Text(actionLabel) }
    }
  }
}
