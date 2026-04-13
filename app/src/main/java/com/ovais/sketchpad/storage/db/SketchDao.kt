package com.ovais.sketchpad.storage.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ovais.sketchpad.features.data.SketchDraftEntity

@Dao
interface SketchDao {

    @Query("SELECT * FROM sketch_draft WHERE id = :id")
    suspend fun getDraft(id: String): SketchDraftEntity?

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun saveDraft(entity: SketchDraftEntity)

    @Query("DELETE FROM sketch_draft WHERE id = :id")
    suspend fun deleteDraft(id: String)
}