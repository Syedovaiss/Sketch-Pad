package com.ovais.sketch_pad.pad.data

/**
 * How the eraser interacts with existing ink.
 */
sealed interface EraseType {

    /**
     * Removes the entire [ActiveStroke] when the eraser touches any part of it.
     */
    data object WholeStroke : EraseType

    /**
     * Splits strokes so only the portion inside a disc around the pointer is removed.
     *
     * @param radius Radius of the eraser in **model / stroke coordinate space** (same units as
     *   [SketchPoint] values), before adding half of the stroke width for hit testing.
     */
    data class Area(val radius: Float = 24f) : EraseType
}
