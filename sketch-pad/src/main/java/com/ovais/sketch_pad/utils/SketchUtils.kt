package com.ovais.sketch_pad.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.ovais.sketch_pad.pad.data.ActiveStroke

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