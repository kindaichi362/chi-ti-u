package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private fun schemeOf(c: AppColors): ColorScheme {
    val base = if (c.isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = c.accent,
        onPrimary = c.onAccent,
        primaryContainer = c.accentSoft,
        onPrimaryContainer = c.textPrimary,
        secondary = c.accent,
        onSecondary = c.onAccent,
        secondaryContainer = c.accentSoft,
        onSecondaryContainer = c.textPrimary,
        tertiary = c.income,
        onTertiary = c.onAccent,
        background = c.background,
        onBackground = c.textPrimary,
        surface = c.card,
        onSurface = c.textPrimary,
        surfaceVariant = c.surface2,
        onSurfaceVariant = c.textSecondary,
        surfaceContainer = c.card,
        surfaceContainerLow = c.card,
        surfaceContainerHigh = c.card,
        surfaceContainerHighest = c.surface2,
        surfaceContainerLowest = c.card,
        outline = c.border,
        outlineVariant = c.borderSubtle,
        error = c.danger,
        onError = c.onAccent
    )
}

/** Follows the phone's light/dark setting unless the user chose one in Cài đặt. */
@Composable
fun ChiTieuThanhPhatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = schemeOf(colors),
            typography = Typography,
            shapes = AppShapes,
            content = content
        )
    }
}
