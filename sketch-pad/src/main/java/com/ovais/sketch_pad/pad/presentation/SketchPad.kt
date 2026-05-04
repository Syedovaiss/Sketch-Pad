package com.ovais.sketch_pad.pad.presentation

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalConfiguration
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.ovais.sketch_pad.R
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.EraseType
import com.ovais.sketch_pad.pad.data.CanvasSize
import com.ovais.sketch_pad.pad.data.SketchFileType
import com.ovais.sketch_pad.pad.data.SketchPadIcons
import com.ovais.sketch_pad.pad.data.SketchPoint
import com.ovais.sketch_pad.pad.data.SketchOrientation
import com.ovais.sketch_pad.pad.data.SketchStrokePersistenceFormat
import com.ovais.sketch_pad.pad.data.SketchToolbarOptions
import com.ovais.sketch_pad.pad.data.ToolMode
import com.ovais.sketch_pad.pad.domain.SketchController
import com.ovais.sketch_pad.utils.SketchExporter
import com.ovais.sketch_pad.utils.encodeStrokesForPersistenceFormat
import com.ovais.sketch_pad.utils.toArgbLong
import com.ovais.sketch_pad.utils.toColor
import java.io.File
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * A professional-grade, reusable SketchPad component for drawing and erasing with infinite canvas support.
 */
