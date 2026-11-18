package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueResult
import kotlinx.coroutines.runBlocking

// Invoked explicitly with :source:reanime:probeCatalogue, not by JUnit or CI.
object LiveCatalogueProbe {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        createReAnimeHttpClient().use { client ->
            when (val result = ReAnimeCatalogueSource(client).home(CatalogueMirror.CZ)) {
                is CatalogueResult.Success -> println(
                    "Catalogue fetched: ${result.home.sections.size} sections, " +
                        "${result.home.sections.sumOf { it.anime.size }} cards."
                )

                is CatalogueResult.Failure -> {
                    println("Catalogue unavailable: ${result.error}. No challenge or access control was bypassed.")
                    error("Native catalogue probe did not succeed")
                }
            }
        }
    }
}
