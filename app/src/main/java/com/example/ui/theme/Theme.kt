package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.example.model.BrandConfig
import com.example.model.ThemeMode

@Composable
fun LoyaltyTheme(
  brandConfig: BrandConfig,
  themeMode: ThemeMode,
  content: @Composable () -> Unit,
) {
  val isDark =
    when (themeMode) {
      ThemeMode.SYSTEM -> isSystemInDarkTheme()
      ThemeMode.LIGHT -> false
      ThemeMode.DARK -> true
    }

  val primary = parseHexColor(brandConfig.primaryColorHex, DefaultPrimary)
  val secondary = parseHexColor(brandConfig.secondaryColorHex, DefaultSecondary)
  val customBg = parseHexColor(brandConfig.backgroundColorHex, DefaultBackground)

  val colorScheme =
    if (isDark) {
      val darkPrimary = lerp(primary, Color.White, 0.25f)
      val darkSecondary = lerp(secondary, Color.White, 0.22f)
      val darkBg = lerp(Color(0xFF0E1413), primary, 0.08f)
      val darkSurface = lerp(Color(0xFF16201E), primary, 0.09f)
      val darkSurfaceVariant = lerp(Color(0xFF202D2A), primary, 0.12f)
      darkColorScheme(
        primary = darkPrimary,
        onPrimary = darkPrimary.contrastContentColor(),
        primaryContainer = lerp(primary, Color.Black, 0.45f),
        onPrimaryContainer = Color(0xFFE6F7F4),
        secondary = darkSecondary,
        onSecondary = darkSecondary.contrastContentColor(),
        secondaryContainer = lerp(secondary, Color.Black, 0.50f),
        onSecondaryContainer = Color(0xFFFFF1D6),
        background = darkBg,
        onBackground = Color(0xFFEDF4F2),
        surface = darkSurface,
        onSurface = Color(0xFFEDF4F2),
        surfaceVariant = darkSurfaceVariant,
        onSurfaceVariant = Color(0xFFB3C5C1),
        outline = Color(0xFF394B47),
      )
    } else {
      val lightSurface = Color.White
      val lightSurfaceVariant = lerp(customBg, primary, 0.07f)
      lightColorScheme(
        primary = primary,
        onPrimary = primary.contrastContentColor(),
        primaryContainer = lerp(primary, Color.White, 0.84f),
        onPrimaryContainer = lerp(primary, Color.Black, 0.55f),
        secondary = secondary,
        onSecondary = secondary.contrastContentColor(),
        secondaryContainer = lerp(secondary, Color.White, 0.84f),
        onSecondaryContainer = lerp(secondary, Color.Black, 0.55f),
        background = customBg,
        onBackground = Color(0xFF14211F),
        surface = lightSurface,
        onSurface = Color(0xFF14211F),
        surfaceVariant = lightSurfaceVariant,
        onSurfaceVariant = Color(0xFF4F615E),
        outline = lerp(primary, Color.White, 0.72f),
      )
    }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = LoyaltyTypography,
    content = content,
  )
}
