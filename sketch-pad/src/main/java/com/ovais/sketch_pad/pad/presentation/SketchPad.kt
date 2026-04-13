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
    icons: SketchPadIcons = SketchPadIcons.Default,
    dialogs: SketchPadDialogs = SketchPadDialogs.Default,
    onSave: (List<ActiveStroke>) -> Unit = {},
    onDownloadFile: (List<ActiveStroke>) -> Unit = {},
    onDownloadImage: (List<ActiveStroke>) -> Unit = {},
) {
    val isDark = isSystemInDarkTheme()
    val finalBackgroundColor = if (backgroundColor == Color.Unspecified) {
        if (isDark) Color(0xFF1C1B1F) else Color.White
    } else backgroundColor

    var toolMode by remember { mutableStateOf(ToolMode.DRAW) }

    var brushColor by remember { mutableStateOf(if (isDark) Color.White else Color.Black) }
    var brushWidth by remember { mutableFloatStateOf(6f) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    var toolbarExpanded by remember { mutableStateOf(false) }

    var isPointerDown by remember { mutableStateOf(false) }

    var isErasing by remember { mutableStateOf(false) }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }

    var showClearDialog by remember { mutableStateOf(false) }
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
                val controller = rememberColorPickerController()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        controller = controller,
                        onColorChanged = { envelope ->
                            brushColor = envelope.color
                        }
                    )

                    TextButton(onClick = { showColorPicker = false }) {
                        Text("Done")
                    }
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(dialogs.clearTitle) },
            text = { Text(dialogs.clearMessage) },
            confirmButton = {
                TextButton(onClick = {
                    controller.clear()
                    showClearDialog = false
                }) {
                    Text(dialogs.confirmText)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(dialogs.dismissText)
                }
            }
        )
    }

    Box(
        modifier
            .fillMaxSize()
            .background(finalBackgroundColor)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .pointerInput(toolMode) {

                    detectDragGestures(
                        onDragStart = { pos ->
                            isPointerDown = true

                            if (toolMode == ToolMode.ERASE) {
                                isErasing = true
                                eraserPosition = pos
                                controller.eraseAt(pos.x, pos.y)
                            } else {
                                activePoints.clear()
                                activePoints.add(SketchPoint(pos.x, pos.y))
                            }
                        },

                        onDrag = { change, _ ->
                            val pos = change.position

                            if (!isPointerDown) return@detectDragGestures

                            if (toolMode == ToolMode.ERASE) {
                                eraserPosition = pos
                                controller.eraseAt(pos.x, pos.y)
                                return@detectDragGestures
                            }

                            val last = activePoints.lastOrNull()

                            if (last == null || hypot(
                                    (pos.x - last.x).toDouble(),
                                    (pos.y - last.y).toDouble()
                                ) > 1.5
                            ) {
                                activePoints.add(SketchPoint(pos.x, pos.y))
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

            controller.strokes.forEach { stroke ->
                drawSmoothStroke(stroke.points, stroke.color.toColor(), stroke.strokeWidth)
            }

            drawSmoothStroke(activePoints, brushColor, brushWidth)
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

                    ToolButton(icons.drawIcon, toolMode == ToolMode.DRAW) {
                        toolMode = ToolMode.DRAW
                        isErasing = false
                        eraserPosition = null
                    }

                    ToolButton(icons.eraseIcon, toolMode == ToolMode.ERASE) {
                        toolMode = ToolMode.ERASE
                        isErasing = false
                        eraserPosition = null
                    }

                    ToolButton(icons.undoIcon, false) { controller.undo() }
                    ToolButton(icons.redoIcon, false) { controller.redo() }

                    ToolButton(icons.clearIcon, false) { showClearDialog = true }

                    ToolButton(icons.saveIcon, false) { onSave(controller.strokes) }
                    ToolButton(icons.downloadFile, false) { onDownloadFile(controller.strokes) }
                    ToolButton(icons.downloadImage, false) { onDownloadImage(controller.strokes) }

                    ToolButton(icons.settingsIcon, toolbarExpanded) {
                        toolbarExpanded = !toolbarExpanded
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
                                            brushColor = color
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
                        onValueChange = { brushWidth = it },
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

    Box(
        modifier = Modifier
            .offset {
                androidx.compose.ui.unit.IntOffset(
                    pos.x.toInt(),
                    pos.y.toInt()
                )
            }
            .size(if (isErasing) 38.dp else 32.dp)
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

data class SketchPadIcons(
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

data class SketchPadDialogs(
    val clearTitle: String = "Clear Canvas",
    val clearMessage: String = "Are you sure you want to clear everything? This cannot be undone.",
    val confirmText: String = "Clear",
    val dismissText: String = "Cancel"
) {
    companion object {
        val Default = SketchPadDialogs()
    }
}
