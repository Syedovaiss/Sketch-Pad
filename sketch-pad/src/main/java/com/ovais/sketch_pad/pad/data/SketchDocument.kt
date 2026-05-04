package com.ovais.sketch_pad.pad.data

import kotlinx.serialization.Serializable

/**
 * Versioned, editable sketch payload for persistence and backend sync.
 * Prefer this over raw `[ActiveStroke]` JSON when you need forward-compatible schema evolution.
 */
@Serializable
data class SketchDocument(
    /**
     * Increment when the JSON shape changes in a breaking way.
     * Readers must reject or migrate unknown versions.
     */
    val formatVersion: Int = 1,
    /**
     * Optional id for correlating with your backend / local DB row.
     */
    val sketchId: String? = null,
    val strokes: List<ActiveStroke> = emptyList()
) {
    companion object {
        const val CURRENT_FORMAT_VERSION: Int = 1

        fun fromStrokes(
            strokes: List<ActiveStroke>,
            sketchId: String? = null
        ): SketchDocument = SketchDocument(
            formatVersion = CURRENT_FORMAT_VERSION,
            sketchId = sketchId,
            strokes = strokes
        )
    }
}
