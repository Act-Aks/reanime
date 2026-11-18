package com.reanime.app

import android.app.Application
import com.reanime.feature.browse.browseModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ReAnimeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReAnimeApplication)
            modules(browseModule)
        }
    }
}
