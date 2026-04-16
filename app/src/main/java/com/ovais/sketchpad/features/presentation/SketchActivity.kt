package com.ovais.sketchpad.features.presentation

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.CanvasSize
import com.ovais.sketch_pad.pad.data.SketchPadIcons
import com.ovais.sketch_pad.pad.data.SketchOrientation
import com.ovais.sketch_pad.pad.data.SketchToolbarOptions
import androidx.compose.foundation.layout.statusBarsPadding
import com.ovais.sketch_pad.pad.data.ToolMode
import com.ovais.sketch_pad.pad.presentation.SketchCanvas
import com.ovais.sketch_pad.pad.presentation.SketchPad
import com.ovais.sketch_pad.pad.presentation.SketchPadCallbacks
import com.ovais.sketch_pad.pad.presentation.generateBitmap
import com.ovais.sketch_pad.utils.decodeStrokesFromBase64
import com.ovais.sketch_pad.utils.encodeStrokesToBase64
import com.ovais.sketch_pad.utils.toBase64Png
import com.ovais.sketch_pad.utils.toPngDataUri
import com.ovais.sketchpad.core.ui.theme.SketchPadTheme
import com.ovais.sketchpad.utils.saveFileToDownloads
import com.ovais.sketchpad.utils.saveImageToGallery
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SketchViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SketchPadTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SketchScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SketchScreen(viewModel: SketchViewModel) {
    val controller = viewModel.controller
    val context = LocalContext.current
    var showTips by remember { mutableStateOf(false) }
    var isHeadlessMode by remember { mutableStateOf(false) }
    val currentCanvasSize by remember { mutableStateOf<CanvasSize>(CanvasSize.Free) }
    val sketchPadBackground = MaterialTheme.colorScheme.background
    var savedStrokesBase64 by remember { mutableStateOf<String?>(null) }
    var savedImageBase64DataUri by remember { mutableStateOf<String?>(null) }
    var savedPreviewBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var showDraftCardContent by remember { mutableStateOf(false) }

    // Toolbar Options with all features enabled
    val toolbarOptions = remember {
        SketchToolbarOptions(
            showMove = true,
            showDraw = true,
            showErase = true,
            showUndo = true,
            showRedo = true,
            showClear = true,
            showSave = true,
            showDownloadFile = true,
            showDownloadImage = true,
            showSettings = true,
            showColorPalette = true,
            showBrushSize = true,
            showOrientation = true
        )
    }

    // Custom Icons (using default but showing how to customize)
    val customIcons = remember { SketchPadIcons.Default }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isHeadlessMode) {
            // --- Headless SketchCanvas Sample ---
            // This demonstrates how to use the raw canvas component to build a completely custom UI
            Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Headless Mode", style = MaterialTheme.typography.titleMedium)
                    Row {
                        TextButton(onClick = { controller.toolMode = ToolMode.DRAW }) { Text("Draw") }
                        TextButton(onClick = { controller.toolMode = ToolMode.ERASE }) { Text("Erase") }
                        TextButton(onClick = { isHeadlessMode = false }) {
                            Text("Exit", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                SketchCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .background(Color.White, RoundedCornerShape(8.dp)),
                    controller = controller,
                    onStrokeEnded = { viewModel.saveDraft(controller.strokes) }
                )
            }
        } else {
            SketchPad(
                modifier = Modifier.fillMaxSize(),
                controller = controller,
                backgroundColor = sketchPadBackground,
                toolbarOptions = toolbarOptions,
                icons = customIcons,
                canvasSize = currentCanvasSize,
                callbacks = object : SketchPadCallbacks {
                    override fun onClear() {
                        viewModel.clearDraft()
                        controller.newSketch()
                    }

                    override fun onSave(strokes: List<ActiveStroke>) {
                        viewModel.saveDraft(strokes)
                        saveSketchArtifacts(
                            strokes = strokes,
                            background = sketchPadBackground,
                            onStrokesBase64 = { savedStrokesBase64 = it },
                            onImageBase64 = { savedImageBase64DataUri = it },
                            onPreview = { savedPreviewBitmap = it }
                        )
                    }

                    override fun onDownloadFile(file: java.io.File, type: com.ovais.sketch_pad.pad.data.SketchFileType) {
                        saveFileToDownloads(context, file, type)
                    }

                    override fun onDownloadImage(image: ImageBitmap) {
                        saveImageToGallery(context, image.asAndroidBitmap())
                    }

                    override fun onOrientationToggleRequested(targetOrientation: SketchOrientation) {
                        (context as? ComponentActivity)?.requestedOrientation = when (targetOrientation) {
                            SketchOrientation.Portrait -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                            SketchOrientation.Landscape -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            SketchOrientation.Auto -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        }
                    }
                }
            )
        }

        // Demo panel for save/restore + image/base64 preview
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 88.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDraftCardContent = !showDraftCardContent },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Draft",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (showDraftCardContent) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                        contentDescription = if (showDraftCardContent) "Collapse draft panel" else "Expand draft panel"
                    )
                }

                if (showDraftCardContent) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (savedStrokesBase64.isNullOrBlank()) {
                                    "Tap toolbar save to generate editable strokes + image base64"
                                } else {
                                    "Strokes: ${savedStrokesBase64!!.length} chars • Image URI: ${savedImageBase64DataUri?.length ?: 0} chars"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            TextButton(
                                enabled = !savedStrokesBase64.isNullOrBlank(),
                                onClick = {
                                    savedStrokesBase64?.let { encoded ->
                                        runCatching { decodeStrokesFromBase64(encoded) }
                                            .onSuccess { decoded ->
                                                controller.setStrokes(decoded)
                                            }
                                    }
                                }
                            ) {
                                Text("Restore & Continue Editing")
                            }
                        }
                        savedPreviewBitmap?.let { preview ->
                            Image(
                                bitmap = preview,
                                contentDescription = "Saved sketch preview",
                                modifier = Modifier
                                    .size(82.dp)
                                    .background(Color.White, RoundedCornerShape(10.dp))
                            )
                        }
                    }
                }
            }
        }

        // UX: Floating Tip Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
                .clickable { showTips = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Tips",
                tint = Color.White
            )
        }

        // UX: Tip Overlay
        AnimatedVisibility(
            visible = showTips,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            TipCard(
                onDismiss = { showTips = false },
                onTryHeadless = {
                    isHeadlessMode = true
                    showTips = false
                }
            )
        }
    }
}

