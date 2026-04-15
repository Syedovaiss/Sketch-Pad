package com.ovais.sketchpad.core

import android.app.Application
import com.ovais.sketchpad.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SketchPadApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SketchPadApp)
            modules(appModule)
        }
    }
}