@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun SketchPad(
    modifier: Modifier = Modifier,
    controller: SketchController = remember { SketchController() },
    callbacks: SketchPadCallbacks = object : SketchPadCallbacks {},
    backgroundColor: Color = Color.Unspecified,
    toolbarSelectionColor: Color = Color(0xFF2E7D32),
    toolbarBackgroundColor: Color = Color.Unspecified,
    toolbarIconTint: Color = Color.Unspecified,
    toolbarTextColor: Color = Color.Unspecified,
    toolbarSelectedIconTint: Color = Color.Unspecified,
    exportImageBackgroundColor: Color = Color.Unspecified,
    orientation: SketchOrientation = SketchOrientation.Landscape,
    exportOrientation: SketchOrientation = SketchOrientation.Auto,
    syncActivityOrientation: Boolean = false,
    showToolbar: Boolean = true,
    toolbarOptions: SketchToolbarOptions = SketchToolbarOptions.Default,
    icons: SketchPadIcons = SketchPadIcons.Default,
    gridEnabled: Boolean = true,
    gridSize: Float = 70f,
    gridColor: Color = Color.Unspecified,
    canvasSize: CanvasSize = CanvasSize.Free,
    minZoom: Float = 0.5f,
    maxZoom: Float = 4f,
    exportPadding: Float = 50f,
    maxExportPixels: Long = 40_000_000L,
    /**
     * When non-null, [controller.eraseType] is kept in sync with this value (e.g. hoisted state).
     * Use with [onEraseTypeChange] for a fully controlled erase mode from the parent composable.
     */
    eraseType: EraseType? = null,
    /** Called when the user changes erase mode in settings (and after [controller] is updated). */
    onEraseTypeChange: ((EraseType) -> Unit)? = null,
    /**
     * Toolbar save also invokes [SketchPadCallbacks.onSavePersistencePayload] with an encoded string.
     * Defaults to [SketchStrokePersistenceFormat.Base64] (same as [com.ovais.sketch_pad.utils.encodeStrokesToBase64]).
     * Use [SketchStrokePersistenceFormat.None] to disable the extra callback and only use [SketchPadCallbacks.onSave].
     */
    strokePersistenceFormat: SketchStrokePersistenceFormat = SketchStrokePersistenceFormat.Base64
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDark = isSystemInDarkTheme()
    val density = androidx.compose.ui.platform.LocalDensity.current.density

    val configuration = LocalConfiguration.current
    val systemIsLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    var toolbarOrientation by remember(orientation, systemIsLandscape) {
        mutableStateOf(
            when (orientation) {
                SketchOrientation.Auto ->
                    if (systemIsLandscape) SketchOrientation.Landscape else SketchOrientation.Portrait
                SketchOrientation.Portrait -> SketchOrientation.Portrait
                SketchOrientation.Landscape -> SketchOrientation.Landscape
            }
        )
    }

    val effectiveOrientation = toolbarOrientation
    val effectiveExportOrientation = when (exportOrientation) {
        SketchOrientation.Auto -> effectiveOrientation
        SketchOrientation.Portrait -> SketchOrientation.Portrait
        SketchOrientation.Landscape -> SketchOrientation.Landscape
    }

    val toggleOrientationFromToolbar = {
        val nextOrientation = if (effectiveOrientation == SketchOrientation.Portrait) {
            SketchOrientation.Landscape
        } else {
            SketchOrientation.Portrait
        }
        callbacks.onOrientationToggleRequested(nextOrientation)
        toolbarOrientation = nextOrientation
    }

    LaunchedEffect(effectiveOrientation) {
        callbacks.onOrientationChanged(effectiveOrientation)
    }

    val targetActivityOrientation = when (orientation) {
        SketchOrientation.Portrait -> SketchOrientation.Portrait
        SketchOrientation.Landscape -> SketchOrientation.Landscape
        SketchOrientation.Auto -> effectiveOrientation
    }

    LaunchedEffect(syncActivityOrientation, targetActivityOrientation) {
        if (!syncActivityOrientation) return@LaunchedEffect
        val activity = context.findComponentActivity() ?: return@LaunchedEffect
        activity.requestedOrientation = when (targetActivityOrientation) {
            SketchOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            SketchOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            SketchOrientation.Auto -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val finalBackgroundColor = if (backgroundColor == Color.Unspecified) {
        if (isDark) Color(0xFF1C1B1F) else Color.White
    } else backgroundColor

    val finalGridColor = if (gridColor == Color.Unspecified) {
        if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.05f)
    } else gridColor

    val finalExportBackgroundColor = if (exportImageBackgroundColor == Color.Unspecified) {
        finalBackgroundColor
    } else {
        exportImageBackgroundColor
    }

    val canvasStartInset = if (effectiveOrientation == SketchOrientation.Landscape) 90.dp else 0.dp

    LaunchedEffect(gridEnabled) {
        controller.isGridEnabled = gridEnabled
    }

    LaunchedEffect(eraseType) {
        if (eraseType != null) {
            controller.eraseType = eraseType
        }
    }

    val applyEraseTypeFromUi: (EraseType) -> Unit = { newType ->
        controller.eraseType = newType
        onEraseTypeChange?.invoke(newType)
    }

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

    var toolbarExpanded by remember { mutableStateOf(false) }
    var landscapeToolbarCollapsed by remember(effectiveOrientation) {
        mutableStateOf(effectiveOrientation == SketchOrientation.Landscape)
    }
    var isPointerDown by remember { mutableStateOf(false) }
    var isErasing by remember { mutableStateOf(false) }
    var eraserPosition by remember { mutableStateOf<Offset?>(null) }

    var showColorPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val activePoints = remember { mutableStateListOf<SketchPoint>() }

    val clampedMinZoom = minZoom.coerceAtLeast(0.1f)
    val clampedMaxZoom = maxZoom.coerceAtLeast(clampedMinZoom)

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        controller.scale = (controller.scale * zoomChange).coerceIn(clampedMinZoom, clampedMaxZoom)
        controller.translation += offsetChange
    }

    if (showExportDialog) {
        Dialog(onDismissRequest = { showExportDialog = false }) {
            Column(
                modifier = Modifier
                    .background(
                        if (isDark) Color(0xFF2C2C2C) else Color.White,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Export Sketch",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDark) Color.White else Color.Black
                )

                SketchFileType.entries.forEach { type ->
                    TextButton(
                        onClick = {
                            showExportDialog = false
                            val file = SketchExporter.exportToFile(
                                context = context,
                                strokes = controller.strokes,
                                canvasSize = canvasSize,
                                fileType = type,
                                backgroundColor = finalExportBackgroundColor,
                                includeGrid = controller.isGridEnabled,
                                gridSize = gridSize,
                                gridColor = finalGridColor
                            )
                            callbacks.onDownloadFile(file, type)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(type.label)
                    }
                }

                TextButton(
                    onClick = { showExportDialog = false },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    if (showColorPicker) {
        Dialog(onDismissRequest = { showColorPicker = false }) {
            Box(
                modifier = Modifier
                    .size(320.dp)
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(start = canvasStartInset)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = controller.translation.x
                        translationY = controller.translation.y
                        scaleX = controller.scale
                        scaleY = controller.scale
                    }
                    .transformable(state = transformState)
                    .pointerInput(toolMode, controller.eraseType) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                isPointerDown = true
                                val model = (pos - controller.translation) / controller.scale
                                if (toolMode == ToolMode.ERASE) {
                                    isErasing = true
                                    eraserPosition = pos
                                    controller.eraseAt(model.x, model.y)
                                } else if (toolMode == ToolMode.DRAW) {
                                    activePoints.clear()
                                    activePoints.add(SketchPoint(model.x, model.y))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                val pos = change.position
                                change.consume()
                                if (!isPointerDown) return@detectDragGestures
                                if (toolMode == ToolMode.MOVE) {
                                    controller.translation += dragAmount
                                    return@detectDragGestures
                                }
                                val model = (pos - controller.translation) / controller.scale
                                if (toolMode == ToolMode.ERASE) {
                                    eraserPosition = pos
                                    controller.eraseAt(model.x, model.y)
                                } else if (toolMode == ToolMode.DRAW) {
                                    val last = activePoints.lastOrNull()
                                    if (last == null || hypot(
                                            (model.x - last.x).toDouble(),
                                            (model.y - last.y).toDouble()
                                        ) > (1.0 / controller.scale)
                                    ) {
                                        activePoints.add(SketchPoint(model.x, model.y))
                                    }
                                }
                            },
                            onDragEnd = {
                                isPointerDown = false
                                isErasing = false
                                eraserPosition = null
                                if (controller.toolMode == ToolMode.DRAW && activePoints.isNotEmpty()) {
                                    controller.add(
                                        ActiveStroke(
                                            points = activePoints.toList(),
                                            color = controller.brushColor.toArgbLong(),
                                            strokeWidth = controller.brushWidth
                                        )
                                    )
                                }
                                activePoints.clear()
                            }
                        )
                    }
            ) {
            if (controller.isGridEnabled) {
                // Draw grid without transformation or with inverse transformation if needed
                // But grid usually should be fixed or move with the canvas.
                // If it moves with canvas, it should be inside the transformed scope.
                // However, drawGrid implementation currently uses translation and scale.
                // Since we use graphicsLayer, we should draw grid in a way that respects it.
                // Actually, if we want the grid to move/scale with content, drawing it here (transformed by graphicsLayer) is correct.
                // But we need to adjust drawGrid to NOT take translation/scale if it's already in a graphicsLayer.
                // Let's keep it simple: draw it in a non-transformed way if we want infinite grid, 
                // or just draw it here and it will be scaled/translated.
                drawGrid(Offset.Zero, 1f, finalGridColor, gridSize)
            }

            // Paper Background
            val paperSize = when (canvasSize) {
                is CanvasSize.A4 -> canvasSize.size
                is CanvasSize.A3 -> canvasSize.size
                is CanvasSize.Custom -> canvasSize.size
                is CanvasSize.Screen -> size
                is CanvasSize.Free -> null
            }

            paperSize?.let {
                drawRect(
                    color = if (isDark) Color(0xFF2C2C2C) else Color.White,
                    size = it
                )
                drawRect(
                    color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(
                        alpha = 0.2f
                    ),
                    size = it,
                    style = Stroke(width = 2f / controller.scale)
                )
            }

            controller.strokes.forEach { stroke ->
                var strokeColor = stroke.color.toColor()
                // Adjust visibility for white/black strokes on contrasting backgrounds
                if (isDark) {
                    if (strokeColor == Color.Black) strokeColor = Color.White
                } else {
                    if (strokeColor == Color.White) strokeColor = Color.Black
                }

                drawPath(
                    path = controller.getPathFor(stroke),
                    color = strokeColor,
                    style = Stroke(
                        width = stroke.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }

            drawSmoothStroke(activePoints, brushColor, brushWidth)
            }

            EraserCursor(
                positionProvider = { eraserPosition },
                isErasing = isErasing,
                icon = icons.eraseIcon
            )
        }

        // Full-surface move layer so panning is not constrained to canvas draw bounds.
        if (toolMode == ToolMode.MOVE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            controller.translation += dragAmount
                        }
                    }
            )
        }

        if (showToolbar) {
            val toolbarBg = if (toolbarBackgroundColor != Color.Unspecified) {
                toolbarBackgroundColor
            } else if (isDark) {
                Color(0xFF2C2C2C)
            } else {
                Color(0xFF121212)
            }
            val resolvedToolbarTextColor = if (toolbarTextColor != Color.Unspecified) {
                toolbarTextColor
            } else if (toolbarBg.luminance() > 0.5f) {
                Color.Black
            } else {
                Color.White
            }
            val resolvedToolbarSelectedIconTint = if (toolbarSelectedIconTint != Color.Unspecified) {
                toolbarSelectedIconTint
            } else {
                Color.White
            }
            val toolbarAlignment = when (effectiveOrientation) {
                SketchOrientation.Portrait -> Alignment.TopCenter
                SketchOrientation.Landscape, SketchOrientation.Auto -> Alignment.TopStart
            }

            Column(
                modifier = Modifier
                    .align(toolbarAlignment)
                    .statusBarsPadding()
                    .padding(
                        start = 10.dp,
                        top = 10.dp,
                        end = 10.dp,
                        bottom = 10.dp
                    )
                    .background(toolbarBg, RoundedCornerShape(14.dp))
                    .padding(10.dp)
                    .zIndex(200f)
            ) {
                val isLandscapeLayout = effectiveOrientation == SketchOrientation.Landscape
                val iconSpacing = if (isLandscapeLayout) 6.dp else 10.dp

                if (isLandscapeLayout) {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(iconSpacing)
                    ) {
                        ToolButton(
                            icon = if (landscapeToolbarCollapsed) ">" else "<",
                            selected = false,
                            iconTint = toolbarIconTint
                        ) {
                            landscapeToolbarCollapsed = !landscapeToolbarCollapsed
                        }

                        if (landscapeToolbarCollapsed) {
                            return@Column
                        }

                        if (toolbarOptions.showMove) {
                            ToolButton(
                                icon = icons.moveIcon,
                                selected = toolMode == ToolMode.MOVE,
                                selectedColor = toolbarSelectionColor,
                                iconTint = toolbarIconTint,
                                selectedIconTint = resolvedToolbarSelectedIconTint
                            ) {
                                controller.toolMode = ToolMode.MOVE
                                isErasing = false
                                eraserPosition = null
                            }
                        }
                        if (toolbarOptions.showDraw) {
                            ToolButton(
                                icon = icons.drawIcon,
                                selected = toolMode == ToolMode.DRAW,
                                selectedColor = toolbarSelectionColor,
                                iconTint = toolbarIconTint,
                                selectedIconTint = resolvedToolbarSelectedIconTint
                            ) {
                                controller.toolMode = ToolMode.DRAW
                                isErasing = false
                                eraserPosition = null
                            }
                        }
                        if (toolbarOptions.showErase) {
                            ToolButton(
                                icon = icons.eraseIcon,
                                selected = toolMode == ToolMode.ERASE,
                                selectedColor = toolbarSelectionColor,
                                iconTint = toolbarIconTint,
                                selectedIconTint = resolvedToolbarSelectedIconTint
                            ) {
                                controller.toolMode = ToolMode.ERASE
                                isErasing = false
                                eraserPosition = null
                            }
                        }
                        if (toolbarOptions.showUndo) {
                            ToolButton(
                                icon = icons.undoIcon,
                                selected = false,
                                enabled = controller.canUndo()
                            ) {
                                controller.undo()
                            }
                        }
                        if (toolbarOptions.showRedo) {
                            ToolButton(
                                icon = icons.redoIcon,
                                selected = false,
                                enabled = controller.canRedo()
                            ) {
                                controller.redo()
                            }
                        }
                        if (toolbarOptions.showClear) {
                            ToolButton(icon = icons.clearIcon, selected = false) {
                                callbacks.onClearRequested {
                                    controller.clear()
                                    callbacks.onClear()
                                }
                            }
                        }
                        if (toolbarOptions.showSave) {
                            ToolButton(icon = icons.saveIcon, selected = false) {
                                dispatchToolbarSave(
                                    controller.strokes,
                                    controller,
                                    callbacks,
                                    strokePersistenceFormat
                                )
                            }
                        }
                        if (toolbarOptions.showDownloadFile) {
                            ToolButton(icon = icons.downloadFile, selected = false) {
                                showExportDialog = true
                            }
                        }
                        if (toolbarOptions.showDownloadImage) {
                            ToolButton(icon = icons.downloadImage, selected = false) {
                                val paperSizeLimit = when (canvasSize) {
                                    is CanvasSize.A4 -> canvasSize.size
                                    is CanvasSize.A3 -> canvasSize.size
                                    is CanvasSize.Custom -> canvasSize.size
                                    else -> null
                                }

                                val exportSpec = if (paperSizeLimit == null) {
                                    calculateStrokeExportSpec(
                                        strokes = controller.strokes,
                                        padding = exportPadding,
                                        fallbackWidth = if (effectiveOrientation == SketchOrientation.Landscape) 1920 else 1080,
                                        fallbackHeight = if (effectiveOrientation == SketchOrientation.Landscape) 1080 else 1920
                                    ).withOrientation(effectiveOrientation)
                                } else {
                                    null
                                }

                                val bitmap = generateBitmap(
                                    strokes = controller.strokes,
                                    width = paperSizeLimit?.width?.toInt() ?: exportSpec!!.width,
                                    height = paperSizeLimit?.height?.toInt() ?: exportSpec!!.height,
                                backgroundColor = finalExportBackgroundColor,
                                    gridEnabled = controller.isGridEnabled,
                                    gridColor = finalGridColor,
                                    gridSize = gridSize,
                                    translation = exportSpec?.translation ?: Offset.Zero,
                                    density = density,
                                    canvasSize = canvasSize,
                                exportOrientation = effectiveExportOrientation,
                                    maxExportPixels = maxExportPixels
                                )
                                callbacks.onDownloadImage(bitmap)
                            }
                        }
                        if (toolbarOptions.showOrientation) {
                            ToolButton(
                                icon = icons.orientationIcon,
                                selected = false,
                                iconTint = toolbarIconTint
                            ) {
                                toggleOrientationFromToolbar()
                            }
                        }
                        if (toolbarOptions.showSettings) {
                            ToolButton(icon = icons.settingsIcon, selected = toolbarExpanded) {
                                toolbarExpanded = !toolbarExpanded
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(iconSpacing)
                    ) {
                        if (toolbarOptions.showMove) {
                            ToolButton(
                                icon = icons.moveIcon,
                                selected = toolMode == ToolMode.MOVE,
                                selectedColor = toolbarSelectionColor,
                                iconTint = toolbarIconTint,
                                selectedIconTint = resolvedToolbarSelectedIconTint
                            ) {
                                controller.toolMode = ToolMode.MOVE
                                isErasing = false
                                eraserPosition = null
                            }
                        }
                        if (toolbarOptions.showDraw) {
                            ToolButton(
                                icon = icons.drawIcon,
                                selected = toolMode == ToolMode.DRAW,
                                selectedColor = toolbarSelectionColor,
                                iconTint = toolbarIconTint,
                                selectedIconTint = resolvedToolbarSelectedIconTint
                            ) {
                                controller.toolMode = ToolMode.DRAW
                                isErasing = false
                                eraserPosition = null
                            }
                        }
                        if (toolbarOptions.showErase) {
                            ToolButton(
                                icon = icons.eraseIcon,
                                selected = toolMode == ToolMode.ERASE,
                                selectedColor = toolbarSelectionColor,
                                iconTint = toolbarIconTint,
                                selectedIconTint = resolvedToolbarSelectedIconTint
                            ) {
                                controller.toolMode = ToolMode.ERASE
                                isErasing = false
                                eraserPosition = null
                            }
                        }
                        if (toolbarOptions.showUndo) {
                            ToolButton(
                                icon = icons.undoIcon,
                                selected = false,
                                enabled = controller.canUndo()
                            ) {
                                controller.undo()
                            }
                        }
                        if (toolbarOptions.showRedo) {
                            ToolButton(
                                icon = icons.redoIcon,
                                selected = false,
                                enabled = controller.canRedo()
                            ) {
                                controller.redo()
                            }
                        }
                        if (toolbarOptions.showClear) {
                            ToolButton(icon = icons.clearIcon, selected = false) {
                                callbacks.onClearRequested {
                                    controller.clear()
                                    callbacks.onClear()
                                }
                            }
                        }
                        if (toolbarOptions.showSave) {
                            ToolButton(icon = icons.saveIcon, selected = false) {
                                dispatchToolbarSave(
                                    controller.strokes,
                                    controller,
                                    callbacks,
                                    strokePersistenceFormat
                                )
                            }
                        }
                        if (toolbarOptions.showDownloadFile) {
                            ToolButton(icon = icons.downloadFile, selected = false) {
                                showExportDialog = true
                            }
                        }
                        if (toolbarOptions.showDownloadImage) {
                            ToolButton(icon = icons.downloadImage, selected = false) {
                                val paperSizeLimit = when (canvasSize) {
                                    is CanvasSize.A4 -> canvasSize.size
                                    is CanvasSize.A3 -> canvasSize.size
                                    is CanvasSize.Custom -> canvasSize.size
                                    else -> null
                                }

                                val exportSpec = if (paperSizeLimit == null) {
                                    calculateStrokeExportSpec(
                                        strokes = controller.strokes,
                                        padding = exportPadding,
                                        fallbackWidth = if (effectiveOrientation == SketchOrientation.Landscape) 1920 else 1080,
                                        fallbackHeight = if (effectiveOrientation == SketchOrientation.Landscape) 1080 else 1920
                                    ).withOrientation(effectiveOrientation)
                                } else {
                                    null
                                }

                                val bitmap = generateBitmap(
                                    strokes = controller.strokes,
                                    width = paperSizeLimit?.width?.toInt() ?: exportSpec!!.width,
                                    height = paperSizeLimit?.height?.toInt() ?: exportSpec!!.height,
                                    backgroundColor = finalExportBackgroundColor,
                                    gridEnabled = controller.isGridEnabled,
                                    gridColor = finalGridColor,
                                    gridSize = gridSize,
                                    translation = exportSpec?.translation ?: Offset.Zero,
                                    density = density,
                                    canvasSize = canvasSize,
                                    exportOrientation = effectiveExportOrientation,
                                    maxExportPixels = maxExportPixels
                                )
                                callbacks.onDownloadImage(bitmap)
                            }
                        }
                        if (toolbarOptions.showOrientation) {
                            ToolButton(
                                icon = icons.orientationIcon,
                                selected = false,
                                iconTint = toolbarIconTint
                            ) {
                                toggleOrientationFromToolbar()
                            }
                        }
                        if (toolbarOptions.showSettings) {
                            ToolButton(icon = icons.settingsIcon, selected = toolbarExpanded) {
                                toolbarExpanded = !toolbarExpanded
                            }
                        }
                    }
                }

                if (toolbarExpanded) {
                    if (isLandscapeLayout) {
                        val dialogTextColor = if (isDark) Color.White else Color.Black
                        Dialog(onDismissRequest = { toolbarExpanded = false }) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .heightIn(max = configuration.screenHeightDp.dp * 0.9f)
                                    .verticalScroll(rememberScrollState())
                                    .background(
                                        if (isDark) Color(0xFF2C2C2C) else Color.White,
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Toolbar Settings",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = dialogTextColor
                                )
                                ToolbarSettingsContent(
                                    isDark = isDark,
                                    toolbarOptions = toolbarOptions,
                                    brushColor = brushColor,
                                    brushWidth = brushWidth,
                                    isGridEnabled = controller.isGridEnabled,
                                    textColor = dialogTextColor,
                                    onBrushColorSelected = { controller.brushColor = it },
                                    onOpenColorPicker = { showColorPicker = true },
                                    onBrushWidthChanged = { controller.brushWidth = it },
                                    onGridEnabledChanged = { controller.isGridEnabled = it },
                                    eraseType = controller.eraseType,
                                    onEraseTypeChange = applyEraseTypeFromUi
                                )
                                TextButton(
                                    onClick = { toolbarExpanded = false },
                                    modifier = Modifier.align(Alignment.End)
                                ) { Text("Done") }
                            }
                        }
                    } else {
                        ToolbarSettingsContent(
                            isDark = isDark,
                            toolbarOptions = toolbarOptions,
                            brushColor = brushColor,
                            brushWidth = brushWidth,
                            isGridEnabled = controller.isGridEnabled,
                            textColor = resolvedToolbarTextColor,
                            onBrushColorSelected = { controller.brushColor = it },
                            onOpenColorPicker = { showColorPicker = true },
                            onBrushWidthChanged = { controller.brushWidth = it },
                            onGridEnabledChanged = { controller.isGridEnabled = it },
                            eraseType = controller.eraseType,
                            onEraseTypeChange = applyEraseTypeFromUi
                        )
                    }
                }
            }
        }
    }
}

