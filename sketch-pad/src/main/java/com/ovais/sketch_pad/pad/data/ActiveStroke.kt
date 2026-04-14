package com.ovais.sketch_pad.pad.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ActiveStroke(
    val points: List<SketchPoint>,
    val color: Long,
    val strokeWidth: Float,
    val id: String = UUID.randomUUID().toString()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ActiveStroke) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
