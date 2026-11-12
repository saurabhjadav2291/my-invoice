package com.revishingstudio.invoice.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.revishingstudio.invoice.pdf.InvoicePdfExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Shows an invoice PDF inside the app, page by page, with Share in the top bar.
 * Pinch to zoom, drag to look around while zoomed, double tap to zoom in or back out.
 */
@Composable
fun PdfPreviewScreen(path: String, title: String, nav: Navigator, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val actions = LocalActions.current
    val file = remember(path) { File(path) }
    var pages by remember { mutableStateOf<List<ImageBitmap>?>(null) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(path) {
        // Sharp enough to zoom in on small print: twice the screen width, within memory limits.
        val targetWidth = (context.resources.displayMetrics.widthPixels * 2).coerceIn(1200, 2400)
        val rendered = withContext(Dispatchers.IO) { runCatching { renderPages(file, targetWidth) } }
        rendered.onSuccess { pages = it }.onFailure { failed = true }
    }

    InnerScaffold(
        modifier = modifier,
        topBar = {
            BackTopBar(title, nav::back) {
                if (pages != null) {
                    AppButton("Share", { InvoicePdfExporter.share(context, file) }, variant = ButtonVariant.Link, icon = AppIcons.Send)
                }
            }
        },
    ) { padding ->
        val list = pages
        when {
            failed -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Could not show this PDF", style = MaterialTheme.typography.bodyLarge, color = Ui.colors.greyText)
            }
            list == null -> Unit
            else -> ZoomablePages(list, Modifier.fillMaxSize().padding(padding))
        }
    }
    LaunchedEffect(failed) { if (failed) actions.message("Could not show this PDF") }
}

@Composable
private fun ZoomablePages(pages: List<ImageBitmap>, modifier: Modifier) {
    val c = Ui.colors
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(o: Offset, s: Float): Offset {
        val maxX = size.width * (s - 1f) / 2f
        val maxY = size.height * (s - 1f) / 2f
        return Offset(o.x.coerceIn(-maxX, maxX), o.y.coerceIn(-maxY, maxY))
    }

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { tap ->
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        offset = clamp((center - tap) * 1.5f, scale)
                    }
                })
            }
            .pointerInput(Unit) {
                // Pinch always zooms. A one-finger drag pans only while zoomed in, so at normal
                // size it is left to the page scroll.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val fingers = event.changes.count { it.pressed }
                        if (fingers >= 2 || scale > 1f) {
                            val zoom = event.calculateZoom()
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            offset = clamp(offset + event.calculatePan(), newScale)
                            scale = newScale
                            if (newScale == 1f) offset = Offset.Zero
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .verticalScroll(rememberScrollState(), enabled = scale == 1f)
                .padding(horizontal = ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            pages.forEachIndexed { index, page ->
                Image(
                    page,
                    contentDescription = "Page ${index + 1}",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(page.width.toFloat() / page.height)
                        .shadow(6.dp, SmoothShape(4.dp), ambientColor = c.ink.copy(alpha = 0.12f), spotColor = c.ink.copy(alpha = 0.12f))
                        .background(Color.White)
                        .border(1.dp, c.hairline, SmoothShape(4.dp)),
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

/** Draws each page of [file] on white, [width] pixels wide. */
private fun renderPages(file: File, width: Int): List<ImageBitmap> =
    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { renderer ->
            (0 until renderer.pageCount).map { i ->
                renderer.openPage(i).use { page ->
                    val height = (width.toFloat() * page.height / page.width).toInt()
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmap.asImageBitmap()
                }
            }
        }
    }
