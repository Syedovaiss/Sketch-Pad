package com.ovais.sketch_pad.pad.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.ToolMode

class SketchController {

    val _strokes = mutableStateListOf<ActiveStroke>()
    val strokes: List<ActiveStroke> get() = _strokes

    private val undone = ArrayDeque<ActiveStroke>()

    // SDK State moved here for better flexibility & ViewModel integration
    var toolMode by mutableStateOf(ToolMode.DRAW)
    var brushColor by mutableStateOf(Color.Black)
    var brushWidth by mutableFloatStateOf(6f)

    // Event callback for host apps/ViewModels
    var onEvent: ((SketchEvent) -> Unit)? = null

    fun setStrokes(newStrokes: List<ActiveStroke>) {
        _strokes.clear()
        _strokes.addAll(newStrokes)
        undone.clear()
    }

    fun add(stroke: ActiveStroke) {
        _strokes.add(stroke)
        undone.clear()
        onEvent?.invoke(SketchEvent.StrokeAdded(stroke))
    }

    fun undo() {
        if (_strokes.isNotEmpty()) {
            val last = _strokes.removeAt(_strokes.size - 1)
            undone.addLast(last)
            onEvent?.invoke(SketchEvent.Undo(last))
        }
    }

    fun redo() {
        if (undone.isNotEmpty()) {
            val stroke = undone.removeLast()
            _strokes.add(stroke)
            onEvent?.invoke(SketchEvent.Redo(stroke))
        }
    }

    fun eraseAt(x: Float, y: Float) {
        val iterator = _strokes.iterator()
        while (iterator.hasNext()) {
            val stroke = iterator.next()
            val pts = stroke.points
            for (i in 0 until pts.size - 1) {
                val d = com.ovais.sketch_pad.utils.distanceToSegment(
                    x, y,
                    pts[i].x, pts[i].y,
                    pts[i + 1].x, pts[i + 1].y
                )
                if (d < 30f + stroke.strokeWidth) {
                    iterator.remove()
                    undone.clear() // Erasing clears redo history as it's a new state change
                    onEvent?.invoke(SketchEvent.StrokeRemoved(stroke))
                    return
                }
            }
        }
    }

    fun clear() {
        if (_strokes.isEmpty()) return
        _strokes.clear()
        undone.clear()
        onEvent?.invoke(SketchEvent.Clear)
    }
}

sealed class SketchEvent {
    data class StrokeAdded(val stroke: ActiveStroke) : SketchEvent()
    data class StrokeRemoved(val stroke: ActiveStroke) : SketchEvent()
    data class Undo(val stroke: ActiveStroke) : SketchEvent()
    data class Redo(val stroke: ActiveStroke) : SketchEvent()
    object Clear : SketchEvent()
}
