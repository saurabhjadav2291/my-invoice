package com.revishingstudio.invoice.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Screen side margin from design.md section 4.1. */
val ScreenMargin = 20.dp

private val ShadowInk = Color(0xFF0B2A5B)

/**
 * The page behind every screen: the canvas colour, with the blue sky gradient across
 * the top ~40% when [sky] is true (top-level screens and invoice details).
 */
@Composable
fun AppBackground(sky: Boolean, content: @Composable BoxScope.() -> Unit) {
    val c = Ui.colors
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        if (sky) {
            val height = maxHeight * 0.42f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(height)
                    .background(
                        Brush.verticalGradient(
                            0f to c.sky[0],
                            0.32f to c.sky[1],
                            0.68f to c.sky[2],
                            1f to c.sky[3],
                        ),
                    ),
            )
        }
        content()
    }
}

/**
 * Header of a top-level screen, drawn on the sky: an optional avatar, a big title and a
 * short line under it, with round buttons on the right.
 */
@Composable
fun SkyHeader(
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
            .padding(start = ScreenMargin, end = ScreenMargin, top = 20.dp, bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (avatar != null) {
            avatar()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = c.onSky,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSky.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** 44 dp circle with the studio logo, or the first letter of a name. */
@Composable
fun Avatar(logo: ImageBitmap?, name: String, onSky: Boolean = true, size: Dp = 44.dp) {
    val c = Ui.colors
    val fill = if (onSky) Color.White.copy(alpha = if (c.isDark) 0.10f else 0.25f) else c.primarySoft
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (logo != null) Color.White else fill)
            .border(1.dp, if (onSky) Color.White.copy(alpha = 0.4f) else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (logo != null) {
            Image(logo, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.padding(6.dp).fillMaxSize())
        } else {
            Text(
                name.trim().take(1).uppercase().ifEmpty { "?" },
                style = MaterialTheme.typography.titleMedium,
                color = if (onSky) c.onSky else c.primaryText,
            )
        }
    }
}

/** Plain white card (dark grey in dark mode) with large rounded corners and no shadow. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Ui.colors
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface)
            .then(if (c.isDark) Modifier.border(1.dp, c.hairline, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** The larger card that sits on the sky gradient, holding a screen's headline numbers. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Ui.colors
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .then(
                if (c.isDark) {
                    Modifier
                } else {
                    Modifier.shadow(16.dp, shape, ambientColor = ShadowInk.copy(alpha = 0.06f), spotColor = ShadowInk.copy(alpha = 0.10f))
                },
            )
            .clip(shape)
            .background(c.surfaceRaised)
            .border(1.dp, if (c.isDark) Color.White.copy(alpha = 0.06f) else Color.Transparent, shape)
            .padding(20.dp),
        content = content,
    )
}

/** Heading above a group of cards, with an optional link on the right. */
@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(text, style = MaterialTheme.typography.titleLarge, color = Ui.colors.ink, modifier = Modifier.weight(1f))
        if (trailing != null) {
            Text(trailing, style = MaterialTheme.typography.bodySmall, color = Ui.colors.greyText)
        }
    }
}

// ---------------------------------------------------------------------------
// Buttons. Every button is a full pill or a circle (design.md section 6.8).
// ---------------------------------------------------------------------------

/** Blue pill for the main action on a screen. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val c = Ui.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                10.dp,
                CircleShape,
                ambientColor = c.primary.copy(alpha = if (c.isDark) 0.25f else 0.35f),
                spotColor = c.primary.copy(alpha = if (c.isDark) 0.25f else 0.35f),
            )
            .clip(CircleShape)
            .background(c.primary)
            .drawBehind { drawPillHighlight() }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp))
    }
}

/** White pill with a hairline border, for secondary actions. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
    contentColor: Color = Ui.colors.ink,
) {
    val c = Ui.colors
    Row(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.hairline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = contentColor, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

/** Black pill (light in dark mode), for a strong action that isn't the blue one. */
@Composable
fun DarkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val c = Ui.colors
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(if (c.isDark) 0.dp else 8.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
            .clip(CircleShape)
            .background(c.inverse)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = c.onInverse, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = c.onInverse, style = MaterialTheme.typography.labelLarge)
    }
}

