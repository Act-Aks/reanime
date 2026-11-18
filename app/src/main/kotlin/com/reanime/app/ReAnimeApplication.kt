package com.reanime.app

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.reanime.app.di.catalogueModule
import com.reanime.core.network.di.NetworkClientRole
import com.reanime.core.network.di.networkModule
import com.reanime.feature.home.homeModule
import io.ktor.client.HttpClient
import org.koin.android.ext.android.getKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.qualifier.named

class ReAnimeApplication :
    Application(),
    SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ReAnimeApplication)
            modules(networkModule("ReAnime/${BuildConfig.VERSION_NAME} (Android)"), catalogueModule, homeModule)
        }
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            // Separate client: artwork must never inherit website cookies/credentials.
            add(
                KtorNetworkFetcherFactory(httpClient = {
                    getKoin().get<HttpClient>(named(NetworkClientRole.ARTWORK))
                })
            )
        }
        .build()
}
