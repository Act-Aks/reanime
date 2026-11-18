package com.reanime.source.reanime

import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import com.reanime.core.network.KtorNetworkClient
import com.reanime.core.network.createHttpClient
import kotlinx.coroutines.runBlocking

// Explicit opt-in; two HTML requests, no scripts/embeds/media hosts, never part of JUnit/CI.
object LiveDetailsProbe {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val request = AnimeRequest(args.firstOrNull() ?: "red-river-nb73wr", CatalogueMirror.CZ)
        createHttpClient("ReAnime metadata integration probe").use { client ->
            val network = KtorNetworkClient(client)
            val details = ReAnimeDetailsSource(network).details(request)
            val episodes = ReAnimeEpisodeSource(network).episodes(request)
            check(details is Result.Success) { "Details unavailable: $details" }
            check(episodes is Result.Success) { "Episode metadata unavailable: $episodes" }
            val hasSynopsis = !details.data.synopsis.isNullOrBlank()
            println(
                "Details fetched: ${details.data.facts.size} facts, ${details.data.genres.size} genres, " +
                    "synopsis=$hasSynopsis; ${episodes.data.episodes.size} episode cards, " +
                    "reported=${episodes.data.reportedCount}, partial=${episodes.data.isPartial}."
            )
        }
    }
}
