package com.ovais.sketch_pad.utils

import kotlinx.serialization.json.Json

/**
 * Shared [Json] for all sketch stroke / document serialization in this module.
 */
internal val SketchJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
