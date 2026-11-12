package com.revishingstudio.invoice.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/**
 * Logos are kept small. A full-size camera photo can be 50 megapixels (about 200 MB
 * once decoded), which is enough to crash the app when the Invoices header or a PDF
 * draws it.
 */
object LogoImages {
    /** Plenty for a logo printed about 6 cm wide and shown at 56 dp. */
    const val MAX_SIDE = 1024

    /**
     * Copies the picked image into the app as a small PNG. Each pick gets its own file,
     * so invoices already sent keep the logo they were sent with.
     */
    fun import(context: Context, uri: Uri): File {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not read that image" }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, MAX_SIDE) }
        val decoded = resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IllegalArgumentException("Could not read that image")
        val scaled = scaleDown(decoded, MAX_SIDE)
        val dir = File(context.filesDir, "logos").apply { mkdirs() }
        val file = File(dir, "logo-${System.currentTimeMillis()}.png")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()
        return file
    }

    /** Reads a saved logo no larger than [maxSide] pixels, even if an old one was saved at full size. */
    fun decode(path: String, maxSide: Int = MAX_SIDE): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxSide) }
        return BitmapFactory.decodeFile(path, options)
    }

    private fun sampleSize(width: Int, height: Int, maxSide: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxSide) sample *= 2
        return sample
    }

    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), true)
    }
}
