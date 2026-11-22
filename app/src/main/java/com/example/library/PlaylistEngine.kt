package com.example.library

import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.core.model.SmartPlaylistRule
import com.example.core.model.SmartRuleType
import com.example.persistence.M3uPlaylistCodec
import com.example.persistence.PlaylistDao
import com.example.persistence.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class PlaylistEngine(
    private val playlistDao: PlaylistDao
) {
    suspend fun createPlaylist(
        name: String,
        description: String = "",
        initialTracks: List<MusicTrack> = emptyList(),
        smartRule: SmartPlaylistRule? = null
    ): MusicPlaylist = withContext(Dispatchers.IO) {
        val playlist = MusicPlaylist(
            id = "pl_${UUID.randomUUID()}",
            name = name.trim().ifBlank { "Untitled Playlist" },
            description = description.trim(),
            providerId = ProviderId.LOCAL,
            isSmart = smartRule != null,
            smartRule = smartRule,
            trackIds = initialTracks.map { it.id },
            artworkUri = initialTracks.firstOrNull()?.artworkUri
        )
        playlistDao.upsertPlaylist(playlist.toEntity())
        if (initialTracks.isNotEmpty()) {
            playlistDao.replacePlaylistTrackOrder(playlist.id, playlist.trackIds)
        }
        playlist
    }

    suspend fun renamePlaylist(playlist: MusicPlaylist, newName: String) = withContext(Dispatchers.IO) {
        val clean = newName.trim()
        if (clean.isEmpty()) return@withContext
        playlistDao.upsertPlaylist(playlist.copy(name = clean).toEntity())
    }

    suspend fun toggleFavoritePlaylist(playlist: MusicPlaylist) = withContext(Dispatchers.IO) {
        playlistDao.upsertPlaylist(playlist.copy(isFavorite = !playlist.isFavorite).toEntity())
    }

    suspend fun duplicatePlaylist(playlist: MusicPlaylist): MusicPlaylist = withContext(Dispatchers.IO) {
        val copy = playlist.copy(
            id = "pl_${UUID.randomUUID()}",
            name = "${playlist.name} (Copy)",
            createdAtMs = System.currentTimeMillis()
        )
        playlistDao.upsertPlaylist(copy.toEntity())
        if (copy.trackIds.isNotEmpty()) {
            playlistDao.replacePlaylistTrackOrder(copy.id, copy.trackIds)
        }
        copy
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        playlistDao.clearPlaylistTracks(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addTrackToPlaylist(playlist: MusicPlaylist, track: MusicTrack) = withContext(Dispatchers.IO) {
        if (playlist.isSmart) return@withContext
        val updated = (playlist.trackIds + track.id).distinct()
        playlistDao.replacePlaylistTrackOrder(playlist.id, updated)
        if (playlist.artworkUri == null && track.artworkUri != null) {
            playlistDao.upsertPlaylist(playlist.copy(artworkUri = track.artworkUri).toEntity())
        }
    }

    suspend fun removeTrackFromPlaylist(playlist: MusicPlaylist, trackId: String) = withContext(Dispatchers.IO) {
        if (playlist.isSmart) return@withContext
        val updated = playlist.trackIds.filterNot { it == trackId }
        playlistDao.replacePlaylistTrackOrder(playlist.id, updated)
    }

    suspend fun moveTrackInPlaylist(playlist: MusicPlaylist, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        if (playlist.isSmart) return@withContext
        if (fromIndex !in playlist.trackIds.indices || toIndex !in playlist.trackIds.indices) return@withContext
        val mutable = playlist.trackIds.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        playlistDao.replacePlaylistTrackOrder(playlist.id, mutable)
    }

    suspend fun importM3uPlaylist(content: String, fallbackName: String, localLibrary: List<MusicTrack>): MusicPlaylist {
        val (parsedName, entries) = M3uPlaylistCodec.parseM3u(content)
        val matchedTracks = entries.mapNotNull { entry ->
            localLibrary.firstOrNull { track ->
                (track.localFilePath != null && track.localFilePath == entry.location) ||
                    (track.localContentUri != null && track.localContentUri == entry.location) ||
                    (entry.title != null && track.title.equals(entry.title, ignoreCase = true) &&
                        (entry.artist == null || track.artist.equals(entry.artist, ignoreCase = true)))
            }
        }
        return createPlaylist(
            name = parsedName ?: fallbackName.substringBeforeLast('.').ifBlank { "Imported M3U" },
            description = "Imported ${matchedTracks.size} of ${entries.size} tracks",
            initialTracks = matchedTracks
        )
    }

    fun evaluateSmartPlaylist(rule: SmartPlaylistRule, library: List<MusicTrack>): List<MusicTrack> =
        Companion.evaluateSmartPlaylist(rule, library)

    companion object {
        fun evaluateSmartPlaylist(rule: SmartPlaylistRule, library: List<MusicTrack>): List<MusicTrack> {
            var result = library
            val favoriteArtists = library.filter { it.isFavorite }.map { it.artist.lowercase() }.toSet()

            for (criterion in rule.rules) {
                result = when (criterion) {
                    SmartRuleType.MOST_PLAYED -> result.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
                    SmartRuleType.RECENTLY_ADDED -> result.sortedByDescending { it.dateAddedSec }
                    SmartRuleType.DECADE -> {
                        val start = rule.decadeStartYear ?: 2010
                        result.filter { it.year in start..(start + 9) }
                    }
                    SmartRuleType.FAVORITE_ARTISTS -> result.filter { it.artist.lowercase() in favoriteArtists }
                    SmartRuleType.MIN_RATING -> result.filter { it.rating >= 4 || it.isFavorite }
                    SmartRuleType.HI_RES_AUDIO -> result.filter {
                        it.qualityInfo.isLossless || (it.qualityInfo.sampleRateHz ?: 0) > 48000 || (it.qualityInfo.bitDepth ?: 0) > 16
                    }
                    SmartRuleType.RECENTLY_UNPLAYED -> result.filter { it.playCount == 0 }
                    SmartRuleType.SHORT_TRACKS -> result.filter { it.durationMs in 1..150_000L }
                    SmartRuleType.LONG_TRACKS -> result.filter { it.durationMs >= 300_000L }
                }
            }
            return result.take(rule.maxTracks)
        }
    }

    fun buildAutomaticLocalMixes(library: List<MusicTrack>): List<Pair<String, List<MusicTrack>>> {
        if (library.size < 3) return emptyList()
        val mixes = mutableListOf<Pair<String, List<MusicTrack>>>()

        val played = library.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        if (played.size >= 3) {
            mixes.add("Heavy Rotation Mix" to played.take(25))
        }

        val lossless = library.filter { it.qualityInfo.isLossless }
        if (lossless.size >= 2) {
            mixes.add("Audiophile Lossless Shelf" to lossless.take(25))
        }

        val topGenre = library.filter { it.genre != "Unknown" }
            .groupBy { it.genre }
            .maxByOrNull { it.value.size }
        if (topGenre != null && topGenre.value.size >= 3) {
            mixes.add("${topGenre.key} Spotlight" to topGenre.value.take(25))
        }

        val neverPlayed = library.filter { it.playCount == 0 }
        if (neverPlayed.size >= 3) {
            mixes.add("Fresh & Unplayed" to neverPlayed.take(25))
        }

        return mixes
    }
}
