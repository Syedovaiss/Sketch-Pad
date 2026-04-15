package com.ovais.sketchpad.di

import androidx.room.Room
import com.ovais.sketchpad.features.domain.SketchRepo
import com.ovais.sketchpad.features.presentation.SketchViewModel
import com.ovais.sketchpad.storage.db.AppDatabase
import com.ovais.sketchpad.storage.db.SketchDao
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single<AppDatabase> {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "sketch_pad_db"
        ).build()
    }
    single<SketchDao> { get<AppDatabase>().sketchDao() }
    single { SketchRepo(get()) }
    viewModelOf(::SketchViewModel)
}
