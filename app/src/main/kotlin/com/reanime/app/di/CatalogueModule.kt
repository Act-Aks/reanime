package com.reanime.app.di

import com.reanime.core.data.DefaultAnimeRepository
import com.reanime.core.data.DefaultCatalogueRepository
import com.reanime.core.model.anime.AnimeDetailsRepository
import com.reanime.core.model.anime.EpisodeRepository
import com.reanime.core.model.catalogue.CatalogueRepository
import com.reanime.source.reanime.AnimeDetailsPageSource
import com.reanime.source.reanime.CataloguePageSource
import com.reanime.source.reanime.EpisodePageSource
import com.reanime.source.reanime.ReAnimeCatalogueSource
import com.reanime.source.reanime.ReAnimeDetailsSource
import com.reanime.source.reanime.ReAnimeEpisodeSource
import org.koin.dsl.module

val catalogueModule = module {
    single<CataloguePageSource> { ReAnimeCatalogueSource(get()) }
    single<CatalogueRepository> { DefaultCatalogueRepository(get()) }
    single<AnimeDetailsPageSource> { ReAnimeDetailsSource(get()) }
    single<EpisodePageSource> { ReAnimeEpisodeSource(get()) }
    single { DefaultAnimeRepository(get(), get()) }
    single<AnimeDetailsRepository> { get<DefaultAnimeRepository>() }
    single<EpisodeRepository> { get<DefaultAnimeRepository>() }
}
