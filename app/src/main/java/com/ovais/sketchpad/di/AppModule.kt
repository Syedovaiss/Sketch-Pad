package com.ovais.sketchpad.di

import com.ovais.sketchpad.features.domain.SketchRepo
import com.ovais.sketchpad.features.presentation.SketchViewModel
import kotlinx.serialization.json.Json
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    single { SketchRepo() }
    viewModelOf(::SketchViewModel)
}
