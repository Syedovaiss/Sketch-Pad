package com.ovais.sketch_pad.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.CanvasSize
import com.ovais.sketch_pad.pad.data.SketchFileType
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for exporting sketches to various file formats.
 */
object SketchExporter {

    /**
     * Exports the given strokes to the specified file type.
     */
    fun exportToFile(
        context: Context,
        strokes: List<ActiveStroke>,
        canvasSize: CanvasSize,
        fileType: SketchFileType,
        fileName: String? = null,
        backgroundColor: Color = Color.White,
        includeGrid: Boolean = false,
        gridSize: Float = 50f,
        gridColor: Color = Color.LightGray.copy(alpha = 0.2f)
    ): File {
        val name = fileName ?: "sketch_${System.currentTimeMillis()}.${fileType.extension}"
        return when (fileType) {
            SketchFileType.PDF -> exportToPdf(context, strokes, canvasSize, name, backgroundColor, includeGrid, gridSize, gridColor)
            SketchFileType.SVG -> exportToSvg(context, strokes, canvasSize, name, backgroundColor)
            SketchFileType.JSON -> exportToJson(context, strokes, name)
            SketchFileType.TEXT -> exportToText(context, strokes, name)
        }
    }

    private fun calculateBounds(strokes: List<ActiveStroke>): Rect {
        if (strokes.isEmpty()) return Rect(0f, 0f, 1080f, 1920f)
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        strokes.forEach { stroke ->
            stroke.points.forEach { pt ->
                minX = min(minX, pt.x)
                minY = min(minY, pt.y)
                maxX = max(maxX, pt.x)
                maxY = max(maxY, pt.y)
            }
        }
        
        // Add padding
        return Rect(minX - 50f, minY - 50f, maxX + 50f, maxY + 50f)
    }

