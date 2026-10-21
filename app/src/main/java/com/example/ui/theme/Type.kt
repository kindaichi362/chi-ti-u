package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System sans (Roboto / the phone's font) renders Vietnamese diacritics well, so no bundled font.
// Letter spacing is 0 throughout: Material's default tracking spreads Vietnamese text too wide.
private fun style(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = FontFamily.Default, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.sp)

val Typography = Typography(
    displaySmall = style(32, 40, FontWeight.Bold),
    headlineMedium = style(26, 32, FontWeight.Bold),
    headlineSmall = style(22, 28, FontWeight.SemiBold),
    titleLarge = style(20, 26, FontWeight.SemiBold),
    titleMedium = style(16, 22, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 22),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium)
)
