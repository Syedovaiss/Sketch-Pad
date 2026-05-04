package com.ovais.sketch_pad.utils

import android.util.Base64
import com.ovais.sketch_pad.pad.data.ActiveStroke
import com.ovais.sketch_pad.pad.data.SketchDocument
import com.ovais.sketch_pad.pad.data.SketchStrokePersistenceFormat
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

private const val GZIP_MAGIC_0: Byte = 0x1f
private const val GZIP_MAGIC_1: Byte = 0x8b.toByte()

/**
 * Encodes strokes as **plain UTF-8 JSON** (a JSON array of [ActiveStroke]).
 * Smaller than [encodeStrokesToBase64] for the same data and suitable for DB `TEXT`, file, or HTTP body.
 */
fun encodeStrokesToJson(strokes: List<ActiveStroke>): String =
    SketchJson.encodeToString(strokes)

/**
 * Decodes a JSON array of strokes produced by [encodeStrokesToJson].
 */
fun decodeStrokesFromJson(json: String): List<ActiveStroke> =
    SketchJson.decodeFromString(json)

/**
 * Encodes a [SketchDocument] as plain UTF-8 JSON (versioned wrapper; recommended for APIs).
 */
fun encodeSketchDocument(document: SketchDocument): String =
    SketchJson.encodeToString(document)

/**
 * Decodes [SketchDocument]. Supports [SketchDocument.CURRENT_FORMAT_VERSION] only.
 */
fun decodeSketchDocument(json: String): SketchDocument {
    val doc = SketchJson.decodeFromString<SketchDocument>(json.trim())
    require(doc.formatVersion == SketchDocument.CURRENT_FORMAT_VERSION) {
        "Unsupported sketch formatVersion ${doc.formatVersion}; supported=${SketchDocument.CURRENT_FORMAT_VERSION}"
    }
    return doc
}

/**
 * GZIP-compresses the JSON from [encodeStrokesToJson], then Base64-encodes the bytes.
 * Useful when you must store binary in a **text-only** column and want a smaller payload than uncompressed JSON + Base64.
 */
fun encodeStrokesToGzipBase64(strokes: List<ActiveStroke>): String {
    val jsonBytes = encodeStrokesToJson(strokes).toByteArray(Charsets.UTF_8)
    return Base64.encodeToString(gzip(jsonBytes), Base64.NO_WRAP)
}

/**
 * Inverse of [encodeStrokesToGzipBase64].
 */
fun decodeStrokesFromGzipBase64(encoded: String): List<ActiveStroke> {
    val compressed = Base64.decode(encoded.trim(), Base64.DEFAULT)
    val json = gunzip(compressed).toString(Charsets.UTF_8)
    return decodeStrokesFromJson(json)
}

/**
 * [encodeSketchDocument] + gzip + Base64.
 */
fun encodeSketchDocumentToGzipBase64(document: SketchDocument): String {
    val jsonBytes = encodeSketchDocument(document).toByteArray(Charsets.UTF_8)
    return Base64.encodeToString(gzip(jsonBytes), Base64.NO_WRAP)
}

/**
 * Inverse of [encodeSketchDocumentToGzipBase64].
 */
fun decodeSketchDocumentFromGzipBase64(encoded: String): SketchDocument {
    val compressed = Base64.decode(encoded.trim(), Base64.DEFAULT)
    val json = gunzip(compressed).toString(Charsets.UTF_8)
    return decodeSketchDocument(json)
}

/**
 * Best-effort decode for apps that may have mixed legacy storage:
 * 1. JSON array of strokes (`[...]`)
 * 2. JSON [SketchDocument] object (`{ "formatVersion": 1, ... }`)
 * 3. Legacy: Base64 of UTF-8 JSON array (same as [decodeStrokesFromBase64])
 * 4. GZIP+Base64 of UTF-8 JSON (array or document), i.e. outputs of [encodeStrokesToGzipBase64] / [encodeSketchDocumentToGzipBase64]
 */
fun decodeStrokesFlexible(payload: String): List<ActiveStroke> {
    val trimmed = payload.trim()
    if (trimmed.isEmpty()) return emptyList()
    return decodeStrokesFlexibleUtf8(trimmed)
        ?: decodeStrokesFlexibleBase64(trimmed)
}

private fun decodeStrokesFlexibleUtf8(text: String): List<ActiveStroke>? {
    when {
        text.startsWith('[') -> return SketchJson.decodeFromString<List<ActiveStroke>>(text)
        text.startsWith('{') -> return decodeSketchDocument(text).strokes
    }
    return null
}

private fun decodeStrokesFlexibleBase64(base64: String): List<ActiveStroke> {
    val bytes = try {
        Base64.decode(base64.trim(), Base64.DEFAULT)
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException("Payload is not valid JSON or Base64 sketch data")
    }
    if (bytes.size >= 2 && bytes[0] == GZIP_MAGIC_0 && bytes[1] == GZIP_MAGIC_1) {
        val inner = gunzip(bytes).toString(Charsets.UTF_8)
        return decodeStrokesFlexibleUtf8(inner.trim())
            ?: throw IllegalArgumentException("GZIP payload is not valid sketch JSON")
    }
    val utf8 = bytes.toString(Charsets.UTF_8).trim()
    return decodeStrokesFlexibleUtf8(utf8)
        ?: throw IllegalArgumentException("Decoded Base64 is not valid sketch JSON")
}

private fun gzip(data: ByteArray): ByteArray {
    val out = ByteArrayOutputStream(data.size)
    GZIPOutputStream(out).use { it.write(data) }
    return out.toByteArray()
}

private fun gunzip(data: ByteArray): ByteArray {
    GZIPInputStream(ByteArrayInputStream(data)).use { gzip ->
        return gzip.readBytes()
    }
}

/**
 * Builds the persistence string for [format], or `null` when [SketchStrokePersistenceFormat.None].
 * [sketchId] is used for [SketchStrokePersistenceFormat.JsonDocument] and [GzipBase64Document].
 */
fun encodeStrokesForPersistenceFormat(
    strokes: List<ActiveStroke>,
    format: SketchStrokePersistenceFormat,
    sketchId: String? = null
): String? = when (format) {
    SketchStrokePersistenceFormat.None -> null
    SketchStrokePersistenceFormat.JsonArray -> encodeStrokesToJson(strokes)
    SketchStrokePersistenceFormat.JsonDocument ->
        encodeSketchDocument(SketchDocument.fromStrokes(strokes, sketchId = sketchId))
    SketchStrokePersistenceFormat.Base64 -> encodeStrokesToBase64(strokes)
    SketchStrokePersistenceFormat.GzipBase64JsonArray -> encodeStrokesToGzipBase64(strokes)
    SketchStrokePersistenceFormat.GzipBase64Document ->
        encodeSketchDocumentToGzipBase64(SketchDocument.fromStrokes(strokes, sketchId = sketchId))
}