    fun exportToPdf(
        context: Context,
        strokes: List<ActiveStroke>,
        canvasSize: CanvasSize,
        fileName: String,
        backgroundColor: Color = Color.White,
        includeGrid: Boolean = false,
        gridSize: Float = 50f,
        gridColor: Color = Color.LightGray.copy(alpha = 0.2f)
    ): File {
        val pdf = PdfDocument()
        val bounds = if (canvasSize is CanvasSize.Free) calculateBounds(strokes) else null
        val (width, height) = getDimensions(canvasSize, bounds)

        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        // Apply offset if we calculated bounds for Free canvas
        if (bounds != null) {
            canvas.translate(-bounds.left, -bounds.top)
        }

        canvas.drawColor(backgroundColor.toArgb())

        if (includeGrid) {
            val paintGrid = Paint().apply {
                color = gridColor.toArgb()
                strokeWidth = 1f
            }
            val startX = bounds?.left ?: 0f
            val startY = bounds?.top ?: 0f
            val endX = (bounds?.left ?: 0f) + width
            val endY = (bounds?.top ?: 0f) + height
            
            var x = startX - (startX % gridSize)
            while (x < endX) {
                canvas.drawLine(x, startY, x, endY, paintGrid)
                x += gridSize
            }
            var y = startY - (startY % gridSize)
            while (y < endY) {
                canvas.drawLine(startX, y, endX, y, paintGrid)
                y += gridSize
            }
        }

        val paint = Paint().apply {
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        strokes.forEach { stroke ->
            var strokeColor = Color(stroke.color.toULong())
            if (backgroundColor.luminance() > 0.5f && strokeColor.luminance() > 0.9f) {
                strokeColor = Color.Black
            } else if (backgroundColor.luminance() <= 0.5f && strokeColor.luminance() < 0.1f) {
                strokeColor = Color.White
            }

            paint.color = strokeColor.toArgb()
            paint.strokeWidth = stroke.strokeWidth

            val path = android.graphics.Path()
            if (stroke.points.isNotEmpty()) {
                path.moveTo(stroke.points.first().x, stroke.points.first().y)
                for (i in 1 until stroke.points.size) {
                    val prev = stroke.points[i - 1]
                    val curr = stroke.points[i]
                    path.quadTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                }
                path.lineTo(stroke.points.last().x, stroke.points.last().y)
            }
            canvas.drawPath(path, paint)
        }

        pdf.finishPage(page)
        val tempFile = File(context.cacheDir, fileName)
        tempFile.outputStream().use { pdf.writeTo(it) }
        pdf.close()
        return tempFile
    }

    fun exportToSvg(
        context: Context,
        strokes: List<ActiveStroke>,
        canvasSize: CanvasSize,
        fileName: String,
        backgroundColor: Color = Color.White
    ): File {
        val bounds = if (canvasSize is CanvasSize.Free) calculateBounds(strokes) else null
        val (width, height) = getDimensions(canvasSize, bounds)
        val bgHex = String.format("#%06X", (0xFFFFFF and backgroundColor.toArgb()))
        
        val svgBuilder = StringBuilder()
        // Use fixed width/height for standard sizes or bounds for Free size
        svgBuilder.append("<svg width=\"$width\" height=\"$height\" viewBox=\"${bounds?.left ?: 0f} ${bounds?.top ?: 0f} $width $height\" xmlns=\"http://www.w3.org/2000/svg\">\n")
        svgBuilder.append("  <rect x=\"${bounds?.left ?: 0f}\" y=\"${bounds?.top ?: 0f}\" width=\"$width\" height=\"$height\" fill=\"$bgHex\" />\n")

        strokes.forEach { stroke ->
            var strokeColor = Color(stroke.color.toULong())
            if (backgroundColor.luminance() > 0.5f && strokeColor.luminance() > 0.9f) {
                strokeColor = Color.Black
            } else if (backgroundColor.luminance() <= 0.5f && strokeColor.luminance() < 0.1f) {
                strokeColor = Color.White
            }
            val colorHex = String.format("#%06X", (0xFFFFFF and strokeColor.toArgb()))
            
            svgBuilder.append("  <path d=\"")
            if (stroke.points.isNotEmpty()) {
                svgBuilder.append("M ${stroke.points.first().x} ${stroke.points.first().y} ")
                for (i in 1 until stroke.points.size) {
                    val prev = stroke.points[i - 1]
                    val curr = stroke.points[i]
                    val midX = (prev.x + curr.x) / 2f
                    val midY = (prev.y + curr.y) / 2f
                    svgBuilder.append("Q ${prev.x} ${prev.y} $midX $midY ")
                }
                svgBuilder.append("L ${stroke.points.last().x} ${stroke.points.last().y}")
            }
            svgBuilder.append("\" fill=\"none\" stroke=\"$colorHex\" stroke-width=\"${stroke.strokeWidth}\" stroke-linecap=\"round\" stroke-linejoin=\"round\" />\n")
        }
        svgBuilder.append("</svg>")

        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(svgBuilder.toString())
        return tempFile
    }

    fun exportToJson(context: Context, strokes: List<ActiveStroke>, fileName: String): File {
        val jsonString = Json.encodeToString(strokes)
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(jsonString)
        return tempFile
    }

    fun exportToText(context: Context, strokes: List<ActiveStroke>, fileName: String): File {
        val summary = "Sketch Summary\nStrokes: ${strokes.size}\nTotal Points: ${strokes.sumOf { it.points.size }}"
        val tempFile = File(context.cacheDir, fileName)
        tempFile.writeText(summary)
        return tempFile
    }

    private fun getDimensions(canvasSize: CanvasSize, bounds: Rect?): Pair<Int, Int> {
        if (bounds != null) return bounds.width.toInt() to bounds.height.toInt()
        return when (canvasSize) {
            is CanvasSize.A4 -> 2480 to 3508
            is CanvasSize.A3 -> 3508 to 4961
            is CanvasSize.Custom -> canvasSize.size?.let { it.width.toInt() to it.height.toInt() } ?: (1080 to 1920)
            else -> 1080 to 1920
        }
    }
}
