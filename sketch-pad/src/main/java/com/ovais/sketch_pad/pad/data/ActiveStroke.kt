package com.ovais.sketch_pad.pad.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import kotlinx.serialization.Serializable

@Serializable
data class ActiveStroke(
    val points: List<SketchPoint>,
    val color: Long,
    val strokeWidth: Float
)

fun ActiveStroke.toPath(): Path {
    val path = Path()

    if (points.isEmpty()) return path

    path.moveTo(points.first().x, points.first().y)

    for (i in 1 until points.size) {
        path.lineTo(points[i].x, points[i].y)
    }

    return path
}