private fun dispatchToolbarSave(
    strokes: List<ActiveStroke>,
    controller: SketchController,
    callbacks: SketchPadCallbacks,
    strokePersistenceFormat: SketchStrokePersistenceFormat
) {
    callbacks.onSave(strokes)
    if (strokePersistenceFormat == SketchStrokePersistenceFormat.None) return
    val payload = encodeStrokesForPersistenceFormat(
        strokes,
        strokePersistenceFormat,
        controller.sketchId
    ) ?: return
    callbacks.onSavePersistencePayload(strokes, payload, strokePersistenceFormat)
}

private tailrec fun Context.findComponentActivity(): androidx.activity.ComponentActivity? {
    return when (this) {
        is androidx.activity.ComponentActivity -> this
        is ContextWrapper -> baseContext.findComponentActivity()
        else -> null
    }
}

@Composable
private fun ToolbarSettingsContent(
    isDark: Boolean,
    toolbarOptions: SketchToolbarOptions,
    brushColor: Color,
    brushWidth: Float,
    isGridEnabled: Boolean,
    textColor: Color,
    onBrushColorSelected: (Color) -> Unit,
    onOpenColorPicker: () -> Unit,
    onBrushWidthChanged: (Float) -> Unit,
    onGridEnabledChanged: (Boolean) -> Unit,
    eraseType: EraseType,
    onEraseTypeChange: (EraseType) -> Unit
) {
    var areaRadiusFallback by remember { mutableStateOf(24f) }
    LaunchedEffect(eraseType) {
        if (eraseType is EraseType.Area) {
            areaRadiusFallback = eraseType.radius
        }
    }
    if (toolbarOptions.showColorPalette) {
        Spacer(Modifier.height(6.dp))
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
                            detectTapGestures { onBrushColorSelected(color) }
                        }
                )
            }
            Box(
                Modifier
                    .size(34.dp)
                    .background(brushColor, CircleShape)
                    .border(
                        2.dp,
                        if (isDark) Color.White else Color.Black,
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { onOpenColorPicker() }
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
    }

    if (toolbarOptions.showBrushSize) {
        Spacer(Modifier.height(6.dp))
        Text("Brush Size: ${brushWidth.toInt()}", color = textColor)
        Slider(
            value = brushWidth,
            onValueChange = onBrushWidthChanged,
            valueRange = 2f..40f
        )
    }

    if (toolbarOptions.showEraseMode) {
        Spacer(Modifier.height(6.dp))
        val isAreaMode = eraseType is EraseType.Area
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Area eraser (partial)", color = textColor)
            Switch(
                checked = isAreaMode,
                onCheckedChange = { useArea ->
                    onEraseTypeChange(
                        if (useArea) EraseType.Area(areaRadiusFallback) else EraseType.WholeStroke
                    )
                }
            )
        }
        if (isAreaMode) {
            val radius = (eraseType as EraseType.Area).radius
            Spacer(Modifier.height(4.dp))
            Text("Eraser radius: ${radius.toInt()}", color = textColor)
            Slider(
                value = radius,
                onValueChange = { v ->
                    areaRadiusFallback = v
                    onEraseTypeChange(EraseType.Area(v))
                },
                valueRange = 8f..56f
            )
        }
    }

    if (toolbarOptions.showGridToggle) {
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Show Grid", color = textColor)
            Switch(
                checked = isGridEnabled,
                onCheckedChange = onGridEnabledChanged
            )
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
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            val prev = points[i - 1]
            val curr = points[i]
            val mid = Offset((prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
            quadraticTo(prev.x, prev.y, mid.x, mid.y)
        }
        lineTo(points.last().x, points.last().y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawGrid(
    translation: Offset,
    scale: Float,
    gridColor: Color,
    gridSizeBase: Float = 50f
) {
    val gridSize = gridSizeBase * scale
    val startX = translation.x % gridSize
    val startY = translation.y % gridSize
    var x = startX
    while (x < size.width) {
        drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += gridSize
    }
    var y = startY
    while (y < size.height) {
        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += gridSize
    }
}

private data class StrokeExportSpec(
    val width: Int,
    val height: Int,
    val translation: Offset
)

private fun StrokeExportSpec.withOrientation(orientation: SketchOrientation): StrokeExportSpec {
    return when (orientation) {
        SketchOrientation.Landscape -> {
            if (width >= height) this else copy(width = height)
        }
        SketchOrientation.Portrait -> {
            if (height >= width) this else copy(height = width)
        }
        SketchOrientation.Auto -> this
    }
}

private fun calculateStrokeExportSpec(
    strokes: List<ActiveStroke>,
    padding: Float = 50f,
    fallbackWidth: Int = 1080,
    fallbackHeight: Int = 1920
): StrokeExportSpec {
    if (strokes.isEmpty()) {
        return StrokeExportSpec(
            width = fallbackWidth,
            height = fallbackHeight,
            translation = Offset.Zero
        )
    }

    var minX = Float.POSITIVE_INFINITY
    var minY = Float.POSITIVE_INFINITY
    var maxX = Float.NEGATIVE_INFINITY
    var maxY = Float.NEGATIVE_INFINITY

    strokes.forEach { stroke ->
        stroke.points.forEach { point ->
            minX = min(minX, point.x)
            minY = min(minY, point.y)
            maxX = max(maxX, point.x)
            maxY = max(maxY, point.y)
        }
    }

    if (!minX.isFinite() || !minY.isFinite() || !maxX.isFinite() || !maxY.isFinite()) {
        return StrokeExportSpec(
            width = fallbackWidth,
            height = fallbackHeight,
            translation = Offset.Zero
        )
    }

    val width = (maxX - minX + 2f * padding).toInt().coerceAtLeast(1)
    val height = (maxY - minY + 2f * padding).toInt().coerceAtLeast(1)
    val translation = Offset(-minX + padding, -minY + padding)

    return StrokeExportSpec(width = width, height = height, translation = translation)
}

fun generateBitmap(
    strokes: List<ActiveStroke>,
    width: Int? = null,
    height: Int? = null,
    backgroundColor: Color = Color.White,
    gridEnabled: Boolean = false,
    gridSize: Float = 50f,
    gridColor: Color = Color.Black.copy(alpha = 0.05f),
    translation: Offset = Offset.Zero,
    scale: Float = 1f,
    density: Float = 1f,
    canvasSize: CanvasSize = CanvasSize.Free,
    exportOrientation: SketchOrientation = SketchOrientation.Auto,
    maxExportPixels: Long = 40_000_000L
): ImageBitmap {
    val maxBitmapDimension = 8192
    val clampedMaxExportPixels = maxExportPixels.coerceAtLeast(1L)
    val fixedCanvasSize = when (canvasSize) {
        is CanvasSize.A4 -> canvasSize.size
        is CanvasSize.A3 -> canvasSize.size
        is CanvasSize.Custom -> canvasSize.size
        is CanvasSize.Screen, is CanvasSize.Free -> null
    }
    val exportSpec = if (fixedCanvasSize == null) calculateStrokeExportSpec(strokes) else null
    val resolvedWidth = width ?: fixedCanvasSize?.width?.toInt() ?: exportSpec!!.width
    val resolvedHeight = height ?: fixedCanvasSize?.height?.toInt() ?: exportSpec!!.height
    val resolvedTranslation = if (translation == Offset.Zero) {
        exportSpec?.translation ?: Offset.Zero
    } else {
        translation
    }
    val rawBaseWidth = resolvedWidth.coerceAtLeast(1)
    val rawBaseHeight = resolvedHeight.coerceAtLeast(1)

    val (baseWidth, baseHeight) = when (exportOrientation) {
        SketchOrientation.Portrait -> {
            if (rawBaseWidth > rawBaseHeight) rawBaseWidth to rawBaseWidth else rawBaseWidth to rawBaseHeight
        }
        SketchOrientation.Landscape -> {
            if (rawBaseHeight > rawBaseWidth) rawBaseHeight to rawBaseHeight else rawBaseWidth to rawBaseHeight
        }
        SketchOrientation.Auto -> rawBaseWidth to rawBaseHeight
    }

    val orientationAdjustedTranslation = when (exportOrientation) {
        SketchOrientation.Portrait -> {
            if (rawBaseWidth > rawBaseHeight) {
                resolvedTranslation + Offset(0f, (rawBaseWidth - rawBaseHeight) / 2f)
            } else {
                resolvedTranslation
            }
        }
        SketchOrientation.Landscape -> {
            if (rawBaseHeight > rawBaseWidth) {
                resolvedTranslation + Offset((rawBaseHeight - rawBaseWidth) / 2f, 0f)
            } else {
                resolvedTranslation
            }
        }
        SketchOrientation.Auto -> resolvedTranslation
    }
    val downscaleByDimension = min(
        maxBitmapDimension.toFloat() / baseWidth.toFloat(),
        maxBitmapDimension.toFloat() / baseHeight.toFloat()
    ).coerceAtMost(1f)
    val currentPixels = baseWidth.toLong() * baseHeight.toLong()
    val downscaleByPixels = if (currentPixels > clampedMaxExportPixels) {
        sqrt(clampedMaxExportPixels.toDouble() / currentPixels.toDouble()).toFloat()
    } else {
        1f
    }
    val outputScaleFactor = min(downscaleByDimension, downscaleByPixels).coerceAtMost(1f)
    val finalWidth = (baseWidth * outputScaleFactor).toInt().coerceAtLeast(1)
    val finalHeight = (baseHeight * outputScaleFactor).toInt().coerceAtLeast(1)
    val effectiveScale = scale * outputScaleFactor
    val effectiveTranslation = orientationAdjustedTranslation * outputScaleFactor

    val bitmap = ImageBitmap(finalWidth, finalHeight)
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val canvasDrawScope = CanvasDrawScope()

    canvasDrawScope.draw(
        density = Density(density),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = Size(finalWidth.toFloat(), finalHeight.toFloat())
    ) {
        drawRect(backgroundColor)
        if (gridEnabled) {
            drawGrid(effectiveTranslation, effectiveScale, gridColor, gridSize)
        }
        withTransform({
            translate(effectiveTranslation.x, effectiveTranslation.y)
            scale(effectiveScale, effectiveScale, Offset.Zero)
        }) {
            val paperSize = when (canvasSize) {
                is CanvasSize.A4 -> canvasSize.size
                is CanvasSize.A3 -> canvasSize.size
                is CanvasSize.Custom -> canvasSize.size
                is CanvasSize.Screen -> size
                is CanvasSize.Free -> null
            }
            paperSize?.let { drawRect(color = Color.White, size = it) }
            strokes.forEach { stroke ->
                drawSmoothStroke(
                    points = stroke.points,
                    color = stroke.color.toColor(),
                    width = stroke.strokeWidth
                )
            }
        }
    }
    return bitmap
}

@Composable
private fun ToolButton(
    icon: Any,
    selected: Boolean,
    selectedColor: Color = Color(0xFF2E7D32),
    iconTint: Color = Color.Unspecified,
    selectedIconTint: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.background(
            if (selected) selectedColor else Color.Transparent,
            RoundedCornerShape(10.dp)
        )
    ) {
        val resolvedIconTint = if (iconTint != Color.Unspecified) iconTint else Color.LightGray
        val tint = if (selected) selectedIconTint else if (enabled) resolvedIconTint else Color.DarkGray
        when (icon) {
            is Int -> Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                tint = tint
            )

            is ImageVector -> Icon(imageVector = icon, contentDescription = null, tint = tint)
            is String -> Text(text = icon, color = tint)
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
            is Int -> Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = tint
            )

            is ImageVector -> Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.align(Alignment.Center),
                tint = tint
            )

            is String -> Text(
                text = icon,
                modifier = Modifier.align(Alignment.Center),
                color = tint
            )
        }
    }
}
