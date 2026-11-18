package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueError
import com.reanime.core.model.catalogue.CatalogueMirror
import com.reanime.core.model.catalogue.CatalogueResult
import com.reanime.core.model.catalogue.CatalogueSectionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReAnimeHomeParserTest {
    private val parser = ReAnimeHomeParser()

    @Test
    fun `semantic sections parse without styles or executing scripts`() {
        val result = parser.parse(homeFixture(), CatalogueMirror.CZ) as CatalogueResult.Success

        assertEquals(CatalogueSectionKind.entries, result.home.sections.map { it.kind })
        val first = result.home.sections.first().anime.single()
        assertEquals("moon-garden-abc123", first.id)
        assertEquals("Moon & Garden", first.title)
        assertEquals("https://s4.anilist.co/file/cover.jpg", first.posterUrl)
        assertNull(result.home.sections[1].anime.single().posterUrl)
    }

    @Test
    fun `mirror choice does not change anime identity`() {
        val first = parser.parse(homeFixture(), CatalogueMirror.CZ)
        assertEquals(first, parser.parse(homeFixture(), CatalogueMirror.BZ))
    }

    @Test
    fun `unrecognized layout is not misreported as an empty catalogue`() {
        assertEquals(
            CatalogueResult.Failure(CatalogueError.LAYOUT_CHANGED),
            parser.parse("<main><section><h2>Changed</h2></section></main>", CatalogueMirror.CZ)
        )
    }

    @Test
    fun `challenge page is reported rather than executed`() {
        assertEquals(
            CatalogueResult.Failure(CatalogueError.ACCESS_BLOCKED),
            parser.parse("<html><title>Just a moment...</title></html>", CatalogueMirror.CZ)
        )
    }

    @Test
    fun `source links reject arbitrary hosts paths schemes and credentials`() {
        listOf(
            "https://example.com/anime/title-123",
            "http://reanime.cz/anime/title-123",
            "https://user@reanime.cz/anime/title-123",
            "https://reanime.cz:444/anime/title-123",
            "javascript:alert(1)",
            "/anime/title%2F123",
            "/profile/title-123",
            "https://reanime.cz.evil.example/anime/title-123",
            "https://[broken"
        ).forEach { href -> assertNull(href, ReAnimeLinks.animeId(href, CatalogueMirror.CZ)) }
    }

    @Test
    fun `artwork rejects untrusted or credential-bearing URLs`() {
        listOf(
            "http://s4.anilist.co/cover.jpg",
            "https://localhost/cover.jpg",
            "https://example.com/cover.jpg",
            "https://user@s4.anilist.co/cover.jpg",
            "data:image/svg+xml,<svg/>",
            "/placeholder.svg"
        ).forEach { url -> assertNull(url, ReAnimeLinks.posterUrl(url)) }
    }
}

internal fun homeFixture(): String = checkNotNull(
    ReAnimeHomeParserTest::class.java.getResource("/home.html")
).readText()
