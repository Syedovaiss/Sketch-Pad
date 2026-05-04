package com.ovais.sketch_pad.pad.presentation

import androidx.compose.ui.graphics.ImageBitmap
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchFileType
import com.ovais.sketch_pad.pad.data.SketchOrientation
import com.ovais.sketch_pad.pad.data.SketchStrokePersistenceFormat
import java.io.File

/**
 * Centralized callback interface for SketchPad events.
 */
interface SketchPadCallbacks {
    /**
     * Called when the user taps the toolbar clear action. The canvas is cleared only if you invoke
     * [performClear] (after any confirmation, e.g. an [android.app.AlertDialog]).
     *
     * Default: clears immediately. Override to show a dialog; call [performClear] on confirm only —
     * do not call it when the user cancels, so the sketch stays unchanged.
     */
    fun onClearRequested(performClear: () -> Unit) {
        performClear()
    }

    /**
     * Called after the canvas has been cleared via [onClearRequested]'s [performClear].
     */
    fun onClear() {}
    fun onSave(strokes: List<ActiveStroke>) {}

    /**
     * Called after [onSave] when [SketchPad.strokePersistenceFormat] is not [SketchStrokePersistenceFormat.None].
     * The default [SketchPad] format is [SketchStrokePersistenceFormat.Base64].
     *
     * @param payload Encoding depends on [format] (plain JSON, Base64, gzip+Base64, etc.).
     */
    fun onSavePersistencePayload(
        strokes: List<ActiveStroke>,
        payload: String,
        format: SketchStrokePersistenceFormat
    ) {
    }

    fun onDownloadFile(file: File, type: SketchFileType) {}
    fun onDownloadImage(image: ImageBitmap) {}
    fun onOrientationToggleRequested(targetOrientation: SketchOrientation) {}
    fun onOrientationChanged(orientation: SketchOrientation) {}
}

