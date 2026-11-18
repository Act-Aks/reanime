package com.reanime.source.reanime

import com.fleeksoft.ksoup.Ksoup
import com.reanime.core.model.anime.AnimeDetails
import com.reanime.core.model.anime.AnimeFact
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.result.Result
import kotlinx.collections.immutable.toPersistentList

class ReAnimeDetailsParser {
    fun parse(html: String, request: AnimeRequest): Result<AnimeDetails, DetailsError> {
        val document = Ksoup.parse(html)
        val title = document.selectFirst("main h1[data-anime-title]")
        val canonical = document.selectFirst("link[rel=canonical]")?.attr("href")
        return when {
            document.isAccessChallenge() -> Result.Failure(DetailsError.ACCESS_BLOCKED)

            title == null || title.text().isBlank() || canonical == null -> Result.Failure(DetailsError.LAYOUT_CHANGED)

            ReAnimeLinks.animeId(canonical, request.mirror) != request.animeId ->
                Result.Failure(DetailsError.IDENTITY_MISMATCH)

            else -> {
                val facts = document.select("main p").mapNotNull { label ->
                    val name = label.text()
                    val value = label.parent()?.children()?.firstOrNull { it.tagName() == "span" }?.text()
                    if (name in FACT_LABELS && !value.isNullOrBlank()) AnimeFact(name, value) else null
                }.distinctBy { it.label }.toPersistentList()
                val genres = document.select("main a[href]")
                    .filter { it.attr("href").startsWith("/search?genre=") }
                    .map { it.text() }.filter(String::isNotBlank).distinct().toPersistentList()
                val poster = document.selectFirst("meta[property=og:image]")?.attr("content")
                    ?.let(ReAnimeLinks::posterUrl)
                Result.Success(
                    AnimeDetails(
                        anime = AnimeSummary(request.animeId, title.text(), poster),
                        alternativeTitle = document.selectFirst("main h2[data-anime-title]")?.text(),
                        synopsis = title.parent()?.parent()?.takeIf { it.closest("main") != null }
                            ?.selectFirst("p")?.text()?.takeIf(String::isNotBlank),
                        facts = facts,
                        genres = genres
                    )
                )
            }
        }
    }

    private companion object {
        val FACT_LABELS = setOf("Type", "Episodes", "Duration", "Status", "Start Date", "Season")
    }
}
