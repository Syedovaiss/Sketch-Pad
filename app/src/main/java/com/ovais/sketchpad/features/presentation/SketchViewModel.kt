package com.ovais.sketchpad.features.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.domain.SketchController
import com.ovais.sketchpad.features.domain.SketchRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SketchViewModel @Inject constructor(
    private val repo: SketchRepo,
    private val json: Json
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
                    val strokes = json.decodeFromString<List<ActiveStroke>>(data)
                    controller.setStrokes(strokes, "default")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun saveDraft(strokes: List<ActiveStroke>, id: String = "default") {
        viewModelScope.launch {
            val jsonStr = json.encodeToString(strokes)
            repo.save(id, jsonStr)
        }
    }

    fun clearDraft(id: String = "default") {
        viewModelScope.launch {
            repo.save(id, "[]")
        }
    }
}
