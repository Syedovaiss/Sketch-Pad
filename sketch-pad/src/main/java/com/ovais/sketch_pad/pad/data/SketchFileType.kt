package com.ovais.sketch_pad.pad.data

/**
 * Supported file formats for exporting the sketch data.
 */
enum class SketchFileType(val extension: String, val label: String) {
    PDF("pdf", "Portable Document Format (PDF)"),
    SVG("svg", "Scalable Vector Graphics (SVG)"),
    JSON("json", "Raw Data (JSON)"),
    TEXT("txt", "Metadata (Text)")
}
