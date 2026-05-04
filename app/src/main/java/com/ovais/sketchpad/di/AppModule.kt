package com.ovais.sketchpad.di

import com.ovais.sketchpad.features.domain.SketchRepo
import com.ovais.sketchpad.features.presentation.SketchViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { SketchRepo() }
    viewModelOf(::SketchViewModel)
}
