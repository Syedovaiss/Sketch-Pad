package com.ovais.sketchpad.storage.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ovais.sketchpad.storage.db.SketchDao
import com.ovais.sketchpad.features.data.SketchDraftEntity

@Database(entities = [SketchDraftEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sketchDao(): SketchDao
}