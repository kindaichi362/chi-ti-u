package com.example.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val AppleDarkColorScheme = darkColorScheme(
    primary = AppleOrange,
    onPrimary = Color.Black,
    primaryContainer = AppleSurface2,
    onPrimaryContainer = AppleTextPrimary,
    secondary = AppleCyan,
    onSecondary = Color.Black,
    secondaryContainer = AppleSurface2,
    onSecondaryContainer = AppleTextPrimary,
    tertiary = AppleGreen,
    onTertiary = Color.Black,
    background = OledBlack,
    surface = AppleSurface1,
    onBackground = AppleTextPrimary,
    onSurface = AppleTextPrimary,
    surfaceVariant = AppleSurface2,
    onSurfaceVariant = AppleTextSecondary,
    outline = AppleBorder,
    outlineVariant = AppleBorderSubtle
)

@Composable
fun ChiTieuThanhPhatTheme(
    darkTheme: Boolean = true, // Default to Apple Dark OLED aesthetic
    dynamicColor: Boolean = false, // Keep signature Apple Watch dark styling consistent
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppleDarkColorScheme,
        typography = Typography,
        content = content
    )
}
