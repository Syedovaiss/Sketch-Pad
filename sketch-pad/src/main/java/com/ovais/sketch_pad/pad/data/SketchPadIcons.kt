package com.ovais.sketch_pad.pad.data

import com.ovais.sketch_pad.R

data class SketchPadIcons(
    val moveIcon: Int = R.drawable.ic_move,
    val drawIcon: Int = R.drawable.ic_draw,
    val eraseIcon: Int = R.drawable.ic_erase,
    val undoIcon: Int = R.drawable.ic_undo,
    val redoIcon: Int = R.drawable.ic_redo,
    val clearIcon: Int = R.drawable.ic_clear,
    val saveIcon: Int = R.drawable.ic_save,
    val settingsIcon: Int = R.drawable.ic_settings,
    val downloadFile: Int = R.drawable.download_file,
    val downloadImage: Int = R.drawable.download_image,
) {
    companion object {
        val Default = SketchPadIcons()
    }
}