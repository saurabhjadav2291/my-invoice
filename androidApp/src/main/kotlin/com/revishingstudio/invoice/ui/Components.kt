package com.revishingstudio.invoice.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/** Screen side margin from design.md section 4.1. */
val ScreenMargin = 20.dp

/** Corner radius of text fields and form buttons (design.md section 4.2). */
val FormCorner = 8.dp
val FormShape = RoundedCornerShape(FormCorner)

private val ShadowInk = Color(0xFF0B2A5B)
private val DeepBlue = Color(0xFF2F6FD6)
private val DarkBlue = Color(0xFF1F55B0)
private val GemDark = Color(0xFF2559B8)
private val LightBlue = Color(0xFF8DBAF6)

// ---------------------------------------------------------------------------
// Liquid glass (design.md section 5.3)
// ---------------------------------------------------------------------------

enum class Glass(val tint: Color) {
    /** Cards, fields, the hero card, list rows. */
    Regular(Color.White.copy(alpha = 0.68f)),

    /** Things that sit on the blue sky: header buttons, the avatar ring. */
    Clear(Color.White.copy(alpha = 0.22f)),

    /** The tab bar and the dark round "open" buttons. */
    Smoked(Color(0xFF000203).copy(alpha = 0.62f)),

    /** Menus and dialogs float over busy content, so they are nearly opaque. */
    Overlay(Color.White.copy(alpha = 0.94f)),
}

/**
 * The glass look: tint, a specular rim (bright top left, faint bottom right), a thin inner
 * highlight along the top edge, and a soft shadow. Blur comes from [hazeGlass] where real
 * content scrolls behind the surface; elsewhere the backdrop is the plain sky or canvas.
 */
fun Modifier.glass(
    shape: Shape,
    glass: Glass = Glass.Regular,
    shadow: Boolean = true,
    tint: Color = glass.tint,
): Modifier = this
    .then(
        if (shadow) {
            Modifier.shadow(
                if (glass == Glass.Smoked) 10.dp else 14.dp,
                shape,
                ambientColor = ShadowInk.copy(alpha = 0.08f),
                spotColor = ShadowInk.copy(alpha = if (glass == Glass.Smoked) 0.22f else 0.10f),
            )
        } else {
            Modifier
        },
    )
    .clip(shape)
    .background(tint)
    .drawWithContent {
        drawContent()
        drawInnerHighlight(glass)
    }
    .border(
        1.dp,
        Brush.linearGradient(
            if (glass == Glass.Smoked) {
                listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0.06f))
            } else {
                listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.15f))
            },
        ),
        shape,
    )