/** A text-only pill, e.g. "Save" in a top bar. Blue text by default. */
@Composable
fun TextPill(text: String, onClick: () -> Unit, color: Color = Ui.colors.primaryText, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

enum class RoundStyle { Light, Dark, OnSky }

/** 44 dp round icon button: white with a hairline, black ("open" arrow, back) or frosted on the sky. */
@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RoundStyle = RoundStyle.Light,
    size: Dp = 44.dp,
) {
    val c = Ui.colors
    val (fill, border, tint) = when (style) {
        RoundStyle.Light -> Triple(c.surface, c.hairline, c.ink)
        RoundStyle.Dark -> Triple(c.inverse, Color.Transparent, c.onInverse)
        RoundStyle.OnSky -> Triple(
            Color.White.copy(alpha = if (c.isDark) 0.08f else 0.20f),
            Color.White.copy(alpha = if (c.isDark) 0.12f else 0.40f),
            c.onSky,
        )
    }
    Box(
        modifier
            .size(size)
            .then(
                if (style == RoundStyle.Dark && !c.isDark) {
                    Modifier.shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.18f))
                } else {
                    Modifier
                },
            )
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** The round blue "New invoice" button beside the tab bar. */
@Composable
fun NewInvoiceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Box(
        modifier
            .size(64.dp)
            .shadow(14.dp, CircleShape, ambientColor = c.primary.copy(alpha = 0.45f), spotColor = c.primary.copy(alpha = 0.55f))
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF6FA6F3), c.primary, Color(0xFF2F6FD6)),
                    center = Offset.Unspecified,
                ),
            )
            .drawBehind {
                // Soft gloss on the top left.
                drawOval(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent), startY = 0f, endY = size.height * 0.5f),
                    topLeft = Offset(size.width * 0.18f, size.height * 0.08f),
                    size = Size(size.width * 0.5f, size.height * 0.32f),
                )
            }
            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = "New invoice", tint = Color.White, modifier = Modifier.size(30.dp))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPillHighlight() {
    drawRect(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.Transparent),
            endY = size.height * 0.55f,
        ),
    )
}

// ---------------------------------------------------------------------------
// Chips
// ---------------------------------------------------------------------------

/** Filter chip: black when selected (light in dark mode), soft grey otherwise. */
@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val fill by animateColorAsState(if (selected) c.inverse else c.surface, label = "chip fill")
    val text by animateColorAsState(if (selected) c.onInverse else c.ink, label = "chip text")
    Box(
        modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, if (selected) Color.Transparent else c.hairline, CircleShape)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
    }
}

/** A row of pills where exactly one is chosen, like a segmented control. */
@Composable
fun <T> PillChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Row(
        modifier
            .clip(CircleShape)
            .background(c.neutralChip)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val fill by animateColorAsState(if (isSelected) c.surface else Color.Transparent, label = "segment fill")
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(
                        if (isSelected && !c.isDark) {
                            Modifier.shadow(3.dp, CircleShape, ambientColor = ShadowInk.copy(alpha = 0.08f), spotColor = ShadowInk.copy(alpha = 0.12f))
                        } else {
                            Modifier
                        },
                    )
                    .clip(CircleShape)
                    .background(fill)
                    .border(1.dp, if (isSelected && c.isDark) c.hairline else Color.Transparent, CircleShape)
                    .selectable(selected = isSelected, onClick = { onSelect(option) }, role = Role.RadioButton),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label(option),
                    color = if (isSelected) c.ink else c.greyText,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                    maxLines = 1,
                )
            }
        }
    }
}

