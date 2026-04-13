package com.ovais.sketchpad.features.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.ToolMode
import com.ovais.sketchpad.features.domain.SketchRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SketchViewModel @Inject constructor(
    private val repo: SketchRepo,
    private val json: Json
) : ViewModel() {


    private val _state = MutableStateFlow(SketchState())
    val state = _state

    init {
        loadDraft()
    }

    private fun loadDraft() {
        viewModelScope.launch {
            val data = repo.load("default")

            if (data != null) {
                val strokes = json.decodeFromString<List<ActiveStroke>>(data)

                _state.value = _state.value.copy(
                    strokes = strokes
                )
            }
        }
    }

    fun saveDraft(strokes: List<ActiveStroke>) {
        viewModelScope.launch {
            val jsonStr = json.encodeToString(
                strokes
            )

            repo.save("default", jsonStr)
        }
    }
}

data class SketchState(
    val strokes: List<ActiveStroke> = emptyList(),
    val toolMode: ToolMode = ToolMode.DRAW
)