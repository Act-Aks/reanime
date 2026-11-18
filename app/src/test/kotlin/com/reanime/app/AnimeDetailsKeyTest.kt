package com.reanime.app

import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.catalogue.CatalogueMirror
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnimeDetailsKeyTest {
    @Test
    fun `typed destination restores its anime and mirror identifiers`() {
        val key = AnimeDetailsKey("moon-garden-abc123", "BZ")
        val restored = Json.decodeFromString<AnimeDetailsKey>(Json.encodeToString(key))
        assertEquals(key, restored)
        assertEquals(AnimeRequest(key.animeId, CatalogueMirror.BZ), restored.requestOrNull())
    }

    @Test
    fun `unknown mirror cannot become an arbitrary network host`() {
        assertNull(AnimeDetailsKey("moon-garden-abc123", "evil.example").requestOrNull())
    }

    @Test
    fun `destinations distinguish both title and mirror`() {
        val first = AnimeDetailsKey("moon-garden-abc123", "CZ")
        assertNotEquals(first, first.copy(animeId = "another-show-123"))
        assertNotEquals(first, first.copy(mirrorName = "BZ"))
    }
}
