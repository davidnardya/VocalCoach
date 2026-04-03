package org.davidnardya.vocalcoach.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Deep Charcoal (#121212) — canonical app scaffold background; keep in sync with Material [background]. */
val DeepCharcoal: Color = Color(0xFF121212)

private val ElectricOrange = Color(0xFFFF8F00)
private val BloodRed = Color(0xFFB22222)

val BrutalMetalColorScheme: ColorScheme = darkColorScheme(
    primary = ElectricOrange,
    onPrimary = DeepCharcoal,
    secondary = BloodRed,
    onSecondary = Color.White,
    background = DeepCharcoal,
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = BloodRed,
    onError = Color.White,
    outline = Color(0xFF444444),
)

val BrutalMetalTypography: Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 72.sp,
        letterSpacing = 1.sp,
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 52.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun BrutalMetalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BrutalMetalColorScheme,
        typography = BrutalMetalTypography,
        content = content,
    )
}
