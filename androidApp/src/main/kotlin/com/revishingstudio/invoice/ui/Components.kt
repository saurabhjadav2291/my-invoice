package com.revishingstudio.invoice.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Screen side margin from design.md section 4.1. */
val ScreenMargin = 20.dp

/** Corner radius of text fields and form buttons (design.md section 4.2). */
val FormCorner = 8.dp
val FormShape = RoundedCornerShape(FormCorner)

/** Tags: status, invoice numbers, currency codes. */
val TagShape = RoundedCornerShape(6.dp)

/** Cards and the tab bar. */
val CardShape = RoundedCornerShape(16.dp)

/** One easing curve for every transition (design.md section 9). */
val AppEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private val ShadowColor = Color(0xFF111827)

/**
 * A plain raised surface: white, a hairline, and a soft neutral shadow when [floating].
 * Depth comes from surface steps and hairlines, never from colour.
 */
@Composable
fun Modifier.surface(shape: Shape, color: Color = Ui.colors.surface, floating: Boolean = false): Modifier = this
    .then(
        if (floating) {
            Modifier.shadow(12.dp, shape, ambientColor = ShadowColor.copy(alpha = 0.06f), spotColor = ShadowColor.copy(alpha = 0.10f))
        } else {
            Modifier
        },
    )
    .clip(shape)
    .background(color)
    .border(1.dp, Ui.colors.hairline, shape)

// ---------------------------------------------------------------------------
// Progressive blur at the screen edges (design.md section 5.5)
// ---------------------------------------------------------------------------

/**
 * Progressive blur band. [blurAtTop] true for the band over the status bar (strongest at
 * the top edge), false for the band behind the bottom navigation. Android 12+ blurs; older
 * phones get the tint gradient alone.
 */
fun Modifier.progressiveBlur(state: HazeState, tint: Color, blurAtTop: Boolean, blur: Dp): Modifier =
    hazeEffect(state) {
        blurRadius = blur
        noiseFactor = 0f
        val brush = Brush.verticalGradient(
            if (blurAtTop) listOf(tint, Color.Transparent) else listOf(Color.Transparent, tint),
        )
        tints = listOf(HazeTint(brush))
        fallbackTint = HazeTint(brush)
        progressive = if (blurAtTop) {
            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
        } else {
            HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f)
        }
    }

// ---------------------------------------------------------------------------
// Backgrounds and headers
// ---------------------------------------------------------------------------

/** The page behind every screen: the plain canvas colour. */
@Composable
fun AppBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier
            .fillMaxSize()
            .background(Ui.colors.background),
        content = content,
    )
}

/**
 * Header of a top-level screen: an optional logo, the page title and a short line under it,
 * with actions on the right. It scrolls with the content.
 */
@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    avatar: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val c = Ui.colors
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = ScreenMargin, end = ScreenMargin, top = 24.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatar != null) {
            avatar()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.greyText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** 44 dp square tile with the studio logo, or the first letter of a name. */
@Composable
fun Avatar(logo: ImageBitmap?, name: String, size: Dp = 44.dp) {
    val c = Ui.colors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(size)
            .surface(shape, color = if (logo != null) c.surface else c.surfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            Image(logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(4.dp).fillMaxSize())
        } else {
            Text(name.trim().take(1).uppercase().ifEmpty { "?" }, style = MaterialTheme.typography.titleMedium, color = c.greyText)
        }
    }
}

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

/** White card with a hairline. Tappable cards step to the raised surface while pressed. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ui.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill by animateColorAsState(if (pressed) c.surfaceRaised else c.surface, tween(150, easing = AppEasing), label = "card press")
    Column(
        modifier
            .fillMaxWidth()
            .surface(CardShape, color = fill)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/**
 * The card holding a screen's key figures. Drawn like the paper invoice: white with the
 * PDF's orange rule along the top edge.
 */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Ui.colors
    Column(
        modifier
            .fillMaxWidth()
            .surface(RoundedCornerShape(20.dp), floating = true),
    ) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(c.paperAccent))
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp), content = content)
    }
}

/** Heading above a group of cards, with an optional note on the right. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 0.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Ui.colors.ink, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = Ui.colors.grey)
        }
    }
}

// ---------------------------------------------------------------------------
// Tags
// ---------------------------------------------------------------------------

/** Invoice number in mono, the app's signature detail: RS/26-27/031. */
@Composable
fun InvoiceNumber(number: String, modifier: Modifier = Modifier, color: Color = Ui.colors.greyText) {
    Text(number, style = MaterialTheme.typography.bodySmall.serial(), color = color, maxLines = 1, modifier = modifier)
}

// ---------------------------------------------------------------------------
// Icon buttons. Buttons with a text label are AppButton in Buttons.kt.
// ---------------------------------------------------------------------------

/** Light: white with a hairline. Quiet: the raised surface, for small in-card actions. */
enum class RoundStyle { Light, Quiet }

