package com.ovais.sketchpad.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.ovais.sketch_pad.pad.data.ActiveStroke
import java.io.File

fun exportImage(
    strokes: List<ActiveStroke>,
    width: Int = 2048,
    height: Int = 2048
): Bitmap {
    val bitmap = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bitmap)

    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    if (strokes.isNotEmpty()) {
        val bounds = getBounds(strokes)
        val padding = 0.9f
        val scaleX = width / bounds.width
        val scaleY = height / bounds.height
        val scale = minOf(scaleX, scaleY) * padding

        val dx = (width - bounds.width * scale) / 2f
        val dy = (height - bounds.height * scale) / 2f

        canvas.withTranslation(dx - bounds.minX * scale, dy - bounds.minY * scale) {
            withScale(scale, scale) {
                strokes.forEach { stroke ->
                    // Fix: Correctly convert the stored color Long back to a Compose Color and then to ARGB Int
                    paint.color = Color(stroke.color.toULong()).toArgb()
                    paint.strokeWidth = stroke.strokeWidth

                    val path = Path()
                    if (stroke.points.isNotEmpty()) {
                        path.moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            val prev = stroke.points[i - 1]
                            val curr = stroke.points[i]
                            val midX = (prev.x + curr.x) / 2f
                            val midY = (prev.y + curr.y) / 2f
                            path.quadTo(prev.x, prev.y, midX, midY)
                        }
                        path.lineTo(stroke.points.last().x, stroke.points.last().y)
                    }
                    drawPath(path, paint)
                }
            }
        }
    }

    return bitmap
}

private fun exportPdf(
    strokes: List<ActiveStroke>,
    file: File
) {
    val pdf = PdfDocument()

    // A4 size approximation
    val pageInfo = PdfDocument.PageInfo.Builder(2480, 3508, 1).create()
    val page = pdf.startPage(pageInfo)
    val canvas = page.canvas

    // background
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    if (strokes.isNotEmpty()) {
        val bounds = getBounds(strokes)
        val padding = 0.9f
        val scaleX = pageInfo.pageWidth / bounds.width
        val scaleY = pageInfo.pageHeight / bounds.height
        val scale = minOf(scaleX, scaleY) * padding

        val dx = (pageInfo.pageWidth - bounds.width * scale) / 2f
        val dy = (pageInfo.pageHeight - bounds.height * scale) / 2f

        canvas.withTranslation(dx - bounds.minX * scale, dy - bounds.minY * scale) {
            withScale(scale, scale) {
                strokes.forEach { stroke ->
                    // Fix: Correctly convert the stored color Long back to a Compose Color and then to ARGB Int
                    paint.color = Color(stroke.color.toULong()).toArgb()
                    paint.strokeWidth = stroke.strokeWidth

                    val path = Path()
                    if (stroke.points.isNotEmpty()) {
                        path.moveTo(stroke.points.first().x, stroke.points.first().y)
                        for (i in 1 until stroke.points.size) {
                            val prev = stroke.points[i - 1]
                            val curr = stroke.points[i]
                            val midX = (prev.x + curr.x) / 2f
                            val midY = (prev.y + curr.y) / 2f
                            path.quadTo(prev.x, prev.y, midX, midY)
                        }
                        path.lineTo(stroke.points.last().x, stroke.points.last().y)
                    }
                    drawPath(path, paint)
                }
            }
        }
    }

    pdf.finishPage(page)

    file.outputStream().use {
        pdf.writeTo(it)
    }

    pdf.close()
}

data class Bounds(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float
) {
    val width get() = (maxX - minX).coerceAtLeast(1f)
    val height get() = (maxY - minY).coerceAtLeast(1f)
}

private fun getBounds(strokes: List<ActiveStroke>): Bounds {
    val all = strokes.flatMap { it.points }
    if (all.isEmpty()) return Bounds(0f, 0f, 1f, 1f)

    val minX = all.minOf { it.x }
    val minY = all.minOf { it.y }
    val maxX = all.maxOf { it.x }
    val maxY = all.maxOf { it.y }

    return Bounds(minX, minY, maxX, maxY)
}

fun saveImageToGallery(context: Context, bitmap: Bitmap) {
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "sketch_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SketchPad")
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}

private fun savePdfToDownloads(context: Context, file: File) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        val resolver = context.contentResolver

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "sketch_${System.currentTimeMillis()}.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/SketchPad")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                file.inputStream().copyTo(out)
            }
        }

    } else {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        val destFile = File(downloadsDir, "sketch_${System.currentTimeMillis()}.pdf")

        file.copyTo(destFile, overwrite = true)
    }
}

fun exportAndSavePdf(
    context: Context,
    strokes: List<ActiveStroke>
) {
    val tempFile = File(context.cacheDir, "temp_sketch.pdf")

    exportPdf(strokes, tempFile)

    savePdfToDownloads(context, tempFile)

    tempFile.delete()
}
