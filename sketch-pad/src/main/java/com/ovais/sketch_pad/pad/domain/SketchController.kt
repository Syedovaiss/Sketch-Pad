package com.ovais.sketch_pad.pad.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.ToolMode
import kotlin.collections.ArrayDeque

class SketchController {

    val _strokes = mutableStateListOf<ActiveStroke>()
    val strokes: List<ActiveStroke> get() = _strokes

    private val history = ArrayDeque<HistoryAction>()
    private val redoStack = ArrayDeque<HistoryAction>()

    var toolMode by mutableStateOf(ToolMode.DRAW)
    var brushColor by mutableStateOf(Color.Black)
    var brushWidth by mutableFloatStateOf(6f)

    var onEvent: ((SketchEvent) -> Unit)? = null

    private sealed class HistoryAction {
        data class AddStroke(val stroke: ActiveStroke) : HistoryAction()
        data class RemoveStroke(val stroke: ActiveStroke, val index: Int) : HistoryAction()
        data class Clear(val strokes: List<ActiveStroke>) : HistoryAction()
    }

    fun canUndo(): Boolean = history.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun setStrokes(newStrokes: List<ActiveStroke>) {
        _strokes.clear()
        _strokes.addAll(newStrokes)
        history.clear()
        redoStack.clear()
    }

    fun add(stroke: ActiveStroke) {
        _strokes.add(stroke)
        history.addLast(HistoryAction.AddStroke(stroke))
        redoStack.clear()
        onEvent?.invoke(SketchEvent.StrokeAdded(stroke))
    }

    fun undo() {
        if (history.isEmpty()) return
        val action = history.removeLast()
        redoStack.addLast(action)

        when (action) {
            is HistoryAction.AddStroke -> {
                _strokes.remove(action.stroke)
                onEvent?.invoke(SketchEvent.Undo(action.stroke))
            }
            is HistoryAction.RemoveStroke -> {
                _strokes.add(action.index, action.stroke)
                onEvent?.invoke(SketchEvent.Redo(action.stroke))
            }
            is HistoryAction.Clear -> {
                _strokes.addAll(action.strokes)
                onEvent?.invoke(SketchEvent.Redo(action.strokes.lastOrNull() ?: return))
            }
        }
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        val action = redoStack.removeLast()
        history.addLast(action)

        when (action) {
            is HistoryAction.AddStroke -> {
                _strokes.add(action.stroke)
                onEvent?.invoke(SketchEvent.StrokeAdded(action.stroke))
            }
            is HistoryAction.RemoveStroke -> {
                _strokes.remove(action.stroke)
                onEvent?.invoke(SketchEvent.StrokeRemoved(action.stroke))
            }
            is HistoryAction.Clear -> {
                _strokes.clear()
                onEvent?.invoke(SketchEvent.Clear)
            }
        }
    }

    fun eraseAt(x: Float, y: Float) {
        for (i in _strokes.indices.reversed()) {
            val stroke = _strokes[i]
            val pts = stroke.points
            for (j in 0 until pts.size - 1) {
                val d = com.ovais.sketch_pad.utils.distanceToSegment(
                    x, y,
                    pts[j].x, pts[j].y,
                    pts[j + 1].x, pts[j + 1].y
                )
                if (d < 30f + stroke.strokeWidth) {
                    val removedStroke = _strokes.removeAt(i)
                    history.addLast(HistoryAction.RemoveStroke(removedStroke, i))
                    redoStack.clear()
                    onEvent?.invoke(SketchEvent.StrokeRemoved(removedStroke))
                    return
                }
            }
        }
    }

    fun clear() {
        if (_strokes.isEmpty()) return
        val snapshot = _strokes.toList()
        _strokes.clear()
        history.addLast(HistoryAction.Clear(snapshot))
        redoStack.clear()
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
