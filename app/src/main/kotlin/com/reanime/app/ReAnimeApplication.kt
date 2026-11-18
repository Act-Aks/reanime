package com.reanime.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.reanime.app.di.catalogueModule
import com.reanime.feature.browse.browseModule
import com.reanime.source.reanime.createReAnimeHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class ReAnimeApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReAnimeApplication)
            modules(catalogueModule, browseModule)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            // Separate client: artwork must never inherit website cookies/credentials.
            add(KtorNetworkFetcherFactory(httpClient = { createReAnimeHttpClient() }))
        }
        .build()
}
