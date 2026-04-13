package com.ovais.sketch_pad.pad.data

import kotlinx.serialization.Serializable

@Serializable
data class ActiveStroke(
    val points: List<SketchPoint>,
    val color: Long,
    val strokeWidth: Float
)