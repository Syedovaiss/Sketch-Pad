package com.ovais.sketch_pad.utils

import android.graphics.Bitmap
import android.util.Base64
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import com.ovais.sketch_pad.pad.data.ActiveStroke
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream

internal fun distanceToSegment(
    px: Float, py: Float,
    x1: Float, y1: Float,
    x2: Float, y2: Float
): Float {

    val A = px - x1
    val B = py - y1
    val C = x2 - x1
    val D = y2 - y1

    val dot = A * C + B * D
    val lenSq = C * C + D * D

    val t = if (lenSq != 0f) dot / lenSq else -1f

    val (xx, yy) = when {
        t < 0 -> x1 to y1
        t > 1 -> x2 to y2
        else -> (x1 + t * C) to (y1 + t * D)
    }

    val dx = px - xx
    val dy = py - yy

    return kotlin.math.sqrt(dx * dx + dy * dy)
}

internal fun eraseAt(
    strokes: MutableList<ActiveStroke>,
    x: Float,
    y: Float,
    threshold: Float = 30f
) {
    val iterator = strokes.iterator()

    while (iterator.hasNext()) {
        val stroke = iterator.next()

        val pts = stroke.points
        for (i in 0 until pts.size - 1) {

            val d = distanceToSegment(
                x, y,
                pts[i].x, pts[i].y,
                pts[i + 1].x, pts[i + 1].y
            )

            if (d < threshold + stroke.strokeWidth) {
                iterator.remove()
                return
            }
        }
    }
}



fun Color.toArgbLong(): Long = toArgb().toLong() and 0xFFFFFFFFL

fun Long.toColor(): Color = Color(this.toInt())

private const val PNG_DATA_URI_PREFIX = "data:image/png;base64,"

private val sketchJson = Json {
    ignoreUnknownKeys = true
}

/**
 * Encodes editable stroke data to Base64 JSON for save/restore flows.
 */
fun encodeStrokesToBase64(strokes: List<ActiveStroke>): String {
    val json = sketchJson.encodeToString(strokes)
    return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
}

/**
 * Decodes stroke data from Base64 JSON and returns editable strokes.
 */
fun decodeStrokesFromBase64(base64: String): List<ActiveStroke> {
    val decodedBytes = Base64.decode(base64.trim(), Base64.DEFAULT)
    val json = decodedBytes.toString(Charsets.UTF_8)
    return sketchJson.decodeFromString(json)
}

/**
 * Converts Android [Bitmap] to raw Base64 PNG bytes.
 */
fun Bitmap.toBase64Png(): String {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, output)
    return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
}

/**
 * Converts Compose [ImageBitmap] to raw Base64 PNG bytes.
 */
fun ImageBitmap.toBase64Png(): String = asAndroidBitmap().toBase64Png()

/**
 * Ensures raw Base64 PNG is returned as Data URI:
 * data:image/png;base64,...
 */
fun String.toPngDataUri(): String {
    val value = trim()
    return if (value.startsWith(PNG_DATA_URI_PREFIX)) value else "$PNG_DATA_URI_PREFIX$value"
}