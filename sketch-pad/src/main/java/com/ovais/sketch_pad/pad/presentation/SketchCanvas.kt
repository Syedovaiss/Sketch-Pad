package com.ovais.sketch_pad.pad.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchPadIcons
import com.ovais.sketch_pad.pad.data.SketchPoint
import com.ovais.sketch_pad.pad.data.ToolMode
import com.ovais.sketch_pad.pad.domain.SketchController
import com.ovais.sketch_pad.utils.toArgbLong
import com.ovais.sketch_pad.utils.toColor
import kotlin.math.hypot

/**
 * A "headless" Sketch Canvas that handles drawing logic and gestures.
 * Supports infinite canvas panning and zooming.
 */
@Composable
fun SketchCanvas(
    modifier: Modifier = Modifier,
    controller: SketchController,
    backgroundColor: Color = Color.Transparent,
    onStrokeStarted: () -> Unit = {},
    onStrokeEnded: (ActiveStroke) -> Unit = {}
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    var isPointerDown by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }
    val activePoints = remember { mutableStateListOf<SketchPoint>() }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        translation += offsetChange
    }

    Box(
        modifier
            .background(backgroundColor)
            .transformable(state = transformState)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(controller.toolMode) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            isPointerDown = true
                            val adjustedPos = (pos - translation) / scale

                            if (controller.toolMode == ToolMode.ERASE) {
                                isErasing = true
                                eraserPosition = pos
                                controller.eraseAt(adjustedPos.x, adjustedPos.y)
                            } else if (controller.toolMode == ToolMode.DRAW) {
                                onStrokeStarted()
                                activePoints.clear()
                                activePoints.add(SketchPoint(adjustedPos.x, adjustedPos.y))
                            }
                        },
                        onDrag = { change, dragAmount ->
                            val pos = change.position
                            change.consume()

                            if (!isPointerDown) return@detectDragGestures

                            if (controller.toolMode == ToolMode.MOVE) {
                                translation += dragAmount
                                return@detectDragGestures
                            }

                            val adjustedPos = (pos - translation) / scale

                            if (controller.toolMode == ToolMode.ERASE) {
                                eraserPosition = pos
                                controller.eraseAt(adjustedPos.x, adjustedPos.y)
                                return@detectDragGestures
                            }

                            if (controller.toolMode == ToolMode.DRAW) {
                                val last = activePoints.lastOrNull()
                                if (last == null || hypot(
                                        (adjustedPos.x - last.x).toDouble(),
                                        (adjustedPos.y - last.y).toDouble()
                                    ) > (1.0 / scale)
                                ) {
                                    activePoints.add(SketchPoint(adjustedPos.x, adjustedPos.y))
                                }
                            }
                        },
                        onDragEnd = {
                            isPointerDown = false
                            isErasing = false
                            eraserPosition = null

                            if (controller.toolMode == ToolMode.DRAW && activePoints.isNotEmpty()) {
                                val stroke = ActiveStroke(
                                    points = activePoints.toList(),
                                    color = controller.brushColor.toArgbLong(),
                                    strokeWidth = controller.brushWidth
                                )
                                controller.add(stroke)
                                onStrokeEnded(stroke)
                            }
                            activePoints.clear()
                        }
                    )
                }
        ) {
            withTransform({
                translate(translation.x, translation.y)
                scale(scale, scale, Offset.Zero)
            }) {
                // Draw existing strokes
                controller.strokes.forEach { stroke ->
                    drawSmoothPath(stroke.points, stroke.color.toColor(), stroke.strokeWidth)
                }
                // Draw current active stroke
                if (controller.toolMode == ToolMode.DRAW) {
                    drawSmoothPath(activePoints, controller.brushColor, controller.brushWidth)
                }
            }
        }

        // Eraser Cursor
        eraserPosition?.let { pos ->
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            pos.x.toInt() - 16,
                            pos.y.toInt() - 16
                        )
                    }
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.7f), CircleShape)
                    .zIndex(100f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(SketchPadIcons.Default.eraseIcon),
                    null
                )
            }
        }
    }
}

private fun DrawScope.drawSmoothPath(points: List<SketchPoint>, color: Color, width: Float) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            quadraticTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
