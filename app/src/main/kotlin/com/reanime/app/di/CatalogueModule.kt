package com.reanime.app.di

import com.reanime.core.data.DefaultCatalogueRepository
import com.reanime.core.model.catalogue.CatalogueRepository
import com.reanime.source.reanime.CataloguePageSource
import com.reanime.source.reanime.ReAnimeCatalogueSource
import com.reanime.source.reanime.createReAnimeHttpClient
import org.koin.dsl.module
import org.koin.dsl.onClose

val catalogueModule = module {
    single { createReAnimeHttpClient() } onClose { it?.close() }
    single<CataloguePageSource> { ReAnimeCatalogueSource(get()) }
    single<CatalogueRepository> { DefaultCatalogueRepository(get()) }
}
