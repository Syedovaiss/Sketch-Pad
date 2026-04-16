package com.ovais.sketch_pad.pad.data

data class SketchToolbarOptions(
    val showMove: Boolean = true,
    val showDraw: Boolean = true,
    val showErase: Boolean = true,
    val showUndo: Boolean = true,
    val showRedo: Boolean = true,
    val showClear: Boolean = true,
    val showSave: Boolean = true,
    val showDownloadFile: Boolean = true,
    val showDownloadImage: Boolean = true,
    val showOrientation: Boolean = false,
    val showSettings: Boolean = true,
    val showColorPalette: Boolean = true,
    val showBrushSize: Boolean = true
) {
    companion object {
        val Default = SketchToolbarOptions()
    }
}
