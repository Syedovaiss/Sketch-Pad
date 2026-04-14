package com.ovais.sketch_pad.pad.data

import androidx.compose.ui.geometry.Size

/**
 * Represents the size of the drawing canvas.
 * Dimensions are in pixels at a base DPI.
 */
sealed class CanvasSize(val label: String, val size: Size?) {
    object Free : CanvasSize("Free", null)
    object A4 : CanvasSize("A4", Size(2480f, 3508f)) // 300 DPI
    object A3 : CanvasSize("A3", Size(3508f, 4961f)) // 300 DPI
    object Screen : CanvasSize("Screen", null) // Dynamic to screen size
    data class Custom(val width: Float, val height: Float) : CanvasSize("Custom", Size(width, height))
}
