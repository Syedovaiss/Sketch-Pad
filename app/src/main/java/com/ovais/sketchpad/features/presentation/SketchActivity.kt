package com.ovais.sketchpad.features.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ovais.sketch_pad.pad.data.SketchToolbarOptions
import com.ovais.sketch_pad.pad.domain.SketchController
import com.ovais.sketch_pad.pad.presentation.SketchPad
import com.ovais.sketchpad.core.ui.theme.SketchPadTheme
import com.ovais.sketchpad.utils.exportAndSavePdf
import com.ovais.sketchpad.utils.exportImage
import com.ovais.sketchpad.utils.saveImageToGallery
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: SketchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SketchPadTheme {
                SketchScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun SketchScreen(
    viewModel: SketchViewModel
) {
    val controller = remember { SketchController() }
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state.strokes) {
        if (state.strokes.isNotEmpty() && controller.strokes.isEmpty()) {
            controller.setStrokes(state.strokes)
        }
    }

    SketchPad(
        controller = controller,
        toolbarOptions = SketchToolbarOptions(
            showDownloadImage = false,
            showSettings = false
        ),
        onClear = {
            controller.clear()
        },
        onSave = {
            viewModel.saveDraft(it)
        },
        onDownloadFile = {
            exportAndSavePdf(context, it)
        },
        onDownloadImage = {
            val image = exportImage(it)
            saveImageToGallery(context, image)
        }
    )
}