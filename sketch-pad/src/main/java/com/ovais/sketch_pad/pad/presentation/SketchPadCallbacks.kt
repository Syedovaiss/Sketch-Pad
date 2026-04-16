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
    fun onClear() {}
    fun onSave(strokes: List<ActiveStroke>) {}
    fun onDownloadFile(file: File, type: SketchFileType) {}
    fun onDownloadImage(image: ImageBitmap) {}
    fun onOrientationToggleRequested(targetOrientation: SketchOrientation) {}
    fun onOrientationChanged(orientation: SketchOrientation) {}
}

