package com.reanime.app

import androidx.navigation3.runtime.NavKey
import com.reanime.core.model.anime.AnimeRequest
import com.reanime.core.model.catalogue.CatalogueMirror
import kotlinx.serialization.Serializable

@Serializable
internal data class AnimeDetailsKey(val animeId: String, val mirrorName: String) : NavKey {
    fun requestOrNull(): AnimeRequest? = CatalogueMirror.entries.firstOrNull { it.name == mirrorName }
        ?.let { AnimeRequest(animeId, it) }
}
