package com.revishingstudio.invoice.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Thin, rounded line icons in the style of the design. Drawn on a 24 x 24 grid. */
object AppIcons {

    /** Rounded square with lines: the Invoices tab. */
    val Invoices: ImageVector = lineIcon(
        "Invoices",
        "M7 3.5h10a3.5 3.5 0 0 1 3.5 3.5v10a3.5 3.5 0 0 1 -3.5 3.5h-10a3.5 3.5 0 0 1 -3.5 -3.5v-10a3.5 3.5 0 0 1 3.5 -3.5z",
        "M8 8.5h8",
        "M8 12h8",
        "M8 15.5h5",
    )

    /** A page with a folded corner and text lines. */
    val Document: ImageVector = lineIcon(
        "Document",
        "M14 3h-6.5a2.5 2.5 0 0 0 -2.5 2.5v13a2.5 2.5 0 0 0 2.5 2.5h9a2.5 2.5 0 0 0 2.5 -2.5v-10.5z",
        "M14 3v4a1.5 1.5 0 0 0 1.5 1.5h3.5",
        "M8.75 9h2.5",
        "M8.75 12.75h6.5",
        "M8.75 16.25h6.5",
    )

    /** Two people: the Clients tab. */
    val Clients: ImageVector = lineIcon(
        "Clients",
        "M5.5 7.5a3.75 3.75 0 1 0 7.5 0a3.75 3.75 0 1 0 -7.5 0z",
        "M2.5 20.5v-1a4.5 4.5 0 0 1 4.5 -4.5h4.5a4.5 4.5 0 0 1 4.5 4.5v1",
        "M15.5 3.9a3.75 3.75 0 0 1 0 7.2",
        "M18.5 15.2a4.5 4.5 0 0 1 3 4.3v1",
    )

    private fun lineIcon(name: String, vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        paths.forEach { d ->
            builder.addPath(
                pathData = addPathNodes(d),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
