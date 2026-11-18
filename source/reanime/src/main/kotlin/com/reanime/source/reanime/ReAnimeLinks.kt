package com.reanime.source.reanime

import com.reanime.core.model.catalogue.CatalogueMirror
import java.net.URI
import java.net.URISyntaxException

internal object ReAnimeLinks {
    private val sourceHosts = CatalogueMirror.entries.map { it.host }.toSet()
    private val artworkHosts = setOf("s4.anilist.co", "image.tmdb.org", "artworks.thetvdb.com")
    private val animePath = Regex("/(?:anime|watch)/([a-z0-9]+(?:-[a-z0-9]+)*)/?")
    private val sourceId = Regex("[a-z0-9]+(?:-[a-z0-9]+)*")
    private val episodeNumber = Regex("(?:0|[1-9][0-9]{0,5})(?:\\.[0-9]{1,3})?")

    fun isAnimeId(value: String): Boolean = value.length <= MAX_ID_LENGTH && sourceId.matches(value)

    fun animeId(href: String, mirror: CatalogueMirror): String? {
        val uri = parse(href)?.let { URI("https://${mirror.host}").resolve(it) }
        return uri?.takeIf { it.isTrustedHttps(sourceHosts) }
            ?.rawPath?.let(animePath::matchEntire)?.groupValues?.get(1)?.takeIf(::isAnimeId)
    }

    fun episodeNumber(href: String, mirror: CatalogueMirror, expectedAnimeId: String): String? {
        val uri = parse(href)?.let { URI("https://${mirror.host}").resolve(it) }
        val values = uri?.rawQuery.orEmpty().split('&').filter { it.substringBefore('=') == "ep" }
        val number = values.singleOrNull()?.substringAfter('=', "")
        return number?.takeIf {
            uri?.rawPath == "/watch/$expectedAnimeId" && animeId(href, mirror) == expectedAnimeId && isEpisodeNumber(it)
        }
    }

    fun isEpisodeNumber(value: String): Boolean = episodeNumber.matches(value)

    fun posterUrl(value: String): String? = parse(value)
        ?.takeIf { it.isTrustedHttps(artworkHosts) }
        ?.toASCIIString()

    private fun URI.isTrustedHttps(hosts: Set<String>): Boolean =
        scheme == "https" && host in hosts && userInfo == null && (port == -1 || port == HTTPS_PORT)

    private fun parse(value: String): URI? = try {
        URI(value)
    } catch (_: URISyntaxException) {
        null
    }

    private const val HTTPS_PORT = 443
    private const val MAX_ID_LENGTH = 200
}
