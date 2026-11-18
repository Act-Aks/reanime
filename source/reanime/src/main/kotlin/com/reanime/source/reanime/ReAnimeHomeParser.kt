package com.reanime.source.reanime

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.reanime.core.model.catalogue.AnimeSummary
import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueHome
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueSection
import com.reanime.core.model.catalogue.CatalogueSectionKind
import com.reanime.core.model.result.Result
import kotlinx.collections.immutable.toPersistentList

class ReAnimeHomeParser {
    fun parse(html: String, mirror: CatalogueMirror): Result<CatalogueHome, CatalogueError> {
        val document = Ksoup.parse(html)
        if (document.isAccessChallenge()) {
            return Result.Failure(CatalogueError.ACCESS_BLOCKED)
        }
        val sections = document.select("main section").mapNotNull { section ->
            val kind = sectionKind(section.selectFirst("h2")?.text()) ?: return@mapNotNull null
            val anime = section.select("a[href]").mapNotNull { card -> parseCard(card, mirror) }
                .distinctBy { it.id }.toPersistentList()
            if (anime.isEmpty()) null else CatalogueSection(kind, anime)
        }.distinctBy { it.kind }.toPersistentList()
        return if (sections.isEmpty()) {
            Result.Failure(CatalogueError.LAYOUT_CHANGED)
        } else {
            Result.Success(CatalogueHome(sections))
        }
    }

    private fun parseCard(card: Element, mirror: CatalogueMirror): AnimeSummary? {
        val id = ReAnimeLinks.animeId(card.attr("href"), mirror)
        val title = card.selectFirst("h3")?.text()?.trim()
        if (id == null || title.isNullOrBlank()) return null
        val poster = card.selectFirst("img[src]")?.attr("src")?.let(ReAnimeLinks::posterUrl)
        return AnimeSummary(id = id, title = title, posterUrl = poster)
    }

    private fun sectionKind(heading: String?): CatalogueSectionKind? = when (heading) {
        "Latest Episodes" -> CatalogueSectionKind.LATEST_EPISODES
        "New on Site" -> CatalogueSectionKind.NEW_ON_SITE
        "Upcoming" -> CatalogueSectionKind.UPCOMING
        else -> null
    }
}
