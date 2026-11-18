package com.reanime.core.network.di

import com.reanime.core.network.KtorNetworkClient
import com.reanime.core.network.NetworkClient
import com.reanime.core.network.createHttpClient
import io.ktor.client.HttpClient
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.koin.dsl.onClose

/** Separate instances make ownership explicit and prevent future credential/context sharing. */
fun networkModule(userAgent: String): Module = module {
    single<HttpClient>(named(NetworkClientRole.CATALOGUE)) {
        createHttpClient(userAgent)
    } onClose { it?.close() }
    single<HttpClient>(named(NetworkClientRole.ARTWORK)) {
        createHttpClient(userAgent)
    } onClose { it?.close() }
    single<NetworkClient> { KtorNetworkClient(get(named(NetworkClientRole.CATALOGUE))) }
}
