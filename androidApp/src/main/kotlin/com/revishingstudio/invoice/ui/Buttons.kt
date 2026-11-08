package com.revishingstudio.invoice.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * The one button component (design.md section 6.3). Every button with a text label in the
 * app is an [AppButton]; screens never style their own.
 *
 * - [ButtonVariant.Primary]: main, high-emphasis action. One per screen.
 * - [ButtonVariant.Secondary]: supporting action, frosted white with a soft lift.
 * - [ButtonVariant.Tertiary]: low-emphasis utility action, no fill until hovered.
 * - [ButtonVariant.Link]: inline text action, blue, optionally with an arrow.
 */
enum class ButtonVariant { Primary, Secondary, Tertiary, Link }

/** Colours of one button state. */
private data class ButtonColors(val fill: Color, val content: Color, val icon: Color, val border: Color)

private val ButtonMotion = tween<Color>(200, easing = AppEasing)

@Composable
private fun buttonColors(variant: ButtonVariant, hovered: Boolean, pressed: Boolean, focused: Boolean, tint: Color?): ButtonColors {
    val c = Ui.colors
    return when (variant) {
        ButtonVariant.Primary -> ButtonColors(
            fill = when {
                pressed -> c.primaryPressed
                hovered -> c.primaryHover
                else -> c.primary
            },
            content = Color.White,
            icon = Color.White,
            border = Color.Transparent,
        )
        ButtonVariant.Secondary -> ButtonColors(
            fill = when {
                pressed -> c.primaryTintPressed
                hovered -> c.primaryTint
                else -> KeyBottom
            },
            content = tint ?: c.ink,
            icon = tint ?: c.primary,
            border = if (focused) c.primary else Color.Transparent,
        )
        ButtonVariant.Tertiary -> ButtonColors(
            fill = when {
                pressed -> c.surfacePressed
                hovered -> c.surfaceRaised
                else -> Color.Transparent
            },
            content = tint ?: c.greyText,
            icon = tint ?: c.greyText,
            border = if (focused) c.primary else Color.Transparent,
        )
        ButtonVariant.Link -> ButtonColors(
            fill = when {
                pressed -> c.primaryTintPressed
                hovered -> c.primaryTint
                else -> Color.Transparent
            },
            content = tint ?: if (hovered || pressed) c.primaryHover else c.primary,
            icon = tint ?: if (hovered || pressed) c.primaryHover else c.primary,
            border = if (focused) c.primary else Color.Transparent,
        )
    }
}

/**
 * Pill-shaped button with Default, Hover, Focused, Pressed and Disabled states.
 * Focus is always a crisp 2 dp blue outline, never a glow. [tint] recolours the label of a
 * secondary, tertiary or link button, e.g. red for "Delete".
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: ButtonVariant = ButtonVariant.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    tint: Color? = null,
) {
    val c = Ui.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val focused by interaction.collectIsFocusedAsState()
    val active = enabled
    val colors = buttonColors(variant, hovered && active, pressed && active, focused && active, tint)
    val fill by animateColorAsState(colors.fill, ButtonMotion, label = "button fill")
    val content by animateColorAsState(colors.content, ButtonMotion, label = "button content")
    val iconColor by animateColorAsState(colors.icon, ButtonMotion, label = "button icon")
    val border by animateColorAsState(colors.border, ButtonMotion, label = "button border")
    val scale by animateFloatAsState(if (pressed && active) 0.98f else 1f, tween(200, easing = AppEasing), label = "button press")
    val lift by animateDpAsState(
        when (variant) {
            ButtonVariant.Primary -> if (hovered && active) 6.dp else 2.dp
            ButtonVariant.Secondary -> if (hovered && active) 4.dp else 2.dp
            else -> 0.dp
        },
        tween(200, easing = AppEasing),
        label = "button lift",
    )
    val isLink = variant == ButtonVariant.Link
    val focusRing = c.primary
    Row(
        modifier
            .height(if (isLink) 40.dp else 48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.4f
            }
            .drawBehind {
                // Primary focus: a 3 dp white gap, then a crisp 2 dp blue ring outside the pill.
                if (focused && enabled && variant == ButtonVariant.Primary) {
                    val gap = 3.dp.toPx()
                    val ring = 2.dp.toPx()
                    val out = gap + ring / 2
                    drawRoundRect(
                        Color.White,
                        topLeft = Offset(-gap / 2, -gap / 2),
                        size = Size(size.width + gap, size.height + gap),
                        cornerRadius = CornerRadius(size.height / 2 + gap / 2),
                        style = Stroke(gap),
                    )
                    drawRoundRect(
                        focusRing,
                        topLeft = Offset(-out, -out),
                        size = Size(size.width + out * 2, size.height + out * 2),
                        cornerRadius = CornerRadius(size.height / 2 + out),
                        style = Stroke(ring),
                    )
                }
            }
            .then(
                if (lift > 0.dp) {
                    Modifier.shadow(lift, CircleShape, ambientColor = c.shadow.copy(alpha = 0.06f), spotColor = c.shadow.copy(alpha = 0.10f))
                } else {
                    Modifier
                },
            )
            .then(
                when (variant) {
                    // The soft key of the active segmented tab: gradient, top shade, white rim.
                    ButtonVariant.Primary -> Modifier.softKey(CircleShape, lerp(fill, Color.White, 0.18f), fill, rimAlpha = 0.55f, shadeAlpha = if (pressed) 0.18f else 0.10f)
                    ButtonVariant.Secondary -> Modifier.softKey(
                        CircleShape,
                        lerp(fill, Color.White, 0.8f),
                        fill,
                        shadeAlpha = if (pressed) 0.14f else 0.08f,
                    )
                    else -> Modifier.background(fill, CircleShape)
                },
            )
            .then(if (focused && enabled && variant != ButtonVariant.Primary) Modifier.border(2.dp, border, CircleShape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = if (isLink) 12.dp else 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = content, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
    }
}
