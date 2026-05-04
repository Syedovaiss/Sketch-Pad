package com.ovais.sketchpad.features.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchDocument
import com.ovais.sketch_pad.pad.domain.SketchController
import com.ovais.sketch_pad.utils.decodeStrokesFlexible
import com.ovais.sketch_pad.utils.encodeSketchDocument
import com.ovais.sketchpad.features.domain.SketchRepo
import kotlinx.coroutines.launch

class SketchViewModel(
    private val repo: SketchRepo
) : ViewModel() {

    val controller = SketchController()

    init {
        loadDraft()
    }

    private fun loadDraft() {
        viewModelScope.launch {
            val data = repo.load("default")
            if (data != null) {
                try {
                    val strokes = decodeStrokesFlexible(data)
                    controller.setStrokes(strokes, "default")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveDraft(strokes: List<ActiveStroke>, id: String = "default") {
        viewModelScope.launch {
            val jsonStr = encodeSketchDocument(SketchDocument.fromStrokes(strokes, sketchId = id))
            repo.save(id, jsonStr)
        }
    }

    fun clearDraft(id: String = "default") {
        viewModelScope.launch {
            repo.save(id, "[]")
        }
    }
}
