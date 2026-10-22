package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Every colour the app uses, by role. Two palettes: light (deep teal on warm paper) and dark
 * (charcoal with mint). Screens read colours through the named accessors below, so switching
 * palette re-themes every screen, including older ones written against the previous names.
 */
data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    /** Filled chips, segmented tracks, input fills. */
    val surface2: Color,
    /** Progress tracks, pressed fills. */
    val surface3: Color,
    /** Selected-but-quiet fills (selected nav, light tint). */
    val surface4: Color,
    val border: Color,
    val borderSubtle: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentPressed: Color,
    val onAccent: Color,
    /** Light tint of the accent: icon tiles, banners, selected chips. */
    val accentSoft: Color,
    val income: Color,
    val expense: Color,
    val expenseSoft: Color,
    val transfer: Color,
    val warning: Color,
    val danger: Color,
    /** The big summary cards (deep teal in both themes). */
    val hero: Color,
    val onHero: Color,
    val onHeroMuted: Color
)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF6F5F1),
    card = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEFEEE9),
    surface3 = Color(0xFFE6E9E5),
    surface4 = Color(0xFFDCEDE4),
    border = Color(0xFFE6E9E5),
    borderSubtle = Color(0xFFEEF0EC),
    textPrimary = Color(0xFF202923),
    textSecondary = Color(0xFF58665E),
    textTertiary = Color(0xFF68756D),
    accent = Color(0xFF123F36),
    accentPressed = Color(0xFF0D3029),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFDCEDE4),
    income = Color(0xFF16805D),
    expense = Color(0xFFBF543C),
    expenseSoft = Color(0xFFFBEAE4),
    transfer = Color(0xFF4F6B7A),
    warning = Color(0xFFB7791F),
    danger = Color(0xFFBF543C),
    hero = Color(0xFF123F36),
    onHero = Color(0xFFFFFFFF),
    onHeroMuted = Color(0xFFB9D3C7)
)

val DarkAppColors = AppColors(
    isDark = true,
    background = Color(0xFF111815),
    card = Color(0xFF1B2520),
    surface2 = Color(0xFF232E28),
    surface3 = Color(0xFF2C3832),
    surface4 = Color(0xFF24392F),
    border = Color(0xFF2A352F),
    borderSubtle = Color(0xFF222C27),
    textPrimary = Color(0xFFEDF3EF),
    textSecondary = Color(0xFFB8C7BD),
    textTertiary = Color(0xFFA2B1A7),
    accent = Color(0xFF8FD4B5),
    accentPressed = Color(0xFF7BC4A3),
    onAccent = Color(0xFF0D2A22),
    accentSoft = Color(0xFF24392F),
    income = Color(0xFF6FCF9F),
    expense = Color(0xFFF0907A),
    expenseSoft = Color(0xFF3A2621),
    transfer = Color(0xFF9DB7C6),
    warning = Color(0xFFE5B45C),
    danger = Color(0xFFF0907A),
    hero = Color(0xFF1F4A3F),
    onHero = Color(0xFFEDF3EF),
    onHeroMuted = Color(0xFFA9C9BB)
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Shorthand for the current palette. */
val AppPalette: AppColors
    @Composable @ReadOnlyComposable get() = LocalAppColors.current

// ---------------------------------------------------------------------------------------------
// Role accessors. The names are kept from the first version of the app so every existing
// screen follows the active palette; read them as roles, not as literal colours.
// ---------------------------------------------------------------------------------------------
val OledBlack: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.background
val AppleDarkBackground: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.background
val AppleSurface1: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.card
val AppleSurface2: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface2
val AppleSurface3: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface3
val AppleSurface4: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface4
val AppleBorder: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.border
val AppleBorderSubtle: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.borderSubtle
val AppleTextPrimary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textPrimary
val AppleTextSecondary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textSecondary
val AppleTextTertiary: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.textTertiary
/** The accent (was orange; now deep teal / mint). */
val AppleOrange: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accent
val AppleOrangeLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accentSoft
val AppleGreen: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.income
val AppleGreenLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accentSoft
val AppleRed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.expense
val AppleRedLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.expenseSoft
val AppleBlue: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.transfer
val AppleBlueLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface2
val ApplePurple: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.transfer
val AppleCyan: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.transfer
/** Text/icon colour on top of the accent. */
val OnAccent: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onAccent

val ExpenseRed: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.expense
val ExpenseRedLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.expenseSoft
val IncomeGreen: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.income
val IncomeGreenLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accentSoft
val TransferBlue: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.transfer
val TransferBlueLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface2
val WarningAmber: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning
val AmberGold: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning
val AmberGoldLight: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.surface2
