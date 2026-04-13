package com.ovais.sketchpad.features.di

import com.ovais.sketchpad.features.domain.SketchRepo
import com.ovais.sketchpad.storage.db.SketchDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object SketchPadModule {

    @Provides
    @Singleton
    fun providesSketchRepo(dao: SketchDao): SketchRepo {
        return SketchRepo(dao)
    }
}