package com.revishingstudio.invoice.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val WarmShadow = Color(0xFFB4643C)

/** Warm white page with soft peach and lilac glows, behind every screen. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Canvas)
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFE1CF), Color(0x00FFE1CF)),
                        center = Offset(size.width * 1.05f, size.height * 0.0f),
                        radius = size.width * 1.15f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFF4EDFA), Color(0x00F4EDFA)),
                        center = Offset(-size.width * 0.15f, size.height * 0.42f),
                        radius = size.width * 0.95f,
                    ),
                )
                drawRect(
                    Brush.radialGradient(
                        listOf(Color(0xFFFFEADF), Color(0x00FFEADF)),
                        center = Offset(size.width * 1.0f, size.height * 0.88f),
                        radius = size.width * 0.9f,
                    ),
                )
            },
        content = content,
    )
}

/** Big bold page title used on the three main tabs. */
@Composable
fun LargeTitleBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 12.dp, top = 28.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.weight(1f))
        actions()
    }
}

/** Frosted white card with a soft warm shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(12.dp, shape, ambientColor = WarmShadow.copy(alpha = 0.10f), spotColor = WarmShadow.copy(alpha = 0.16f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFFCFA))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Palette.Hairline)), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** Glossy orange card for the headline number on a screen. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(18.dp, shape, ambientColor = Palette.Orange.copy(alpha = 0.35f), spotColor = Palette.Orange.copy(alpha = 0.45f))
            .clip(shape)
            .drawBehind {
                drawRect(
                    Brush.linearGradient(
                        listOf(Palette.OrangeLight, Palette.Orange, Palette.OrangeDeep),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
                // Soft gloss across the top.
                drawOval(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.30f), Color.Transparent), endY = size.height * 0.6f),
                    topLeft = Offset(-size.width * 0.2f, -size.height * 0.55f),
                    size = Size(size.width * 1.1f, size.height * 1.1f),
                )
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.05f))), shape)
            .padding(20.dp),
        content = content,
    )
}

/** Round glossy orange "add" button. */
@Composable
fun GlossyFab(onClick: () -> Unit, contentDescription: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(64.dp)
            .shadow(16.dp, CircleShape, ambientColor = Palette.Orange, spotColor = Palette.Orange)
            .clip(CircleShape)
            .drawBehind { drawGlossyOrange() }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Filled.Add, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(32.dp))
    }
}

/** Glossy orange pill button for the main action on a screen. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(12.dp, CircleShape, ambientColor = Palette.Orange.copy(alpha = 0.5f), spotColor = Palette.Orange.copy(alpha = 0.6f))
            .clip(CircleShape)
            .drawBehind { drawGlossyOrange() }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium)
    }
}

private fun DrawScope.drawGlossyOrange() {
    drawRect(
        Brush.radialGradient(
            listOf(Palette.OrangeLight, Palette.Orange, Palette.OrangeDeep),
            center = Offset(size.width * 0.35f, size.height * 0.25f),
            radius = maxOf(size.width, size.height) * 0.9f,
        ),
    )
    // Highlight streak, top left.
    val w = size.minDimension
    rotate(-35f, pivot = Offset(w * 0.36f, size.height * 0.26f)) {
        drawOval(
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.75f), Color.White.copy(alpha = 0f)),
                startY = size.height * 0.12f,
                endY = size.height * 0.34f,
            ),
            topLeft = Offset(w * 0.14f, size.height * 0.12f),
            size = Size(w * 0.42f, size.height * 0.22f),
        )
    }
    // Light rim at the bottom.
    drawRect(
        Brush.verticalGradient(
            listOf(Color.Transparent, Color.White.copy(alpha = 0.18f)),
            startY = size.height * 0.65f,
            endY = size.height,
        ),
    )
}

class NavItem(val label: String, val icon: ImageVector, val selected: Boolean, val onClick: () -> Unit)

/** Floating frosted pill with the main tabs. The selected tab glows peach. */
@Composable
fun FloatingNavBar(items: List<NavItem>, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(34.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 10.dp)
            .fillMaxWidth()
            .height(76.dp)
            .shadow(22.dp, shape, ambientColor = WarmShadow.copy(alpha = 0.20f), spotColor = WarmShadow.copy(alpha = 0.28f))
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFAFFFFFF), Color(0xF5FFF9F5))))
            .border(1.dp, Brush.verticalGradient(listOf(Color.White, Palette.Hairline)), shape)
            .padding(7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items.forEach { item ->
            val pill = RoundedCornerShape(28.dp)
            val tint by animateColorAsState(if (item.selected) Palette.Orange else Color(0xFF4A4C58), label = "tab tint")
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(pill)
                    .then(
                        if (item.selected) {
                            Modifier
                                .background(Brush.verticalGradient(listOf(Color(0xFFFFEDE3), Color(0xFFFFDCC9))))
                                .border(1.dp, Color.White.copy(alpha = 0.9f), pill)
                        } else {
                            Modifier
                        },
                    )
                    .selectable(selected = item.selected, onClick = item.onClick, role = Role.Tab),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(item.icon, contentDescription = null, tint = tint, modifier = Modifier.size(26.dp))
                Spacer(Modifier.height(3.dp))
                Text(item.label, color = tint, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

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

/** Glassy 3D page illustration for empty screens. */
@Composable
fun EmptyIllustration(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.size(width = 230.dp, height = 196.dp)) {
        val backShape = RoundedCornerShape(36.dp)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = 14.dp, y = 6.dp)
                .size(150.dp)
                .rotate(-9f)
                .shadow(10.dp, backShape, ambientColor = WarmShadow.copy(alpha = 0.10f), spotColor = WarmShadow.copy(alpha = 0.12f))
                .clip(backShape)
                .background(Brush.linearGradient(listOf(Color(0xFFF8F4FB), Color(0xFFFFFBF8))))
                .border(1.dp, Brush.linearGradient(listOf(Color.White, Color(0xFFEDE6EF))), backShape),
        )

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-14).dp)
                .size(162.dp)
                .then(foldedCard(radius = 36.dp, fold = 44.dp)),
        ) {
            Column(Modifier.padding(start = 26.dp, top = 30.dp)) {
                Icon(icon, contentDescription = null, tint = Palette.Orange, modifier = Modifier.size(42.dp))
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
    val density = LocalDensity.current
    val radiusPx = with(density) { radius.toPx() }
    val foldPx = with(density) { fold.toPx() }
    val shape = foldedCardShape(radiusPx, foldPx)
    return Modifier
        .shadow(26.dp, shape, ambientColor = Palette.Orange.copy(alpha = 0.18f), spotColor = WarmShadow.copy(alpha = 0.30f))
        .clip(shape)
        .background(Brush.linearGradient(listOf(Color.White, Color(0xFFF6F3F6))))
        .border(1.5.dp, Brush.linearGradient(listOf(Color.White, Color(0xFFE9E3EA))), shape)
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
                    listOf(Palette.OrangeLight, Palette.Orange, Palette.OrangeDeep),
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
            .background(Color(0xFFC8C6D2)),
    )
}

/** Illustration, bold title and a short line of help, centred on the page. */
@Composable
fun IllustratedEmptyState(title: String, message: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyIllustration(icon)
        Spacer(Modifier.height(32.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(48.dp))
    }
}
