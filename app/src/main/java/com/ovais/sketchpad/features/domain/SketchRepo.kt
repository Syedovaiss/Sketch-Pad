package com.ovais.sketchpad.features.domain

class SketchRepo(
//    private val dao: SketchDao
) {

    suspend fun save(id: String, json: String) {
       /* dao.saveDraft(
            SketchDraftEntity(
                id = id,
                json = json,
                updatedAt = System.currentTimeMillis()
            )
        )*/
    }

    suspend fun load(id: String): String? {
       /* return dao.getDraft(id)?.json*/
        return null
    }

    suspend fun delete(id: String) {
      /*  dao.deleteDraft(id)*/
    }
}