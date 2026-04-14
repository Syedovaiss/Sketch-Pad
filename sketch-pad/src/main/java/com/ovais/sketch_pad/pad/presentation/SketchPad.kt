package com.ovais.sketch_pad.pad.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.ovais.sketch_pad.R
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchPoint
import com.ovais.sketch_pad.pad.data.ToolMode
import com.ovais.sketch_pad.pad.domain.SketchController
import com.ovais.sketch_pad.utils.toArgbLong
import com.ovais.sketch_pad.utils.toColor
import kotlin.math.hypot

/**
 * A reusable SketchPad component for drawing and erasing.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param controller The controller to manage the sketch state.
 * @param backgroundColor The background color of the canvas.
 * @param showToolbar Whether to show the default toolbar.
 * @param onSave Callback when the save button is clicked.
 */
@Composable
fun SketchPad(
    modifier: Modifier = Modifier,
    controller: SketchController = remember { SketchController() },
    backgroundColor: Color = Color.Unspecified,
    showToolbar: Boolean = true,
    toolbarOptions: SketchToolbarOptions = SketchToolbarOptions.Default,
    icons: SketchPadIcons = SketchPadIcons.Default,
    onClear: () -> Unit = {},
    onSave: (List<ActiveStroke>) -> Unit = {},
    onDownloadFile: (List<ActiveStroke>) -> Unit = {},
    onDownloadImage: (List<ActiveStroke>) -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val finalBackgroundColor = if (backgroundColor == Color.Unspecified) {
        if (isDark) Color(0xFF1C1B1F) else Color.White
    } else backgroundColor

    // Sync controller with theme defaults if not manually set
    LaunchedEffect(isDark) {
        if (controller.brushColor == Color.Black && isDark) {
            controller.brushColor = Color.White
        } else if (controller.brushColor == Color.White && !isDark) {
            controller.brushColor = Color.Black
        }
    }

    val toolMode = controller.toolMode
    val brushColor = controller.brushColor
    val brushWidth = controller.brushWidth

    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(Offset.Zero) }

    var toolbarExpanded by remember { mutableStateOf(false) }

    var isPointerDown by remember { mutableStateOf(false) }

    var isErasing by remember { mutableStateOf(false) }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }

    var showColorPicker by remember { mutableStateOf(false) }

    val activePoints = remember { mutableStateListOf<SketchPoint>() }

    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .background(
                        if (isDark) Color(0xFF2C2C2C) else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp)
            ) {
                    val colorPickerController = rememberColorPickerController()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        controller = colorPickerController,
                        onColorChanged = { envelope ->
                            controller.brushColor = envelope.color
                        }
                    )

                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Done")
                    }
                }
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(finalBackgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(toolMode) {
                    detectDragGestures(
                        onDragStart = { pos ->
                            isPointerDown = true

                            if (toolMode == ToolMode.ERASE) {
                                isErasing = true
                                val adjustedPos = (pos - translation) / scale
                                eraserPosition = pos
                                controller.eraseAt(adjustedPos.x, adjustedPos.y)
                            } else if (toolMode == ToolMode.DRAW) {
                                val adjustedPos = (pos - translation) / scale
                                activePoints.clear()
                                activePoints.add(SketchPoint(adjustedPos.x, adjustedPos.y))
                            }
                        },

                        onDrag = { change, dragAmount ->
                            val pos = change.position
                            change.consume()

                            if (!isPointerDown) return@detectDragGestures

                            if (toolMode == ToolMode.MOVE) {
                                translation += dragAmount
                                return@detectDragGestures
                            }

                            val adjustedPos = (pos - translation) / scale

                            if (toolMode == ToolMode.ERASE) {
                                eraserPosition = pos
                                controller.eraseAt(adjustedPos.x, adjustedPos.y)
                                return@detectDragGestures
                            }

                            if (toolMode == ToolMode.DRAW) {
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

                            if (toolMode == ToolMode.DRAW && activePoints.isNotEmpty()) {

                                val snapshot = activePoints.toList()

                                controller.add(
                                    ActiveStroke(
                                        points = snapshot,
                                        color = brushColor.toArgbLong(),
                                        strokeWidth = brushWidth
                                    )
                                )
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
                controller.strokes.forEach { stroke ->
                    drawSmoothStroke(stroke.points, stroke.color.toColor(), stroke.strokeWidth)
                }

                drawSmoothStroke(activePoints, brushColor, brushWidth)
            }
        }

        EraserCursor(
            positionProvider = { eraserPosition },
            isErasing = isErasing,
            icon = icons.eraseIcon
        )

        if (showToolbar) {
            val toolbarBg = if (isDark) Color(0xFF2C2C2C) else Color(0xFF121212)
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(10.dp)
                    .background(toolbarBg, RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .zIndex(100f)
            ) {

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (toolbarOptions.showMove) {
                        ToolButton(
                            icon = icons.moveIcon,
                            selected = toolMode == ToolMode.MOVE
                        ) {
                            controller.toolMode = ToolMode.MOVE
                            isErasing = false
                            eraserPosition = null
                        }
                    }

                    if (toolbarOptions.showDraw) {
                        ToolButton(
                            icon = icons.drawIcon,
                            selected = toolMode == ToolMode.DRAW
                        ) {
                            controller.toolMode = ToolMode.DRAW
                            isErasing = false
                            eraserPosition = null
                        }
                    }

                    if (toolbarOptions.showErase) {
                        ToolButton(
                            icon = icons.eraseIcon,
                            selected = toolMode == ToolMode.ERASE
                        ) {
                            controller.toolMode = ToolMode.ERASE
                            isErasing = false
                            eraserPosition = null
                        }
                    }

                    if (toolbarOptions.showUndo) {
                        ToolButton(icon = icons.undoIcon, selected = false) {
                            controller.undo()
                        }
                    }

                    if (toolbarOptions.showRedo) {
                        ToolButton(icon = icons.redoIcon, selected = false) {
                            controller.redo()
                        }
                    }

                    if (toolbarOptions.showClear) {
                        ToolButton(icon = icons.clearIcon, selected = false) {
                            onClear()
                        }
                    }

                    if (toolbarOptions.showSave) {
                        ToolButton(icon = icons.saveIcon, selected = false) {
                            onSave(controller.strokes)
                        }
                    }

                    if (toolbarOptions.showDownloadFile) {
                        ToolButton(icon = icons.downloadFile, selected = false) {
                            onDownloadFile(controller.strokes)
                        }
                    }

                    if (toolbarOptions.showDownloadImage) {
                        ToolButton(icon = icons.downloadImage, selected = false) {
                            onDownloadImage(controller.strokes)
                        }
                    }

                    if (toolbarOptions.showSettings) {
                        ToolButton(
                            icon = icons.settingsIcon,
                            selected = toolbarExpanded
                        ) {
                            toolbarExpanded = !toolbarExpanded
                        }
                    }
                }

                if (toolbarExpanded) {

                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val colorPalette = if (isDark) {
                            listOf(Color.White, Color.Red, Color.Green, Color.Blue, Color.Yellow)
                        } else {
                            listOf(Color.Black, Color.Red, Color.Green, Color.Blue, Color.Yellow)
                        }
                        colorPalette.forEach { color ->
                            Box(
                                Modifier
                                    .size(34.dp)
                                    .background(color, CircleShape)
                                    .pointerInput(Unit) {
                                        detectTapGestures {
                                            controller.brushColor = color
                                        }
                                    }
                            )
                        }

                        // Color Picker Trigger
                        Box(
                            Modifier
                                .size(34.dp)
                                .background(
                                    brushColor,
                                    CircleShape
                                )
                                .border(2.dp, if (isDark) Color.White else Color.Black, CircleShape)
                                .pointerInput(Unit) {
                                    detectTapGestures {
                                        showColorPicker = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.color_palette),
                                contentDescription = "Pick Color",
                                tint = if (brushColor.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Text("Brush Size: ${brushWidth.toInt()}", color = Color.White)

                    Slider(
                        value = brushWidth,
                        onValueChange = { controller.brushWidth = it },
                        valueRange = 2f..40f
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawSmoothStroke(
    points: List<SketchPoint>,
    color: Color,
    width: Float
) {
    if (points.size < 2) return

    val path = Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)

        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]

            val mid = Offset(
                (prev.x + curr.x) / 2f,
                (prev.y + curr.y) / 2f
            )

            quadraticTo(prev.x, prev.y, mid.x, mid.y)
        }

        val last = points.last()
        lineTo(last.x, last.y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = width,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

@Composable
private fun ToolButton(
    icon: Any,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.background(
            if (selected) Color(0xFF2E7D32) else Color.Transparent,
            RoundedCornerShape(10.dp)
        )
    ) {
        when (icon) {
            is Int -> {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    tint = if (selected) Color.White else Color.LightGray
                )
            }

            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else Color.LightGray
                )
            }

            is String -> {
                Text(
                    text = icon,
                    color = if (selected) Color.White else Color.LightGray
                )
            }
        }
    }
}

@Composable
private fun EraserCursor(
    positionProvider: () -> Offset?,
    isErasing: Boolean,
    icon: Any
) {
    val pos = positionProvider() ?: return
    val isDark = isSystemInDarkTheme()

    val cursorSize = if (isErasing) 38.dp else 32.dp

    Box(
        modifier = Modifier
            .offset {
                val px = cursorSize.toPx()
                androidx.compose.ui.unit.IntOffset(
                    (pos.x - px / 2f).toInt(),
                    (pos.y - px / 2f).toInt()
                )
            }
            .size(cursorSize)
            .background(
                if (isDark) Color.DarkGray.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.9f),
                CircleShape
            )
            .zIndex(50f)
    ) {
        val tint = if (isDark) Color.White else Color.Black
        when (icon) {
            is Int -> {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = tint
                )
            }

            is ImageVector -> {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center),
                    tint = tint
                )
            }

            is String -> {
                Text(
                    text = icon,
                    modifier = Modifier.align(Alignment.Center),
                    color = tint
                )
            }
        }
    }
}

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
    val showSettings: Boolean = true
) {
    companion object {
        val Default = SketchToolbarOptions()
    }
}

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
