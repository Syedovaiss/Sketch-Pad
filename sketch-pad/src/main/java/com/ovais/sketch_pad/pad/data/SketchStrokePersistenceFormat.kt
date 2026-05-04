package com.ovais.sketch_pad.pad.data

/**
 * How [SketchPad] builds an encoded persistence string when the user saves from the toolbar.
 * Raw strokes are always passed to [SketchPadCallbacks.onSave]; the encoded payload is optional
 * via [SketchPadCallbacks.onSavePersistencePayload].
 */
enum class SketchStrokePersistenceFormat {
    /**
     * Do not build an encoded string; [SketchPadCallbacks.onSavePersistencePayload] is not called.
     */
    None,

    /**
     * Plain UTF-8 JSON array of [ActiveStroke] (see [com.ovais.sketch_pad.utils.encodeStrokesToJson]).
     */
    JsonArray,

    /**
     * Versioned JSON [SketchDocument] (see [com.ovais.sketch_pad.utils.encodeSketchDocument]).
     */
    JsonDocument,

    /**
     * Legacy Base64 over UTF-8 JSON array (see [com.ovais.sketch_pad.utils.encodeStrokesToBase64]).
     */
    Base64,

    /**
     * GZIP-compressed JSON array, then Base64 (see [com.ovais.sketch_pad.utils.encodeStrokesToGzipBase64]).
     */
    GzipBase64JsonArray,

    /**
     * GZIP-compressed [SketchDocument] JSON, then Base64
     * (see [com.ovais.sketch_pad.utils.encodeSketchDocumentToGzipBase64]).
     */
    GzipBase64Document
}
