package com.reanime.source.reanime

import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.result.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReAnimeDetailsParserTest {
    private val request = AnimeRequest("moon-garden-abc123", CatalogueMirror.CZ)
    private val parser = ReAnimeDetailsParser()

    @Test
    fun `semantic details parse without running scripts or copying HTML into the UI`() {
        val result = parser.parse(detailsFixture(), request) as Result.Success
        assertEquals("Moon Garden", result.data.anime.title)
        assertEquals("Tsuki no Niwa", result.data.alternativeTitle)
        assertEquals("https://s4.anilist.co/file/cover.jpg", result.data.anime.posterUrl)
        assertEquals(listOf("Fantasy"), result.data.genres)
        assertEquals(listOf("Episodes", "Status", "Season"), result.data.facts.map { it.label })
        assertEquals("A safe garden story. Source attribution stays.", result.data.synopsis)
        assertFalse(result.data.synopsis.orEmpty().contains("<b>"))
    }

    @Test
    fun `wrong anime canonical is rejected`() {
        assertEquals(
            Result.Failure(DetailsError.IDENTITY_MISMATCH),
            parser.parse(detailsFixture().replace("/anime/moon-garden-abc123", "/anime/another-123"), request)
        )
    }

    @Test
    fun `missing structure and challenges are not valid details`() {
        assertEquals(
            Result.Failure(DetailsError.LAYOUT_CHANGED),
            parser.parse("<main><h1>Unknown</h1></main>", request)
        )
        assertEquals(
            Result.Failure(DetailsError.ACCESS_BLOCKED),
            parser.parse("<title>Just a moment...</title>", request)
        )
    }

    @Test
    fun `untrusted poster is omitted rather than fetched`() {
        val result = parser.parse(
            detailsFixture().replace("https://s4.anilist.co/file/cover.jpg", "https://evil.example/x"),
            request
        )
            as Result.Success
        assertNull(result.data.anime.posterUrl)
    }

    @Test
    fun `missing optional metadata stays absent and footer text is not a synopsis`() {
        val html = "<link rel='canonical' href='https://reanime.cz/anime/moon-garden-abc123'>" +
            "<main><h1 data-anime-title>Moon Garden</h1></main><footer><p>Not a synopsis</p></footer>"
        val result = parser.parse(html, request) as Result.Success
        assertNull(result.data.synopsis)
        assertNull(result.data.alternativeTitle)
        assertNull(result.data.anime.posterUrl)
        assertTrue(result.data.facts.isEmpty())
        assertTrue(result.data.genres.isEmpty())
    }
}

internal fun detailsFixture(): String =
    checkNotNull(ReAnimeDetailsParserTest::class.java.getResource("/details.html")).readText()
