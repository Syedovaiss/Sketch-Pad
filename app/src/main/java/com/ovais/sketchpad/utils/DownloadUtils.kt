package com.ovais.sketchpad.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ovais.sketch_pad.pad.data.SketchFileType
import java.io.File

fun saveImageToGallery(context: Context, bitmap: Bitmap) {
    val resolver = context.contentResolver

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "sketch_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SketchPad")
    }

    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

    uri?.let {
        resolver.openOutputStream(it)?.use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
    }
}


fun saveFileToDownloads(context: Context, file: File, fileType: SketchFileType) {
    val mimeType = when (fileType) {
        SketchFileType.PDF -> "application/pdf"
        SketchFileType.SVG -> "image/svg+xml"
        SketchFileType.JSON -> "application/json"
        SketchFileType.TEXT -> "text/plain"
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/SketchPad")
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                file.inputStream().copyTo(out)
            }
        }
    } else {
        val downloadsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(downloadsDir, file.name)
        file.copyTo(destFile, overwrite = true)
    }
}