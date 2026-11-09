package com.revishingstudio.invoice.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/** Corner smoothing used on every rounded corner (design.md section 4.2, decided 2026-11-09). */
const val CornerSmoothing = 0.7f

/**
 * A rounded rectangle with smoothed ("squircle") corners, the same geometry as Figma's
 * corner smoothing. [smoothing] 0 is a plain rounded corner, 1 the smoothest; the app uses
 * 70% everywhere. Pills and circles stay [androidx.compose.foundation.shape.CircleShape].
 *
 * The maths follows the published Figma method (the figma-squircle library): each corner is
 * a short circular arc in the middle with a cubic Bezier easing into each straight edge, and
 * the curve may reach up to (1 + smoothing) x radius along each edge.
 */
class SmoothShape(private val radius: Dp, private val smoothing: Float = CornerSmoothing) : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val budget = min(w, h) / 2f
        val r = min(with(density) { radius.toPx() }, budget)
        if (r <= 0f) return Outline.Rectangle(androidx.compose.ui.geometry.Rect(0f, 0f, w, h))
        val c = corner(r, smoothing, budget)
        val d = buildString {
            append("M ${w - c.p} 0 ")
            // Top right
            append("c ${c.a} 0 ${c.a + c.b} 0 ${c.a + c.b + c.c} ${c.d} ")
            append("a ${c.r} ${c.r} 0 0 1 ${c.arc} ${c.arc} ")
            append("c ${c.d} ${c.c} ${c.d} ${c.b + c.c} ${c.d} ${c.a + c.b + c.c} ")
            append("L $w ${h - c.p} ")
            // Bottom right
            append("c 0 ${c.a} 0 ${c.a + c.b} ${-c.d} ${c.a + c.b + c.c} ")
            append("a ${c.r} ${c.r} 0 0 1 ${-c.arc} ${c.arc} ")
            append("c ${-c.c} ${c.d} ${-(c.b + c.c)} ${c.d} ${-(c.a + c.b + c.c)} ${c.d} ")
            append("L ${c.p} $h ")
            // Bottom left
            append("c ${-c.a} 0 ${-(c.a + c.b)} 0 ${-(c.a + c.b + c.c)} ${-c.d} ")
            append("a ${c.r} ${c.r} 0 0 1 ${-c.arc} ${-c.arc} ")
            append("c ${-c.d} ${-c.c} ${-c.d} ${-(c.b + c.c)} ${-c.d} ${-(c.a + c.b + c.c)} ")
            append("L 0 ${c.p} ")
            // Top left
            append("c 0 ${-c.a} 0 ${-(c.a + c.b)} ${c.d} ${-(c.a + c.b + c.c)} ")
            append("a ${c.r} ${c.r} 0 0 1 ${c.arc} ${-c.arc} ")
            append("c ${c.c} ${-c.d} ${c.b + c.c} ${-c.d} ${c.a + c.b + c.c} ${-c.d} ")
            append("Z")
        }
        return Outline.Generic(PathParser().parsePathString(d).toPath())
    }

    override fun equals(other: Any?): Boolean =
        other is SmoothShape && other.radius == radius && other.smoothing == smoothing

    override fun hashCode(): Int = 31 * radius.hashCode() + smoothing.hashCode()

    override fun toString(): String = "SmoothShape($radius, $smoothing)"

    private class Corner(val p: Float, val a: Float, val b: Float, val c: Float, val d: Float, val r: Float, val arc: Float)

    private fun corner(radius: Float, smoothingIn: Float, budget: Float): Corner {
        // Shrink the smoothing when the shape is too small for the full curve.
        val maxSmoothing = budget / radius - 1f
        val smoothing = smoothingIn.coerceAtMost(maxSmoothing).coerceAtLeast(0f)
        val p = min((1f + smoothing) * radius, budget)
        val arcMeasure = 90f * (1f - smoothing)
        val arcSection = (sin(rad(arcMeasure / 2f)) * radius * sqrt(2.0)).toFloat()
        val angleAlpha = (90f - arcMeasure) / 2f
        val p3ToP4 = (radius * tan(rad(angleAlpha / 2f))).toFloat()
        val angleBeta = 45f * smoothing
        val c = (p3ToP4 * cos(rad(angleBeta))).toFloat()
        val d = (c * tan(rad(angleBeta))).toFloat()
        val b = (p - arcSection - c - d) / 3f
        val a = 2f * b
        return Corner(p, a, b, c, d, radius, arcSection)
    }

    private fun rad(degrees: Float): Double = degrees * Math.PI / 180.0
}
