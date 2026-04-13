package com.ovais.sketchpad.features.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sketch_draft")
data class SketchDraftEntity(

    @PrimaryKey
    val id: String,

    val json: String,

    val updatedAt: Long
)