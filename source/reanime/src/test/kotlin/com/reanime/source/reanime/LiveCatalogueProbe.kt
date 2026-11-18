package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import com.reanime.core.network.KtorNetworkClient
import com.reanime.core.network.createHttpClient
import kotlinx.coroutines.runBlocking

// Invoked explicitly with :source:reanime:probeCatalogue, not by JUnit or CI.
object LiveCatalogueProbe {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        createHttpClient("ReAnime catalogue integration probe").use { client ->
            when (val result = ReAnimeCatalogueSource(KtorNetworkClient(client)).home(CatalogueMirror.CZ)) {
                is Result.Success -> println(
                    "Catalogue fetched: ${result.data.sections.size} sections, " +
                        "${result.data.sections.sumOf { it.anime.size }} cards."
                )

                is Result.Failure -> {
                    println("Catalogue unavailable: ${result.error}. No challenge or access control was bypassed.")
                    error("Native catalogue probe did not succeed")
                }
            }
        }
    }
}
