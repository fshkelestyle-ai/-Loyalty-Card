package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val DefaultPrimary = Color(0xFF0F766E)
val DefaultSecondary = Color(0xFFD97706)
val DefaultBackground = Color(0xFFFAF7F2)

data class BrandPreset(
  val labelAr: String,
  val primaryHex: String,
  val secondaryHex: String,
  val backgroundHex: String,
)

val BrandPresets =
  listOf(
    BrandPreset("زمردي وذهبي", "#0F766E", "#D97706", "#FAF7F2"),
    BrandPreset("أطلس دافئ", "#9A3412", "#CA8A04", "#FFFBF5"),
    BrandPreset("أزرق ملكي", "#1D4ED8", "#0EA5E9", "#F5F8FF"),
    BrandPreset("إسبريسو فاخر", "#451A03", "#B45309", "#FAF5F0"),
  )

fun parseHexColor(hex: String, fallback: Color): Color {
  return try {
    val clean = hex.trim().removePrefix("#")
    val value =
      when (clean.length) {
        6 -> ("FF$clean").toLong(16)
        8 -> clean.toLong(16)
        else -> return fallback
      }
    Color(value)
  } catch (_: Exception) {
    fallback
  }
}

fun Color.contrastContentColor(): Color {
  return if (this.luminance() > 0.45f) Color(0xFF10201E) else Color.White
}
