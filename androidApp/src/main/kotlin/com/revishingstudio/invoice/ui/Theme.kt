package com.revishingstudio.invoice.ui

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revishingstudio.invoice.R

/** Background and text colours of one invoice status pill. */
@Immutable
data class StatusColors(val content: Color, val fill: Color)

/**
 * Every colour in design.md. Screens use these tokens (or the Material colour scheme
 * built from them), never raw hex values.
 */
@Immutable
data class AppColors(
    /** Always false: the app is light only. Components keep their dark branches in case that changes. */
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val ink: Color,
    val greyText: Color,
    val grey: Color,
    val hairline: Color,
    val primary: Color,
    val primaryText: Color,
    val primarySoft: Color,
    /** Sky header gradient, top to bottom (the last stop is the background). */
    val sky: List<Color>,
    /** Text and icons drawn on the sky header. */
    val onSky: Color,
    /** The "dark" round buttons and selected filter chips. Inverted in dark mode. */
    val inverse: Color,
    val onInverse: Color,
    val neutralChip: Color,
    val tabBar: Color,
    val tabBarIcon: Color,
    val error: Color,
    val draft: StatusColors,
    val sent: StatusColors,
    val partlyPaid: StatusColors,
    val paid: StatusColors,
    val overdue: StatusColors,
)

private val Primary = Color(0xFF438BEF)

val LightAppColors = AppColors(
    isDark = false,
    background = Color(0xFFF2F6F9),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFFFFFFF),
    ink = Color(0xFF000203),
    greyText = Color(0xFF6B6B6C),
    grey = Color(0xFF808081),
    hairline = Color(0xFFE6ECF1),
    primary = Primary,
    primaryText = Color(0xFF2F6FD6),
    primarySoft = Color(0xFFE3EEFD),
    sky = listOf(Color(0xFF3F86EC), Primary, Color(0xFFC9DDFB), Color(0xFFF2F6F9)),
    onSky = Color.White,
    inverse = Color(0xFF000203),
    onInverse = Color.White,
    neutralChip = Color(0xFFF2F6F9),
    tabBar = Color(0xFF000203),
    tabBarIcon = Color.White.copy(alpha = 0.7f),
    error = Color(0xFFD23B3B),
    draft = StatusColors(Color(0xFF6B6B6C), Color(0xFFF2F6F9)),
    sent = StatusColors(Color(0xFF2F6FD6), Color(0xFFE3EEFD)),
    partlyPaid = StatusColors(Color(0xFFB26B00), Color(0xFFFFF3DC)),
    paid = StatusColors(Color(0xFF1E8E4E), Color(0xFFE2F5EA)),
    overdue = StatusColors(Color(0xFFD23B3B), Color(0xFFFDE6E6)),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Shorthand for the current theme's tokens: `Ui.colors.primary`. */
object Ui {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

val Outfit = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Outfit,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/** Type scale from design.md section 3. Only Regular, Medium and SemiBold are used. */
private val AppTypography = Typography(
    displayLarge = style(40, FontWeight.SemiBold, 46, -0.8),
    displayMedium = style(36, FontWeight.SemiBold, 42, -0.6),
    displaySmall = style(32, FontWeight.SemiBold, 38, -0.5), // display
    headlineLarge = style(28, FontWeight.SemiBold, 34, -0.5),
    headlineMedium = style(26, FontWeight.SemiBold, 32, -0.4), // headline
    headlineSmall = style(22, FontWeight.SemiBold, 28, -0.3),
    titleLarge = style(20, FontWeight.SemiBold, 26, -0.2), // title
    titleMedium = style(17, FontWeight.SemiBold, 22, -0.1), // cardTitle
    titleSmall = style(15, FontWeight.SemiBold, 20),
    bodyLarge = style(15, FontWeight.Normal, 22), // body
    bodyMedium = style(15, FontWeight.Normal, 22),
    bodySmall = style(13, FontWeight.Normal, 18), // caption
    labelLarge = style(15, FontWeight.Medium, 20), // button labels
    labelMedium = style(12, FontWeight.Medium, 16, 0.1), // chip
    labelSmall = style(11, FontWeight.Normal, 14, 0.2), // micro
)

/** Amounts use tabular figures so columns of totals line up. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Radii from design.md section 4.2. Buttons, chips and fields set their own full-pill shapes. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(16.dp),
    small = CircleShape,
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private fun schemeFor(c: AppColors) = run {
    lightColorScheme(
        primary = c.primary,
        onPrimary = Color.White,
        primaryContainer = c.primarySoft,
        onPrimaryContainer = c.primaryText,
        secondary = c.primary,
        onSecondary = Color.White,
        secondaryContainer = c.primarySoft,
        onSecondaryContainer = c.primaryText,
        tertiary = c.primaryText,
        background = c.background,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        surfaceVariant = c.neutralChip,
        onSurfaceVariant = c.greyText,
        surfaceTint = Color.Transparent,
        outline = c.hairline,
        outlineVariant = c.hairline,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = c.background,
        inverseSurface = c.inverse,
        inverseOnSurface = c.onInverse,
        error = c.error,
    )
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val colors = LightAppColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        MaterialTheme(
            colorScheme = schemeFor(colors),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
