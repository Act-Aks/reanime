package com.reanime.source.reanime

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.Episode
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.anime.EpisodeLanguage
import com.reanime.core.model.anime.EpisodeListing
import com.reanime.core.model.result.Result
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

class ReAnimeEpisodeParser {
    fun parse(html: String, request: AnimeRequest): Result<EpisodeListing, EpisodeError> {
        val document = Ksoup.parse(html)
        val heading = document.select("main h2").firstOrNull { it.text() == "Episodes" }
        val canonical = document.selectFirst("link[rel=canonical]")?.attr("href")
        return when {
            document.isAccessChallenge() -> Result.Failure(EpisodeError.ACCESS_BLOCKED)

            heading == null || canonical == null -> Result.Failure(EpisodeError.LAYOUT_CHANGED)

            ReAnimeLinks.animeId(canonical, request.mirror) != request.animeId ->
                Result.Failure(EpisodeError.IDENTITY_MISMATCH)

            else -> {
                val count = heading.parent()?.children()?.firstOrNull { it.tagName() == "span" }
                    ?.text()?.toIntOrNull()?.takeIf { it >= 0 }
                val episodes = document.select("main a[data-episode][href]")
                    .mapNotNull { parseEpisode(it, request) }.distinctBy { it.number }
                    .sortedBy { it.number.toBigDecimal() }.toPersistentList()
                val missingList = episodes.isEmpty() && count != 0
                val inconsistentCount = count != null && episodes.size > count
                if (missingList || inconsistentCount) {
                    Result.Failure(EpisodeError.LAYOUT_CHANGED)
                } else {
                    Result.Success(EpisodeListing(request.animeId, episodes, count))
                }
            }
        }
    }

    private fun parseEpisode(card: Element, request: AnimeRequest): Episode? {
        val number = ReAnimeLinks.episodeNumber(card.attr("href"), request.mirror, request.animeId)
        val title = card.selectFirst("h3")?.clone()?.apply { select(".ep-title-num").remove() }?.text()
        if (number == null || number != card.attr("data-episode") || title.isNullOrBlank()) return null
        val languages = buildSet {
            if (card.selectFirst("[title=Subbed]") != null) add(EpisodeLanguage.SUB)
            if (card.selectFirst("[title=Dubbed]") != null) add(EpisodeLanguage.DUB)
        }.toPersistentSet()
        return Episode(
            number = number,
            title = title,
            synopsis = card.selectFirst("p")?.text()?.takeIf(String::isNotBlank),
            languages = languages,
            aired = card.selectFirst(".ep-aired")?.text(),
            thumbnailUrl = card.selectFirst("img[src]")?.attr("src")?.let(ReAnimeLinks::posterUrl)
        )
    }
}
