package com.ovais.sketchpad.storage.di

import android.content.Context
import androidx.room.Room
import com.ovais.sketchpad.storage.db.AppDatabase
import com.ovais.sketchpad.storage.db.SketchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object StorageModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "sketch_pad_db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideSketchDao(db: AppDatabase): SketchDao {
        return db.sketchDao()
    }
}