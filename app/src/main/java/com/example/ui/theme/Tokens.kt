package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One scale for spacing, radius and type across every screen (4/8/12/16/24/32 spacing,
 * 18 dp cards, 12 dp controls), so blocks line up and the same thing looks the same everywhere.
 */
object Space {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 24.dp
    val xxxl = 32.dp

    /** Side padding of every screen. */
    val page = 16.dp

    /** Room at the bottom of scrolling screens so the last row clears the bottom bar. */
    val bottomBarClearance = 96.dp

    /** Smallest touch target. */
    val touch = 44.dp
}

object Radius {
    val sm = RoundedCornerShape(8.dp)
    /** Inputs, buttons, chips. */
    val md = RoundedCornerShape(12.dp)
    /** Cards. */
    val lg = RoundedCornerShape(18.dp)
    val xl = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(50)
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

/** label · body · card title · block title · page title · figure · hero figure. */
object TextSize {
    val xs = 12.sp
    val sm = 14.sp
    val base = 16.sp
    val lg = 18.sp
    val xl = 26.sp
    val figure = 24.sp
    val hero = 32.sp
}

/** Money: tabular digits so columns of amounts line up. Pair with [com.example.ui.components.formatVnd], which keeps "₫" glued to the number. */
val MoneyStyle = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

/** Status colours by meaning, so "over budget" is the same red everywhere. */
object Tone {
    val income: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.income
    val expense: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.expense
    val transfer: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.transfer
    val warning: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.warning
    val danger: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.danger
    val ok: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.income
    val accent: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accent
    val onAccent: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.onAccent
    val accentSoft: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.accentSoft
    val hairline: Color @Composable @ReadOnlyComposable get() = LocalAppColors.current.border
}
