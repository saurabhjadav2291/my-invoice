package com.revishingstudio.invoice.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revishingstudio.invoice.R

/** Colours of the warm, glossy look: warm white, soft peach and a bright orange accent. */
object Palette {
    val Orange = Color(0xFFFF5A1F)
    val OrangeLight = Color(0xFFFF9A5E)
    val OrangeDeep = Color(0xFFE2410A)
    val Peach = Color(0xFFFFE6D8)
    val PeachSoft = Color(0xFFFFF2EA)
    val Canvas = Color(0xFFFFFBF8)
    val Ink = Color(0xFF14141A)
    val InkMuted = Color(0xFF5E606E)
    val InkFaint = Color(0xFF8B8D99)
    val Hairline = Color(0xFFEFE4DC)
    val FieldBorder = Color(0xFFE6D8CE)
    val Error = Color(0xFFD93025)
}

/** The orange accent used across the app (matches the invoice design). */
val Accent = Palette.Orange

private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

private val AppTypography = Typography(
    displayLarge = style(48, FontWeight.Bold, 56, -1.0),
    displayMedium = style(40, FontWeight.Bold, 48, -0.8),
    displaySmall = style(36, FontWeight.Bold, 44, -0.6),
    headlineLarge = style(34, FontWeight.Bold, 40, -0.6),
    headlineMedium = style(28, FontWeight.Bold, 34, -0.4),
    headlineSmall = style(24, FontWeight.Bold, 30, -0.3),
    titleLarge = style(20, FontWeight.Bold, 26, -0.2),
    titleMedium = style(16, FontWeight.SemiBold, 22),
    titleSmall = style(14, FontWeight.SemiBold, 20),
    bodyLarge = style(16, FontWeight.Normal, 24),
    bodyMedium = style(14, FontWeight.Normal, 20),
    bodySmall = style(12, FontWeight.Normal, 16),
    labelLarge = style(14, FontWeight.SemiBold, 20),
    labelMedium = style(12, FontWeight.Medium, 16),
    labelSmall = style(11, FontWeight.Medium, 16),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

private val AppColors = lightColorScheme(
    primary = Palette.Orange,
    onPrimary = Color.White,
    primaryContainer = Palette.Peach,
    onPrimaryContainer = Color(0xFF7A2600),
    secondary = Palette.Orange,
    onSecondary = Color.White,
    secondaryContainer = Palette.Peach,
    onSecondaryContainer = Color(0xFFA33A00),
    tertiary = Palette.OrangeDeep,
    background = Palette.Canvas,
    onBackground = Palette.Ink,
    surface = Color.White,
    onSurface = Palette.Ink,
    surfaceVariant = Palette.PeachSoft,
    onSurfaceVariant = Palette.InkMuted,
    surfaceTint = Color.Transparent,
    outline = Palette.FieldBorder,
    outlineVariant = Palette.Hairline,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFAF7),
    surfaceContainer = Color(0xFFFFF8F4),
    surfaceContainerHigh = Color(0xFFFFF5EF),
    surfaceContainerHighest = Color(0xFFFFF0E7),
    error = Palette.Error,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