/** Round icon button. Icons take the secondary text colour. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RoundStyle = RoundStyle.Light,
    size: Dp = 40.dp,
) {
    val c = Ui.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val fill = when {
        pressed -> c.hairline.copy(alpha = 0.14f).compositeOn(c.surfaceRaised)
        style == RoundStyle.Quiet -> c.surfaceRaised
        else -> c.surface
    }
    Box(
        modifier
            .size(size)
            .then(if (style == RoundStyle.Light) Modifier.surface(CircleShape, color = fill) else Modifier.clip(CircleShape).background(fill))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = c.greyText, modifier = Modifier.size(18.dp))
    }
}

private fun Color.compositeOn(background: Color): Color {
    val a = alpha
    return Color(
        red = red * a + background.red * (1 - a),
        green = green * a + background.green * (1 - a),
        blue = blue * a + background.blue * (1 - a),
    )
}

// ---------------------------------------------------------------------------
// Segmented tabs (design.md section 6.4)
// ---------------------------------------------------------------------------

private val TrackColor = Color(0xFFECEEF1)
private val KeyTop = Color(0xFFDFE2E7)
private val KeyBottom = Color(0xFFD5D9DF)

/**
 * Soft segmented tabs: a pale pill track, and the chosen option pressed into it as a
 * grey pill with a white rim. With [fill] the options share the width equally; without it
 * each option sizes to its label (for a row that scrolls).
 */
@Composable
fun <T> PillChoice(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
) {
    val c = Ui.colors
    Row(
        modifier
            .shadow(4.dp, CircleShape, ambientColor = c.shadow.copy(alpha = 0.05f), spotColor = c.shadow.copy(alpha = 0.08f))
            .background(TrackColor, CircleShape)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.2f))), CircleShape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val text by animateColorAsState(if (isSelected) c.ink else c.greyText, tween(200, easing = AppEasing), label = "tab text")
            Box(
                Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .height(36.dp)
                    .then(
                        if (isSelected) {
                            Modifier
                                .background(Brush.verticalGradient(listOf(KeyTop, KeyBottom)), CircleShape)
                                .drawWithContent {
                                    drawContent()
                                    // Pressed in: a soft shade under the top edge.
                                    drawRoundRect(
                                        Brush.verticalGradient(listOf(c.shadow.copy(alpha = 0.10f), Color.Transparent), endY = 8.dp.toPx()),
                                        cornerRadius = CornerRadius(size.height / 2),
                                    )
                                }
                                .border(1.5.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.5f), Color.White)), CircleShape)
                        } else {
                            Modifier
                        },
                    )
                    .clip(CircleShape)
                    .selectable(selected = isSelected, onClick = { onSelect(option) }, role = Role.Tab)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label(option), color = text, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

/** Small quiet tag, e.g. a currency code. [mono] sets invoice numbers in the serial face. */
@Composable
fun OutlineChip(text: String, modifier: Modifier = Modifier, mono: Boolean = false) {
    val c = Ui.colors
    Box(
        modifier
            .clip(TagShape)
            .background(c.surfaceRaised)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = if (mono) MaterialTheme.typography.labelSmall.serial() else MaterialTheme.typography.labelMedium,
            color = c.greyText,
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------------
// Alert banner (design.md section 6.8)
// ---------------------------------------------------------------------------

enum class BannerKind { Success, Error, Notice }

/**
 * Frosted white banner with a coloured line icon, a message and a quiet second line,
 * e.g. "Payment recorded" over "20:31 · Invoice RS/26-27/031". Used for confirmations
 * and errors.
 */
@Composable
fun AlertBanner(title: String, subtitle: String?, kind: BannerKind, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val (icon, tint) = when (kind) {
        BannerKind.Success -> AppIcons.CheckCircle to c.paid.content
        BannerKind.Error -> AppIcons.AlertCircle to c.error
        BannerKind.Notice -> AppIcons.AlertCircle to c.primary
    }
    Row(
        modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = c.shadow.copy(alpha = 0.06f), spotColor = c.shadow.copy(alpha = 0.10f))
            .background(Brush.verticalGradient(listOf(Color.White, c.frosted)), RoundedCornerShape(20.dp))
            .border(1.dp, c.hairline, RoundedCornerShape(20.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.grey)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bottom navigation (design.md section 6.6)
// ---------------------------------------------------------------------------

class NavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Height of the bar row: 8 dp above, the 72 dp bar, 12 dp below. */
val NavBarRowHeight = 92.dp

/**
 * Frosted white pill bar. Tabs show an icon over a label, blue when selected and grey
 * otherwise. The raised blue + button sits in the middle, with the first tabs on its left
 * and the rest on its right.
 */
@Composable
fun FloatingNavBar(items: List<NavItem>, onNewInvoice: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val split = (items.size + 1) / 2
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 12.dp)
            .height(72.dp)
            .shadow(20.dp, CircleShape, ambientColor = c.shadow.copy(alpha = 0.06f), spotColor = c.shadow.copy(alpha = 0.12f))
            .background(Brush.verticalGradient(listOf(Color.White, c.frosted, Color(0xFFF3F5F8))), CircleShape)
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, c.shadow.copy(alpha = 0.06f))), CircleShape)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(Modifier.weight(1f)) { items.take(split).forEach { NavTab(it, Modifier.weight(1f)) } }
        NewInvoiceButton(onNewInvoice, Modifier.padding(horizontal = 8.dp))
        Row(Modifier.weight(1f)) { items.drop(split).forEach { NavTab(it, Modifier.weight(1f)) } }
    }
}

