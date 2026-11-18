package com.reanime.core.network

import com.reanime.core.network.di.NetworkClientRole
import com.reanime.core.network.di.networkModule
import io.ktor.client.HttpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.koin.core.qualifier.named
import org.koin.dsl.koinApplication

class NetworkModuleTest {
    @Test
    fun `qualified clients are isolated singletons and close with their DI owner`() = runTest {
        val application = koinApplication { modules(networkModule("ReAnime-test")) }
        val catalogue = application.koin.get<HttpClient>(named(NetworkClientRole.CATALOGUE))
        val artwork = application.koin.get<HttpClient>(named(NetworkClientRole.ARTWORK))
        try {
            assertNotSame(catalogue, artwork)
            assertSame(catalogue, application.koin.get<HttpClient>(named(NetworkClientRole.CATALOGUE)))
            assertTrue(application.koin.get<NetworkClient>() is KtorNetworkClient)
        } finally {
            application.close()
        }
        catalogue.coroutineContext[Job]?.join()
        artwork.coroutineContext[Job]?.join()
        catalogue.engine.coroutineContext[Job]?.join()
        artwork.engine.coroutineContext[Job]?.join()
        assertTrue(catalogue.coroutineContext[Job]?.isCompleted == true)
        assertTrue(artwork.coroutineContext[Job]?.isCompleted == true)
        assertTrue(catalogue.engine.coroutineContext[Job]?.isCompleted == true)
        assertTrue(artwork.engine.coroutineContext[Job]?.isCompleted == true)
    }
}
