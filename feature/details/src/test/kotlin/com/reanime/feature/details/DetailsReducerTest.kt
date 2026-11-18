package com.reanime.feature.details

import com.reanime.core.model.anime.DetailsError
import com.reanime.core.model.anime.EpisodeError
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailsReducerTest {
    @Test
    fun `episode failure does not replace loaded details`() {
        val original = DetailsScreenState(details = DetailsContentState.Content(DetailsFixtures.details()))
        val next = reduce(original, DetailsChange.FailedEpisodes(EpisodeError.ACCESS_BLOCKED))
        assertEquals(original.details, next.details)
        assertEquals(EpisodesContentState.Error(EpisodeError.ACCESS_BLOCKED), next.episodes)
        assertEquals(EpisodesContentState.Loading, original.episodes)
    }

    @Test
    fun `refresh failures retain previous metadata and episode content`() {
        val initial = DetailsScreenState(
            details = DetailsContentState.Content(DetailsFixtures.details()),
            episodes = EpisodesContentState.Content(DetailsFixtures.episodes())
        )
        val refreshing = reduce(reduce(initial, DetailsChange.LoadingDetails), DetailsChange.LoadingEpisodes)
        assertTrue(refreshing.isLoading)
        val next = reduce(
            reduce(refreshing, DetailsChange.FailedDetails(DetailsError.TIMEOUT)),
            DetailsChange.FailedEpisodes(EpisodeError.RATE_LIMITED)
        )
        assertFalse(next.isLoading)
        assertEquals(DetailsFixtures.details(), (next.details as DetailsContentState.Content).data)
        assertEquals(DetailsError.TIMEOUT, (next.details as DetailsContentState.Content).refreshError)
        assertEquals(EpisodeError.RATE_LIMITED, (next.episodes as EpisodesContentState.Content).refreshError)
    }

    @Test
    fun `filters only use source tags and clear selection`() {
        val initial = DetailsScreenState(episodes = EpisodesContentState.Content(DetailsFixtures.episodes()))
        val selected = reduce(initial, DetailsChange.SelectEpisode("1"))
        val filtered = reduce(selected, DetailsChange.FilterEpisodes(EpisodeFilter.DUB))
        assertEquals(listOf("2"), filtered.visibleEpisodes.map { it.number })
        assertNull(filtered.selectedEpisode)
        assertEquals("1", selected.selectedEpisode)
    }

    @Test
    fun `only visible episode identities can be selected and tapped again to collapse`() {
        val initial = DetailsScreenState(episodes = EpisodesContentState.Content(DetailsFixtures.episodes()))
        assertEquals(initial, reduce(initial, DetailsChange.SelectEpisode("not-listed")))
        val selected = reduce(initial, DetailsChange.SelectEpisode("1"))
        assertNull(reduce(selected, DetailsChange.SelectEpisode("1")).selectedEpisode)
    }

    @Test
    fun `selection is cleared when refreshed list no longer contains it`() {
        val initial = DetailsScreenState(selectedEpisode = "1")
        val empty = DetailsFixtures.episodes().copy(episodes = persistentListOf(), reportedCount = 0)
        assertNull(reduce(initial, DetailsChange.LoadedEpisodes(empty)).selectedEpisode)
    }
}