@Composable
private fun NavTab(item: NavItem, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val tint by animateColorAsState(if (item.selected) c.primary else c.grey, tween(200, easing = AppEasing), label = "tab")
    Column(
        modifier
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = item.selected, onClick = item.onClick, role = Role.Tab),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.height(4.dp))
        Text(item.label, color = tint, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

/** The raised blue + in the middle of the tab bar: a blue disc in a white ring, no glow. */
@Composable
fun NewInvoiceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(200, easing = AppEasing), label = "plus press")
    val fill by animateColorAsState(if (pressed) c.primaryPressed else c.primary, tween(200, easing = AppEasing), label = "plus fill")
    Box(
        modifier
            .size(64.dp)
            .scale(scale)
            .shadow(10.dp, CircleShape, ambientColor = c.shadow.copy(alpha = 0.08f), spotColor = c.shadow.copy(alpha = 0.16f))
            .background(Color.White, CircleShape)
            .padding(4.dp)
            .background(fill, CircleShape)
            .clip(CircleShape)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(AppIcons.Plus, contentDescription = "New invoice", tint = Color.White, modifier = Modifier.size(28.dp))
    }
}

// ---------------------------------------------------------------------------
// Paper invoice (the home screen's moment) and empty states (design.md section 7)
// ---------------------------------------------------------------------------

/**
 * A small sheet of the real invoice, drawn in code: the PDF's orange rule, a mono number
 * and grey lines for the items. Two sheets when [stacked].
 */
@Composable
fun PaperInvoice(number: String?, modifier: Modifier = Modifier, stacked: Boolean = true) {
    Box(modifier.size(width = 84.dp, height = 104.dp)) {
        if (stacked) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(x = 4.dp, y = 2.dp)
                    .size(width = 72.dp, height = 96.dp)
                    .rotate(6f)
                    .surface(RoundedCornerShape(4.dp), color = Ui.colors.background),
            )
        }
        PaperSheet(
            number,
            Modifier
                .align(Alignment.Center)
                .size(width = 72.dp, height = 96.dp)
                .rotate(-3f),
        )
    }
}

@Composable
private fun PaperSheet(number: String?, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Column(modifier.surface(RoundedCornerShape(4.dp), floating = true).padding(8.dp)) {
        Box(Modifier.width(24.dp).height(4.dp).background(c.paperAccent))
        Spacer(Modifier.height(8.dp))
        if (number != null) {
            Text(number.takeLast(3), style = MaterialTheme.typography.labelSmall.serial(), color = c.grey, maxLines = 1)
            Spacer(Modifier.height(4.dp))
        }
        Line(40)
        Spacer(Modifier.height(4.dp))
        Line(32)
        Spacer(Modifier.weight(1f))
        Row {
            Spacer(Modifier.weight(1f))
            Line(20, c.greyText)
        }
    }
}

@Composable
private fun Line(widthDp: Int, color: Color = Ui.colors.surfaceRaised) {
    Box(Modifier.width(widthDp.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(color))
}

/** A flat paper sheet with the screen's icon: the empty-state illustration. */
@Composable
fun EmptyIllustration(icon: ImageVector, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Box(modifier.size(width = 160.dp, height = 136.dp)) {
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = 12.dp, y = 4.dp)
                .size(width = 96.dp, height = 120.dp)
                .rotate(6f)
                .surface(RoundedCornerShape(8.dp), color = c.background),
        )
        Column(
            Modifier
                .align(Alignment.Center)
                .size(width = 96.dp, height = 120.dp)
                .rotate(-3f)
                .surface(RoundedCornerShape(8.dp), floating = true)
                .padding(12.dp),
        ) {
            Box(Modifier.width(32.dp).height(4.dp).background(c.paperAccent))
            Spacer(Modifier.height(12.dp))
            Icon(icon, contentDescription = null, tint = c.grey, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Line(56)
            Spacer(Modifier.height(8.dp))
            Line(40)
        }
    }
}

/** Illustration, title and a short line of help, centred, with an optional button. No background block. */
@Composable
fun IllustratedEmptyState(
    title: String,
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyIllustration(icon)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Ui.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Ui.colors.greyText, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            AppButton(actionLabel, onAction, Modifier.widthIn(min = 160.dp), variant = ButtonVariant.Secondary, icon = AppIcons.Plus)
        }
    }
}
