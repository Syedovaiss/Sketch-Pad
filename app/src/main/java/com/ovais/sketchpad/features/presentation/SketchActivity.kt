package com.ovais.sketchpad.features.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import com.ovais.sketch_pad.pad.data.SketchToolbarOptions
import com.ovais.sketch_pad.pad.presentation.SketchPad
import com.ovais.sketchpad.core.ui.theme.SketchPadTheme
import com.ovais.sketchpad.utils.saveFileToDownloads
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
    val controller = viewModel.controller
    val context = LocalContext.current

    SketchPad(
        controller = controller,
        toolbarOptions = SketchToolbarOptions(),
        onClear = {
            viewModel.clearDraft()
            controller.newSketch()
        },
        onSave = {
            viewModel.saveDraft(it)
        },
        onDownloadFile = { file, type ->
            saveFileToDownloads(context, file, type)
        },
        onDownloadImage = { imageBitmap ->
            saveImageToGallery(context, imageBitmap.asAndroidBitmap())
        }
    )
}