/** Small outlined pill, e.g. an invoice number on a card. */
@Composable
fun OutlineChip(text: String, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Box(
        modifier
            .clip(CircleShape)
            .border(1.dp, c.hairline, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.greyText, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Floating tab bar (design.md section 6.9)
// ---------------------------------------------------------------------------

class NavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Black floating pill with the tabs, and the round blue New invoice button beside it. */
@Composable
fun FloatingNavBar(items: List<NavItem>, onNewInvoice: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val barShape = CircleShape
        Row(
            Modifier
                .weight(1f)
                .height(64.dp)
                .shadow(if (c.isDark) 0.dp else 18.dp, barShape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.22f))
                .clip(barShape)
                .background(c.tabBar)
                .border(1.dp, if (c.isDark) c.hairline else Color.Transparent, barShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                Row(
                    Modifier
                        .then(if (item.selected) Modifier.weight(1.6f) else Modifier.weight(1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if (item.selected) (if (c.isDark) c.inverse else Color.White) else Color.Transparent)
                        .selectable(selected = item.selected, onClick = item.onClick, role = Role.Tab),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val selectedContent = if (c.isDark) c.onInverse else Color(0xFF000203)
                    Icon(
                        item.icon,
                        contentDescription = if (item.selected) null else item.label,
                        tint = if (item.selected) selectedContent else c.tabBarIcon,
                        modifier = Modifier.size(22.dp),
                    )
                    if (item.selected) {
                        Spacer(Modifier.width(8.dp))
                        Text(item.label, color = selectedContent, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        NewInvoiceButton(onNewInvoice)
    }
}

// ---------------------------------------------------------------------------
// Empty states. A Compose-drawn stand-in for the glossy 3D illustrations.
// ---------------------------------------------------------------------------

/** A page shape with the top right corner cut off for a folded flap. */
private fun foldedCardShape(radius: Float, fold: Float) = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val k = radius * 0.45f
    moveTo(radius, 0f)
    lineTo(w - fold, 0f)
    lineTo(w, fold)
    lineTo(w, h - radius)
    cubicTo(w, h - k, w - k, h, w - radius, h)
    lineTo(radius, h)
    cubicTo(k, h, 0f, h - k, 0f, h - radius)
    lineTo(0f, radius)
    cubicTo(0f, k, k, 0f, radius, 0f)
    close()
}

/** Two soft, glossy pages with a blue folded corner, for empty screens. */
@Composable
fun EmptyIllustration(icon: ImageVector, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Box(modifier.size(width = 230.dp, height = 196.dp)) {
        val backShape = RoundedCornerShape(36.dp)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp, y = 6.dp)
                .size(150.dp)
                .rotate(-9f)
                .then(if (c.isDark) Modifier else Modifier.shadow(10.dp, backShape, ambientColor = ShadowInk.copy(alpha = 0.08f), spotColor = ShadowInk.copy(alpha = 0.10f)))
                .clip(backShape)
                .background(Brush.linearGradient(listOf(c.primarySoft, c.surface)))
                .border(1.dp, c.hairline, backShape),
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-14).dp)
                .size(162.dp)
                .then(foldedCard(radius = 36.dp, fold = 44.dp)),
        ) {
            Column(Modifier.padding(start = 26.dp, top = 30.dp)) {
                Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.size(42.dp))
                Spacer(Modifier.height(20.dp))
                Bar(108)
                Spacer(Modifier.height(11.dp))
                Bar(84)
                Spacer(Modifier.height(11.dp))
                Bar(56)
            }
        }
    }
}

@Composable
private fun foldedCard(radius: Dp, fold: Dp): Modifier {
    val c = Ui.colors
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }
    val foldPx = with(density) { fold.toPx() }
    val shape = foldedCardShape(radiusPx, foldPx)
    return Modifier
        .then(
            if (c.isDark) Modifier else Modifier.shadow(24.dp, shape, ambientColor = c.primary.copy(alpha = 0.18f), spotColor = ShadowInk.copy(alpha = 0.22f)),
        )
        .clip(shape)
        .background(Brush.linearGradient(listOf(c.surfaceRaised, c.surface)))
        .border(1.5.dp, c.hairline, shape)
        .drawWithContent {
            drawContent()
            val w = size.width
            val f = foldPx
            val flap = Path().apply {
                moveTo(w - f, 0f)
                lineTo(w, f)
                cubicTo(w - f * 0.5f, f, w - f, f * 0.5f, w - f, 0f)
                close()
            }
            translate(left = -2f, top = 3f) { drawPath(flap, Color.Black.copy(alpha = 0.08f)) }
            drawPath(
                flap,
                Brush.linearGradient(
                    listOf(Color(0xFF8DBAF6), c.primary, Color(0xFF2F6FD6)),
                    start = Offset(w - f, 0f),
                    end = Offset(w - f * 0.4f, f),
                ),
            )
        }
}

@Composable
private fun Bar(widthDp: Int) {
    Box(
        Modifier
            .width(widthDp.dp)
            .height(9.dp)
            .clip(CircleShape)
            .background(Ui.colors.hairline),
    )
}

/** Illustration, title and a short line of help, centred, with an optional button. */
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
        Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ui.colors.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = Ui.colors.greyText, textAlign = TextAlign.Center)
        if (actionLabel != null) {
            Spacer(Modifier.height(24.dp))
            SecondaryButton(actionLabel, onAction, Modifier.widthIn(min = 180.dp), icon = Icons.Filled.Add)
        }
    }
}

/** Keeps a border-only stroke handy for Material components that take one. */
@Composable
fun hairlineBorder(): BorderStroke = BorderStroke(1.dp, Ui.colors.hairline)
