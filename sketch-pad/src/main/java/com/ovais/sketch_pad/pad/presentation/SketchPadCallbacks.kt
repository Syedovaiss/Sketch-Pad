package com.ovais.sketch_pad.pad.presentation

import androidx.compose.ui.graphics.ImageBitmap
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchFileType
import com.ovais.sketch_pad.pad.data.SketchOrientation
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
    fun onDownloadFile(file: File, type: SketchFileType) {}
    fun onDownloadImage(image: ImageBitmap) {}
    fun onOrientationToggleRequested(targetOrientation: SketchOrientation) {}
    fun onOrientationChanged(orientation: SketchOrientation) {}
}

