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

    /** Arrow pointing up and right: "open this" on cards. */
    val ArrowUpRight: ImageVector = lineIcon("ArrowUpRight", "M7 17L17 7", "M8.5 7h8.5v8.5")

    /** Arrow pointing left: back. */
    val Back: ImageVector = lineIcon("Back", "M19 12H5", "M11 6l-6 6l6 6")

    val Plus: ImageVector = lineIcon("Plus", "M12 5v14", "M5 12h14")

    val Trash: ImageVector = lineIcon(
        "Trash",
        "M4.5 7h15",
        "M9.5 7v-1.5a1.5 1.5 0 0 1 1.5 -1.5h2a1.5 1.5 0 0 1 1.5 1.5v1.5",
        "M6.5 7l0.8 11.2a2 2 0 0 0 2 1.8h5.4a2 2 0 0 0 2 -1.8l0.8 -11.2",
        "M10.25 11v5",
        "M13.75 11v5",
    )

    /** Paper plane: send / share the PDF. */
    val Send: ImageVector = lineIcon(
        "Send",
        "M20.5 3.5l-17 7l7 3l3 7z",
        "M20.5 3.5l-10 10",
    )

    val Pencil: ImageVector = lineIcon(
        "Pencil",
        "M4 20h4l10.5 -10.5a2.83 2.83 0 0 0 -4 -4l-10.5 10.5v4",
        "M13.5 6.5l4 4",
    )

    /** Three dots: more options. */
    val More: ImageVector = lineIcon("More", "M5.5 12h0.01", "M12 12h0.01", "M18.5 12h0.01", width = 3f)

    /** A coin with a tick: record a payment. */
    val Payment: ImageVector = lineIcon(
        "Payment",
        "M3.5 12a8.5 8.5 0 1 0 17 0a8.5 8.5 0 1 0 -17 0z",
        "M8.5 12.2l2.4 2.4l4.6 -4.8",
    )

    /** Gear: the Settings tab. */
    val Settings: ImageVector = lineIcon(
        "Settings",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0 -6 0z",
        "M10.3 3.6a1.7 1.7 0 0 1 3.4 0a1.7 1.7 0 0 0 2.6 1.1a1.7 1.7 0 0 1 2.4 2.4a1.7 1.7 0 0 0 1.1 2.6a1.7 1.7 0 0 1 0 3.4a1.7 1.7 0 0 0 -1.1 2.6a1.7 1.7 0 0 1 -2.4 2.4a1.7 1.7 0 0 0 -2.6 1.1a1.7 1.7 0 0 1 -3.4 0a1.7 1.7 0 0 0 -2.6 -1.1a1.7 1.7 0 0 1 -2.4 -2.4a1.7 1.7 0 0 0 -1.1 -2.6a1.7 1.7 0 0 1 0 -3.4a1.7 1.7 0 0 0 1.1 -2.6a1.7 1.7 0 0 1 2.4 -2.4a1.7 1.7 0 0 0 2.6 -1.1z",
    )

    private fun lineIcon(name: String, vararg paths: String, width: Float = 1.75f): ImageVector {
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
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        return builder.build()
    }
}