private fun DrawScope.drawInnerHighlight(glass: Glass) {
    val alpha = if (glass == Glass.Smoked) 0.22f else 0.60f
    drawLine(
        Brush.horizontalGradient(
            0f to Color.Transparent,
            0.2f to Color.White.copy(alpha = alpha),
            0.8f to Color.White.copy(alpha = alpha),
            1f to Color.Transparent,
        ),
        start = Offset(0f, 1.5.dp.toPx()),
        end = Offset(size.width, 1.5.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
    )
}

/** Real backdrop blur behind a surface (Android 12+; a plain tint on older phones). */
fun Modifier.hazeGlass(state: HazeState, tint: Color, blur: Dp = 24.dp): Modifier =
    hazeEffect(state) {
        blurRadius = blur
        tints = listOf(HazeTint(tint))
        fallbackTint = HazeTint(tint.copy(alpha = maxOf(tint.alpha, 0.92f)))
        noiseFactor = 0f
    }

/**
 * Progressive blur band (design.md section 5.5). [blurAtTop] true for the band over the
 * status bar (strongest at the top edge), false for the band behind the bottom navigation.
 */
fun Modifier.progressiveBlur(state: HazeState, tint: Color, blurAtTop: Boolean, blur: Dp): Modifier =
    hazeEffect(state) {
        blurRadius = blur
        noiseFactor = 0f
        tints = listOf(
            HazeTint(
                Brush.verticalGradient(
                    if (blurAtTop) listOf(tint, Color.Transparent) else listOf(Color.Transparent, tint),
                ),
            ),
        )
        fallbackTint = HazeTint(
            Brush.verticalGradient(
                if (blurAtTop) listOf(tint, Color.Transparent) else listOf(Color.Transparent, tint),
            ),
        )
        progressive = if (blurAtTop) {
            HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
        } else {
            HazeProgressive.verticalGradient(startIntensity = 0f, endIntensity = 1f)
        }
    }

/** The app-wide blur source, set up in InvoiceAppUi. Null where no blur source is active. */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

// ---------------------------------------------------------------------------
// Backgrounds and headers
// ---------------------------------------------------------------------------

/**
 * The page behind every screen: the canvas colour, with the blue sky gradient across
 * the top ~40% when [sky] is true (top-level screens and invoice details).
 */
@Composable
fun AppBackground(sky: Boolean, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = Ui.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(c.background),
    ) {
        if (sky) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(maxHeight * 0.42f)
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
 * short line under it, with round buttons on the right. It scrolls with the content.
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
            Text(title, style = MaterialTheme.typography.headlineMedium, color = c.onSky, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

/** 44 dp circle with the studio logo, or the first letter of a name, in clear glass. */
@Composable
fun Avatar(logo: ImageBitmap?, name: String, onSky: Boolean = true, size: Dp = 44.dp) {
    val c = Ui.colors
    Box(
        Modifier
            .size(size)
            .then(
                when {
                    logo != null -> Modifier.glass(CircleShape, Glass.Regular, shadow = false, tint = Color.White)
                    onSky -> Modifier.glass(CircleShape, Glass.Clear, shadow = false)
                    else -> Modifier.glass(CircleShape, Glass.Regular, shadow = false, tint = c.primarySoft)
                },
            ),
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

// ---------------------------------------------------------------------------
// Cards
// ---------------------------------------------------------------------------

/** Glass card with large rounded corners. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(120), label = "card press")
    Column(
        modifier
            .fillMaxWidth()
            .scale(scale)
            .glass(shape, tint = if (pressed) Color.White.copy(alpha = 0.78f) else Glass.Regular.tint)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** The larger glass card that sits on the sky gradient, holding a screen's headline numbers. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(28.dp), tint = Color.White.copy(alpha = 0.74f))
            .padding(20.dp),
        content = content,
    )
}

/** Heading above a group of cards, with an optional note on the right. */
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
// Buttons (design.md section 6.8). Text buttons have 8 dp corners.
// ---------------------------------------------------------------------------

private val ButtonLabel = TextStyle(fontSize = 16.sp)

/** Skeuomorphic blue button for the main action on a screen (design.md section 5.4). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shadow by animateDpAsState(if (pressed) 4.dp else 12.dp, tween(100), label = "button shadow")
    val drop by animateDpAsState(if (pressed) 1.dp else 0.dp, tween(100), label = "button drop")
    Row(
        modifier
            .fillMaxWidth()
            .offset(y = drop)
            .height(height)
            .shadow(shadow, FormShape, ambientColor = DeepBlue.copy(alpha = 0.40f), spotColor = DeepBlue.copy(alpha = 0.45f))
            .clip(FormShape)
            .drawBehind { drawSkeuoBlue(pressed) }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = Color.White,
            style = MaterialTheme.typography.titleSmall.merge(ButtonLabel).copy(
                shadow = androidx.compose.ui.graphics.Shadow(DarkBlue.copy(alpha = 0.45f), Offset(0f, 2f), 0f),
            ),
        )
    }
}

/** Fill, gloss and bevel of the skeuomorphic blue button. */
private fun DrawScope.drawSkeuoBlue(pressed: Boolean) {
    val r = CornerRadius(FormCorner.toPx())
    val stops = if (pressed) {
        listOf(DeepBlue, Color(0xFF438BEF), Color(0xFF5A9BF2))
    } else {
        listOf(Color(0xFF5A9BF2), Color(0xFF438BEF), DeepBlue)
    }
    drawRoundRect(Brush.verticalGradient(stops), cornerRadius = r)
    if (!pressed) {
        // Gloss across the top half.
        drawRoundRect(
            Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent), endY = size.height * 0.5f),
            size = Size(size.width, size.height * 0.5f),
            cornerRadius = r,
        )
    } else {
        // Inner shadow at the top when pressed in.
        drawRect(Brush.verticalGradient(listOf(ShadowInk.copy(alpha = 0.30f), Color.Transparent), endY = 6.dp.toPx()))
    }
    // Bevel: light top edge, dark bottom edge.
    drawLine(Color.White.copy(alpha = if (pressed) 0.2f else 0.55f), Offset(r.x, 0.75f), Offset(size.width - r.x, 0.75f), 1.dp.toPx())
    drawLine(DarkBlue.copy(alpha = 0.45f), Offset(r.x, size.height - 0.75f), Offset(size.width - r.x, size.height - 0.75f), 1.dp.toPx())
}

/** Glass button with a hairline rim, for secondary actions. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 54.dp,
    contentColor: Color = Ui.colors.ink,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .height(height)
            .scale(if (pressed) 0.98f else 1f)
            .glass(FormShape, tint = if (pressed) Color.White.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.72f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
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

/** Smoked-glass button, for a strong action that isn't the blue one. */
@Composable
fun DarkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .scale(if (pressed) 0.98f else 1f)
            .glass(FormShape, Glass.Smoked, tint = Color(0xFF000203).copy(alpha = if (pressed) 0.75f else 0.86f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/** A text-only button, e.g. "Save" in a top bar or a dialog. Blue text by default. */
@Composable
fun TextPill(text: String, onClick: () -> Unit, color: Color = Ui.colors.primaryText, modifier: Modifier = Modifier) {
    Box(
        modifier
            .heightIn(min = 40.dp)
            .clip(FormShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

enum class RoundStyle { Light, Dark, OnSky }

/** Round glass icon button: regular glass, smoked ("open" arrow, back) or clear on the sky. */
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
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val (glass, tint) = when (style) {
        RoundStyle.Light -> Glass.Regular to c.ink
        RoundStyle.Dark -> Glass.Smoked to Color.White
        RoundStyle.OnSky -> Glass.Clear to c.onSky
    }
    val fill = when (style) {
        RoundStyle.Dark -> Color(0xFF000203).copy(alpha = 0.86f)
        else -> glass.tint
    }
    Box(
        modifier
            .size(size)
            .scale(if (pressed) 0.94f else 1f)
            .glass(CircleShape, glass, shadow = style != RoundStyle.OnSky, tint = fill)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.45f))
    }
}

/** The glossy blue gem "+" button beside the tab bar (design.md section 5.4). */
@Composable
fun NewInvoiceButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(100), label = "gem press")
    Box(
        modifier
            .size(64.dp)
            .scale(scale)
            .shadow(if (pressed) 8.dp else 16.dp, CircleShape, ambientColor = Color(0xFF438BEF).copy(alpha = 0.45f), spotColor = Color(0xFF438BEF).copy(alpha = 0.55f))
            .clip(CircleShape)
            .drawBehind { drawGem(pressed) }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        // Engraved shadow under the "+".
        Icon(Icons.Filled.Add, contentDescription = null, tint = DarkBlue.copy(alpha = 0.5f), modifier = Modifier.offset(y = 1.dp).size(32.dp))
        Icon(Icons.Filled.Add, contentDescription = "New invoice", tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

private fun DrawScope.drawGem(pressed: Boolean) {
    val w = size.width
    val h = size.height
    drawCircle(
        Brush.radialGradient(
            0f to LightBlue,
            0.55f to Color(0xFF438BEF),
            1f to GemDark,
            center = Offset(w * 0.32f, h * 0.28f),
            radius = w * 0.95f,
        ),
    )
    // Bounce light inside the bottom edge.
    drawOval(
        Brush.verticalGradient(listOf(Color.Transparent, LightBlue.copy(alpha = 0.45f)), startY = h * 0.7f, endY = h),
        topLeft = Offset(w * 0.18f, h * 0.68f),
        size = Size(w * 0.64f, h * 0.30f),
    )
    // Specular highlight across the top.
    drawOval(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = if (pressed) 0.45f else 0.70f), Color.Transparent),
            startY = h * 0.04f,
            endY = h * 0.42f,
        ),
        topLeft = Offset(w * 0.16f, h * 0.05f),
        size = Size(w * 0.68f, h * 0.38f),
    )
    // Rims: dark outside, light just inside.
    drawCircle(GemDark.copy(alpha = 0.6f), radius = w / 2 - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
    drawCircle(Color.White.copy(alpha = 0.35f), radius = w / 2 - 1.5.dp.toPx(), style = Stroke(1.dp.toPx()))
}

// ---------------------------------------------------------------------------
// Chips
// ---------------------------------------------------------------------------

/** Filter chip: black when selected, glass otherwise. */
@Composable
fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Ui.colors
    val text by animateColorAsState(if (selected) c.onInverse else c.ink, label = "chip text")
    Box(
        modifier
            .height(36.dp)
            .glass(
                CircleShape,
                if (selected) Glass.Smoked else Glass.Regular,
                shadow = selected,
                tint = if (selected) Color(0xFF000203).copy(alpha = 0.88f) else Glass.Regular.tint,
            )
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
            Box(
                Modifier
                    .weight(1f)
                    .height(36.dp)
                    .then(if (isSelected) Modifier.glass(CircleShape, tint = Color.White) else Modifier.clip(CircleShape))
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

/** Small outlined pill, e.g. an invoice number on a card. A flat tint, not blurred. */
@Composable
fun OutlineChip(text: String, modifier: Modifier = Modifier) {
    val c = Ui.colors
    Box(
        modifier
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.5f))
            .border(1.dp, c.hairline, CircleShape)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.greyText, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------
// Floating tab bar (design.md sections 5.4 and 6.9)
// ---------------------------------------------------------------------------

class NavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/**
 * Smoked-glass tab bar with skeuomorphic depth and a raised white key for the selected tab,
 * plus the blue gem + button beside it. [haze] blurs the content scrolling underneath.
 */
@Composable
fun FloatingNavBar(items: List<NavItem>, onNewInvoice: () -> Unit, haze: HazeState?, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(start = ScreenMargin, end = ScreenMargin, top = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .height(64.dp)
                .shadow(18.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.20f), spotColor = Color.Black.copy(alpha = 0.30f))
                .clip(CircleShape)
                .then(if (haze != null) Modifier.hazeGlass(haze, Glass.Smoked.tint, blur = 20.dp) else Modifier.background(Color(0xFF000203).copy(alpha = 0.9f)))
                .drawWithContent {
                    drawContent()
                    // Bevel: light top edge, inner shadow along the bottom.
                    drawLine(
                        Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.15f to Color.White.copy(alpha = 0.22f),
                            0.85f to Color.White.copy(alpha = 0.22f),
                            1f to Color.Transparent,
                        ),
                        Offset(0f, 1.dp.toPx()),
                        Offset(size.width, 1.dp.toPx()),
                        1.dp.toPx(),
                    )
                    drawRect(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), startY = size.height - 6.dp.toPx()),
                    )
                }
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.White.copy(alpha = 0.04f))), CircleShape)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { item ->
                Row(
                    Modifier
                        .then(if (item.selected) Modifier.weight(1.6f) else Modifier.weight(1f))
                        .fillMaxHeight()
                        .then(
                            if (item.selected) {
                                Modifier
                                    .shadow(4.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.25f), spotColor = Color.Black.copy(alpha = 0.30f))
                                    .clip(CircleShape)
                                    .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE9EEF3))))
                                    .drawWithContent {
                                        drawContent()
                                        drawLine(
                                            Color(0xFFC9D3DD),
                                            Offset(size.height / 2, size.height - 0.5.dp.toPx()),
                                            Offset(size.width - size.height / 2, size.height - 0.5.dp.toPx()),
                                            1.dp.toPx(),
                                        )
                                    }
                            } else {
                                Modifier.clip(CircleShape)
                            },
                        )
                        .selectable(selected = item.selected, onClick = item.onClick, role = Role.Tab),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.selected) {
                        Icon(item.icon, contentDescription = null, tint = Color(0xFF000203), modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(item.label, color = Color(0xFF000203), style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    } else {
                        // Engraved icon: a dark copy 1 dp below the light one.
                        Box {
                            Icon(item.icon, contentDescription = null, tint = Color.Black.copy(alpha = 0.4f), modifier = Modifier.offset(y = 1.dp).size(22.dp))
                            Icon(item.icon, contentDescription = item.label, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.width(10.dp))
        NewInvoiceButton(onNewInvoice)
    }
}

// ---------------------------------------------------------------------------
// Empty states (design.md section 7.1). No background block.
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

/** Two soft glass pages with a blue folded corner: the stand-in for the 3D illustrations. */
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
                .glass(backShape, tint = c.primarySoft.copy(alpha = 0.8f)),
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
        .shadow(24.dp, shape, ambientColor = c.primary.copy(alpha = 0.18f), spotColor = ShadowInk.copy(alpha = 0.22f))
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0.75f))))
        .border(1.5.dp, Brush.linearGradient(listOf(Color.White, c.hairline)), shape)
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
                    listOf(LightBlue, c.primary, DeepBlue),
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

@Suppress("unused")
private fun DrawScope.clipTo(path: Path, block: DrawScope.() -> Unit) = clipPath(path) { block() }
