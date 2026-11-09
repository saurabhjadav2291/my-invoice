package com.revishingstudio.invoice.ui

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

/** Text and fill colours of one invoice status tag. */
@Immutable
data class StatusColors(val content: Color, val fill: Color)

/**
 * Every colour in design.md. Screens use these tokens (or the Material colour scheme
 * built from them), never raw hex values. Surfaces come from one cool neutral ramp;
 * blue is the only accent and marks the primary action, the selection and key figures.
 */
@Immutable
data class AppColors(
    val background: Color,
    val surface: Color,
    /** A step above the surface: inputs, the selected tab, pressed rows. */
    val surfaceRaised: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text. */
    val greyText: Color,
    /** Muted text, meta and icons. */
    val grey: Color,
    val hairline: Color,
    /** Accent fill: primary buttons, the + button, selected states, focus rings. White text on it is 5.4:1. */
    val primary: Color,
    val primaryHover: Color,
    val primaryPressed: Color,
    /** Light blue wash: secondary and link buttons on hover. */
    val primaryTint: Color,
    val primaryTintPressed: Color,
    /** Frosted white of secondary buttons and the tab bar. */
    val frosted: Color,
    /** Tertiary buttons while pressed. */
    val surfacePressed: Color,
    /** Neutral shadow colour; used at 6 to 10% only. */
    val shadow: Color,
    /** Accent used as text or ink on light surfaces. */
    val primaryText: Color,
    val primarySoft: Color,
    /** Dark fills: the snackbar. */
    val inverse: Color,
    val onInverse: Color,
    /** Orange of the invoice PDF, used only where the app shows the paper invoice. */
    val paperAccent: Color,
    val error: Color,
    val draft: StatusColors,
    val sent: StatusColors,
    val partlyPaid: StatusColors,
    val paid: StatusColors,
    val overdue: StatusColors,
)

private val Ink = Color(0xFF111827)

val LightAppColors = AppColors(
    background = Color(0xFFF5F7FA),
    surface = Color(0xFFFFFFFF),
    surfaceRaised = Color(0xFFECEFF4),
    ink = Ink,
    greyText = Color(0xFF4B5565),
    grey = Color(0xFF6B7487),
    hairline = Ink.copy(alpha = 0.08f),
    primary = Color(0xFF155EEF),
    primaryHover = Color(0xFF0B4FD6),
    primaryPressed = Color(0xFF0A45BD),
    primaryTint = Color(0xFFEEF3FE),
    primaryTintPressed = Color(0xFFE1EAFD),
    frosted = Color(0xFFFBFCFD),
    surfacePressed = Color(0xFFE2E6EC),
    shadow = Ink,
    primaryText = Color(0xFF155EEF),
    primarySoft = Color(0xFFEAF1FC),
    inverse = Ink,
    onInverse = Color.White,
    paperAccent = Color(0xFFFF5400),
    error = Color(0xFFDC2626),
    draft = StatusColors(Color(0xFF4B5565), Color(0xFFECEFF4)),
    sent = StatusColors(Color(0xFF2559B8), Color(0xFFEAF1FC)),
    partlyPaid = StatusColors(Color(0xFF9A5B00), Color(0xFFFBF1E0)),
    paid = StatusColors(Color(0xFF15803D), Color(0xFFE6F4EA)),
    overdue = StatusColors(Color(0xFFC62828), Color(0xFFFCEAEA)),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

/** Shorthand for the current theme's tokens: `Ui.colors.primary`. */
object Ui {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
)

/** Partner face for invoice numbers, the app's signature detail. */
val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
)

private fun style(size: Int, weight: FontWeight, line: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = Geist,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

/**
 * Type scale from design.md section 3. Hierarchy comes from size and colour: Regular and
 * Medium only. Negative tracking only on display sizes.
 */
private val AppTypography = Typography(
    displayLarge = style(40, FontWeight.Medium, 44, -0.8),
    displayMedium = style(36, FontWeight.Medium, 40, -0.6),
    displaySmall = style(32, FontWeight.Medium, 36, -0.5), // key figure
    headlineLarge = style(28, FontWeight.Medium, 32, -0.3), // page title
    headlineMedium = style(24, FontWeight.Medium, 28),
    headlineSmall = style(20, FontWeight.Medium, 28),
    titleLarge = style(18, FontWeight.Medium, 24), // section and bar titles
    titleMedium = style(16, FontWeight.Medium, 24), // card title
    titleSmall = style(15, FontWeight.Medium, 20),
    bodyLarge = style(15, FontWeight.Normal, 22), // body
    bodyMedium = style(15, FontWeight.Normal, 22),
    bodySmall = style(13, FontWeight.Normal, 20), // caption
    labelLarge = style(15, FontWeight.Medium, 20), // button labels
    labelMedium = style(12, FontWeight.Medium, 16), // tags
    labelSmall = style(12, FontWeight.Normal, 16, 0.1), // meta
)

/** Amounts use tabular figures so columns of totals line up. */
fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = "tnum")

/** Invoice numbers like RS/26-27/031, set in mono like a serial stamp. */
fun TextStyle.serial(): TextStyle = copy(fontFamily = GeistMono, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

/** Radii from design.md section 4.2: controls 8, tags 6, cards 16, sheets 20. */
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(20.dp),
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
        surfaceVariant = c.surfaceRaised,
        onSurfaceVariant = c.greyText,
        surfaceTint = Color.Transparent,
        outline = c.hairline,
        outlineVariant = c.hairline,
        surfaceContainerLowest = Color.White,
        surfaceContainerLow = Color.White,
        surfaceContainer = Color.White,
        surfaceContainerHigh = Color.White,
        surfaceContainerHighest = c.surfaceRaised,
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
