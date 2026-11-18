package com.reanime.source.reanime

import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.EpisodeError
import com.reanime.core.model.anime.EpisodeLanguage
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReAnimeEpisodeParserTest {
    private val request = AnimeRequest("moon-garden-abc123", CatalogueMirror.CZ)
    private val parser = ReAnimeEpisodeParser()

    @Test
    fun `episodes are identity-checked sorted deduplicated and marked partial`() {
        val result = parser.parse(episodeFixture(), request) as Result.Success
        assertEquals(listOf("0", "1", "2.5"), result.data.episodes.map { it.number })
        assertEquals("The garden opens", result.data.episodes[1].title)
        assertEquals(setOf(EpisodeLanguage.SUB, EpisodeLanguage.DUB), result.data.episodes[1].languages)
        assertEquals("JUL 1, 2026", result.data.episodes[1].aired)
        assertTrue(result.data.isPartial)
        assertEquals(4, result.data.reportedCount)
    }

    @Test
    fun `explicit zero episodes is valid but a missing list with a positive count is not`() {
        val html = "<link rel='canonical' href='https://reanime.cz/watch/moon-garden-abc123?ep=1'>" +
            "<main><div><h2>Episodes</h2><span>0</span></div></main>"
        val result = parser.parse(html, request) as Result.Success
        assertTrue(result.data.episodes.isEmpty())
        assertFalse(result.data.isPartial)
        assertEquals(Result.Failure(EpisodeError.LAYOUT_CHANGED), parser.parse(html.replace(">0<", ">4<"), request))
    }

    @Test
    fun `unknown structure or another anime is not an empty success`() {
        assertEquals(Result.Failure(EpisodeError.LAYOUT_CHANGED), parser.parse("<main></main>", request))
        assertEquals(
            Result.Failure(EpisodeError.IDENTITY_MISMATCH),
            parser.parse(
                episodeFixture().replace(
                    "https://reanime.cz/watch/moon-garden-abc123",
                    "https://reanime.cz/watch/wrong-123"
                ),
                request
            )
        )
    }

    @Test
    fun `ambiguous invalid and foreign episode URLs are rejected`() {
        val prefix = "/watch/moon-garden-abc123?"
        listOf(
            "${prefix}ep=latest",
            "${prefix}ep=1&ep=2",
            "${prefix}ep=%31",
            "${prefix}ep=-1",
            "https://evil.example${prefix}ep=1",
            "${prefix}ep=9999999",
            "${prefix}ep=NaN"
        )
            .forEach { href ->
                assertEquals(null, ReAnimeLinks.episodeNumber(href, CatalogueMirror.CZ, request.animeId))
            }
    }

    @Test
    fun `challenge is exposed as access blocked`() {
        assertEquals(
            Result.Failure(EpisodeError.ACCESS_BLOCKED),
            parser.parse("<form id='challenge-form'></form>", request)
        )
    }

    @Test
    fun `a reported total smaller than the parsed list is rejected`() {
        assertEquals(
            Result.Failure(EpisodeError.LAYOUT_CHANGED),
            parser.parse(episodeFixture().replace("<span>4</span>", "<span>2</span>"), request)
        )
    }
}

internal fun episodeFixture(): String =
    checkNotNull(ReAnimeEpisodeParserTest::class.java.getResource("/episodes.html")).readText()
