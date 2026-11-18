package com.reanime.app.di

import com.reanime.core.data.DefaultCatalogueRepository
import com.reanime.core.model.catalogue.CatalogueRepository
import com.reanime.source.reanime.CataloguePageSource
import com.reanime.source.reanime.ReAnimeCatalogueSource
import org.koin.dsl.module

val catalogueModule = module {
    single<CataloguePageSource> { ReAnimeCatalogueSource(get()) }
    single<CatalogueRepository> { DefaultCatalogueRepository(get()) }
}
