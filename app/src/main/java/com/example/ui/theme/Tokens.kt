package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One scale for spacing, radius and type across every screen, so blocks line up and the same
 * thing looks the same everywhere. Screens use these instead of ad-hoc numbers.
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
}

object Radius {
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(20.dp)
}

/** Seven sizes and that is all: label · body · card title · block title · page title · figure · hero. */
object TextSize {
    val xs = 12.sp
    val sm = 14.sp
    val base = 16.sp
    val lg = 18.sp
    val xl = 22.sp
    val figure = 24.sp
    val hero = 34.sp
}

/** Status colours by meaning, so "over budget" is the same red everywhere. */
object Tone {
    val income = AppleGreen
    val expense = AppleRed
    val transfer = AppleBlue
    val warning = AppleOrange
    val danger = AppleRed
    val ok = AppleGreen
    val accent = AppleOrange
    val hairline = Color(0xFF26262A)
}
