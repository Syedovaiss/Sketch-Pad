package com.ovais.sketch_pad.pad.domain

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.ToolMode
import java.util.UUID
import kotlin.collections.ArrayDeque
import kotlin.math.hypot

/**
 * The central engine for the Sketch Pad.
 * It manages the list of strokes, undo/redo history, tool modes, and coordinate transformations.
 * Use this in your ViewModel to persist the sketch state across configuration changes.
 */
class SketchController {

    private val _strokes = mutableStateListOf<ActiveStroke>()
    val strokes: List<ActiveStroke> get() = _strokes

    private val history = ArrayDeque<HistoryAction>()
    private val redoStack = ArrayDeque<HistoryAction>()

    var toolMode by mutableStateOf(ToolMode.DRAW)
    var brushColor by mutableStateOf(Color.Black)
    var brushWidth by mutableFloatStateOf(10f)
    var isGridEnabled by mutableStateOf(true)

    // Current sketch session ID
    var sketchId by mutableStateOf("default")

    // Centralized transform state for performance and sync
    var scale by mutableFloatStateOf(1f)
    var translation by mutableStateOf(Offset.Zero)

    // Cache paths to avoid re-creating them every frame
    private val pathCache = mutableMapOf<String, Path>()

    var onEvent: ((SketchEvent) -> Unit)? = null

    private sealed class HistoryAction {
        data class AddStroke(val stroke: ActiveStroke) : HistoryAction()
        data class RemoveStroke(val stroke: ActiveStroke, val index: Int) : HistoryAction()
        data class Clear(val strokes: List<ActiveStroke>) : HistoryAction()
    }

    fun canUndo(): Boolean = history.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    /**
     * Resets the controller for a new sketch session.
     * Clears all strokes, history, and resets the canvas scale and translation.
     * @param id A unique identifier for the new sketch.
     */
    fun newSketch(id: String = UUID.randomUUID().toString()) {
        sketchId = id
        _strokes.clear()
        pathCache.clear()
        history.clear()
        redoStack.clear()
        translation = Offset.Zero
        scale = 1f
        onEvent?.invoke(SketchEvent.Clear)
    }

    fun setStrokes(newStrokes: List<ActiveStroke>, id: String = "default") {
        sketchId = id
        pathCache.clear()
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

    /**
     * Retrieves or creates a [Path] for the given [ActiveStroke].
     * Uses an internal [pathCache] to avoid redundant [Path] object allocations during rendering.
     */
    fun getPathFor(stroke: ActiveStroke): Path {
        return pathCache.getOrPut(stroke.id) {
            Path().apply {
                if (stroke.points.isNotEmpty()) {
                    moveTo(stroke.points.first().x, stroke.points.first().y)
                    for (i in 1 until stroke.points.size) {
                        val prev = stroke.points[i - 1]
                        val curr = stroke.points[i]
                        val midX = (prev.x + curr.x) / 2f
                        val midY = (prev.y + curr.y) / 2f
                        quadraticTo(prev.x, prev.y, midX, midY)
                    }
                    lineTo(stroke.points.last().x, stroke.points.last().y)
                }
            }
        }
    }

    fun undo() {
        if (history.isEmpty()) return
        val action = history.removeLast()
        redoStack.addLast(action)

        when (action) {
            is HistoryAction.AddStroke -> {
                _strokes.remove(action.stroke)
                pathCache.remove(action.stroke.id)
                onEvent?.invoke(SketchEvent.Undo(action.stroke))
            }
            is HistoryAction.RemoveStroke -> {
                _strokes.add(action.index, action.stroke)
                onEvent?.invoke(SketchEvent.Undo(action.stroke))
            }
            is HistoryAction.Clear -> {
                _strokes.addAll(action.strokes)
                action.strokes.lastOrNull()?.let { restoredStroke ->
                    onEvent?.invoke(SketchEvent.Undo(restoredStroke))
                }
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
                pathCache.remove(action.stroke.id)
                onEvent?.invoke(SketchEvent.StrokeRemoved(action.stroke))
            }
            is HistoryAction.Clear -> {
                _strokes.clear()
                pathCache.clear()
                onEvent?.invoke(SketchEvent.Clear)
            }
        }
    }

    /**
     * Performs a smart erasure at the given coordinates.
     * Checks for proximity to any existing stroke segments.
     */
    fun eraseAt(x: Float, y: Float) {
        for (i in _strokes.indices.reversed()) {
            val stroke = _strokes[i]
            val pts = stroke.points
            if (pts.size == 1) {
                val singlePoint = pts.first()
                val d = hypot(
                    (x - singlePoint.x).toDouble(),
                    (y - singlePoint.y).toDouble()
                ).toFloat()
                if (d < 30f + stroke.strokeWidth) {
                    val removedStroke = _strokes.removeAt(i)
                    pathCache.remove(removedStroke.id)
                    history.addLast(HistoryAction.RemoveStroke(removedStroke, i))
                    redoStack.clear()
                    onEvent?.invoke(SketchEvent.StrokeRemoved(removedStroke))
                    return
                }
            }
            for (j in 0 until pts.size - 1) {
                val d = com.ovais.sketch_pad.utils.distanceToSegment(
                    x, y,
                    pts[j].x, pts[j].y,
                    pts[j + 1].x, pts[j + 1].y
                )
                if (d < 30f + stroke.strokeWidth) {
                    val removedStroke = _strokes.removeAt(i)
                    pathCache.remove(removedStroke.id)
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
        pathCache.clear()
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
