package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Apple Watch / iOS Dark OLED Aesthetic Palette
// ==========================================
val OledBlack = Color(0xFF000000)
val AppleDarkBackground = Color(0xFF000000)
val AppleSurface1 = Color(0xFF121214)     // Card base
val AppleSurface2 = Color(0xFF1C1C1E)     // Elevated surface / Pill button
val AppleSurface3 = Color(0xFF2C2C2E)     // Selected tab / Input fill
val AppleSurface4 = Color(0xFF3A3A3C)     // Pill highlight
val AppleBorder = Color(0xFF2C2C2E)       // Apple subtle border
val AppleBorderSubtle = Color(0xFF242426)

// Apple Typography Colors
val AppleTextPrimary = Color(0xFFFFFFFF)
val AppleTextSecondary = Color(0xFF8E8E93)
val AppleTextTertiary = Color(0xFF636366)

// Apple System Accents (Vibrant on OLED Dark)
val AppleOrange = Color(0xFFFF9F0A)      // Apple Watch signature amber/orange
val AppleOrangeLight = Color(0x33FF9F0A)
val AppleGreen = Color(0xFF30D158)       // iOS System Green (Income)
val AppleGreenLight = Color(0x2830D158)
val AppleRed = Color(0xFFFF453A)         // iOS System Red (Expense)
val AppleRedLight = Color(0x28FF453A)
val AppleBlue = Color(0xFF0A84FF)        // iOS System Blue (Transfer/Bank)
val AppleBlueLight = Color(0x280A84FF)
val ApplePurple = Color(0xFFBF5AF2)      // iOS System Purple
val AppleCyan = Color(0xFF64D2FF)        // iOS System Cyan

// Compatibility Aliases for existing screens
val EmeraldPrimaryLight = AppleOrange
val EmeraldOnPrimaryLight = Color(0xFF000000)
val EmeraldPrimaryContainerLight = AppleSurface3
val EmeraldOnPrimaryContainerLight = Color(0xFFFFFFFF)

val EmeraldPrimaryDark = AppleOrange
val EmeraldOnPrimaryDark = Color(0xFF000000)
val EmeraldPrimaryContainerDark = AppleSurface2
val EmeraldOnPrimaryContainerDark = AppleTextPrimary

val SecondaryTealLight = AppleCyan
val SecondaryTealDark = AppleCyan

val AmberGold = AppleOrange
val AmberGoldLight = AppleOrangeLight

// Financial Semantics mapped to Apple Dark
val ExpenseRed = AppleRed
val ExpenseRedLight = AppleRedLight
val IncomeGreen = AppleGreen
val IncomeGreenLight = AppleGreenLight
val TransferBlue = AppleBlue
val TransferBlueLight = AppleBlueLight
val WarningAmber = AppleOrange

// Neutrals
val SurfaceLight = Color(0xFF000000) // Default to Apple OLED Dark
val CardLight = AppleSurface1
val TextPrimaryLight = AppleTextPrimary
val TextSecondaryLight = AppleTextSecondary

val SurfaceDark = Color(0xFF000000)
val CardDark = AppleSurface1
val TextPrimaryDark = AppleTextPrimary
val TextSecondaryDark = AppleTextSecondary
