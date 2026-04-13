package com.ovais.sketchpad.features.domain

import com.ovais.sketchpad.features.data.SketchDraftEntity
import com.ovais.sketchpad.storage.db.SketchDao

class SketchRepo(
    private val dao: SketchDao
) {

    suspend fun save(id: String, json: String) {
        dao.saveDraft(
            SketchDraftEntity(
                id = id,
                json = json,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun load(id: String): String? {
        return dao.getDraft(id)?.json
    }

    suspend fun delete(id: String) {
        dao.deleteDraft(id)
    }
}