@Composable
fun TipCard(onDismiss: () -> Unit, onTryHeadless: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Pro Tips 🎨",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row {
                    TextButton(onClick = onTryHeadless) {
                        Text("Try Headless")
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Got it")
                    }
                }
            }

            TipItem("🚀 Performance", "Zooming and panning are GPU-accelerated for a buttery smooth experience.")
            TipItem("💾 Auto-Save", "Your sketches are saved automatically to the local database.")
            TipItem("📐 Infinite Canvas", "Use 'Free' canvas size for unlimited space or select A4/A3 for printing.")
            TipItem("🧩 Headless Component", "You can use SketchCanvas directly (as seen in Headless Mode) to build your own custom toolbars and UI.")
            TipItem("🌓 Theme Sync", "White ink becomes black in light mode and vice versa automatically!")
            TipItem("📤 Multiple Formats", "Export your work as PDF, SVG, or high-res Images.")
        }
    }
}

@Composable
fun TipItem(title: String, description: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 18.sp
        )
    }
}

private fun saveSketchArtifacts(
    strokes: List<ActiveStroke>,
    background: Color,
    onStrokesBase64: (String) -> Unit,
    onImageBase64: (String) -> Unit,
    onPreview: (ImageBitmap) -> Unit
) {
    onStrokesBase64(encodeStrokesToBase64(strokes))
    val imageBitmap = generateBitmap(
        strokes = strokes,
        backgroundColor = background
    )
    onPreview(imageBitmap)
    val imageDataUri = imageBitmap.toBase64Png().toPngDataUri()
    onImageBase64(imageDataUri)
}
