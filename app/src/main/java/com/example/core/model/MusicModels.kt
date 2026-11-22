package com.example.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class ProviderId(
    val displayName: String,
    val badgeLabel: String,
    val isExperimental: Boolean = false
) {
    LOCAL("Local Library", "Local"),
    AUDIUS("Audius", "Audius"),
    JAMENDO("Jamendo", "Jamendo CC"),
    YOUTUBE_MUSIC_EXPERIMENTAL("YouTube Music (Experimental)", "YT Music (Exp)", isExperimental = true),
    SPOTIFY("Spotify (External)", "Spotify"),
    EXTERNAL_STORE("Authorised Store", "Store")
}

@Serializable
enum class ProviderCapability {
    SEARCH,
    TRACK_DETAILS,
    ARTIST_DETAILS,
    ALBUM_DETAILS,
    PLAYLIST_DETAILS,
    STREAM_PLAYBACK,
    LYRICS,
    RECOMMENDATIONS,
    DOWNLOAD_LINKS,
    DIRECT_DOWNLOAD,
    ACCOUNT_CONNECTION,
    PLAYLIST_MANAGEMENT,
    EXTERNAL_HANDOFF
}

@Serializable
enum class QualitySourceKind(val label: String) {
    ACTUAL_FILE("Actual File"),
    PROVIDER_REPORTED("Provider Reported"),
    STREAMING("Streaming"),
    ESTIMATED("Estimated"),
    UNKNOWN("Unknown")
}

@Serializable
data class AudioQualityInfo(
    val format: String = "Unknown",
    val codec: String = "Unknown",
    val bitrateKbps: Int? = null,
    val bitrateMode: String? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val channelCount: Int? = null,
    val fileSizeBytes: Long? = null,
    val encoder: String? = null,
    val sourceKind: QualitySourceKind = QualitySourceKind.UNKNOWN,
    val isLossless: Boolean = false
) {
    fun summaryBadge(): String {
        val parts = mutableListOf<String>()
        if (format != "Unknown") parts.add(format.uppercase())
        if (bitDepth != null && sampleRateHz != null) {
            parts.add("${bitDepth}-bit/${sampleRateHz / 1000}kHz")
        } else if (bitrateKbps != null) {
            parts.add("${bitrateKbps} kbps")
        }
        return if (parts.isEmpty()) "Audio" else parts.joinToString(" · ")
    }
}

@Serializable
data class MusicTrack(
    val id: String,
    val providerId: ProviderId = ProviderId.LOCAL,
    val providerTrackId: String = id,
    val title: String,
    val artist: String,
    val artistId: String = "",
    val album: String = "Unknown Album",
    val albumId: String = "",
    val albumArtist: String = artist,
    val genre: String = "Unknown",
    val trackNumber: Int = 0,
    val discNumber: Int = 1,
    val year: Int = 0,
    val durationMs: Long = 0L,
    val artworkUri: String? = null,
    val localContentUri: String? = null,
    val localFilePath: String? = null,
    val folderPath: String = "",
    val dateAddedSec: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val playCount: Int = 0,
    val lastPlayedMs: Long = 0L,
    val isFavorite: Boolean = false,
    val rating: Int = 0,
    val qualityInfo: AudioQualityInfo = AudioQualityInfo(),
    val licenseName: String? = null,
    val licenseUrl: String? = null,
    val isDownloadPermitted: Boolean = false,
    val downloadUrl: String? = null,
    val externalPageUrl: String? = null,
    val isrc: String? = null,
    val comment: String? = null,
    val composer: String? = null,
    val copyright: String? = null,
    val embeddedLyrics: String? = null,
    val capabilities: Set<ProviderCapability> = setOf(
        ProviderCapability.STREAM_PLAYBACK,
        ProviderCapability.TRACK_DETAILS
    ),
    val canJoinUnifiedQueue: Boolean = providerId != ProviderId.SPOTIFY && providerId != ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL
)

@Serializable
data class MusicAlbum(
    val id: String,
    val providerId: ProviderId,
    val title: String,
    val artist: String,
    val year: Int = 0,
    val artworkUri: String? = null,
    val trackCount: Int = 0,
    val totalDurationMs: Long = 0L,
    val externalUrl: String? = null
)

@Serializable
data class MusicArtist(
    val id: String,
    val providerId: ProviderId,
    val name: String,
    val artworkUri: String? = null,
    val trackCount: Int = 0,
    val albumCount: Int = 0,
    val bio: String? = null,
    val externalUrl: String? = null
)

@Serializable
enum class SmartRuleType(val label: String) {
    MOST_PLAYED("Most Played"),
    RECENTLY_ADDED("Recently Added"),
    DECADE("Selected Decade"),
    FAVORITE_ARTISTS("Favourite Artists"),
    MIN_RATING("Rated 4+ Stars"),
    HI_RES_AUDIO("High-Resolution Audio"),
    RECENTLY_UNPLAYED("Recently Unplayed"),
    SHORT_TRACKS("Short Tracks (< 2m 30s)"),
    LONG_TRACKS("Long Tracks (> 5m)")
}

@Serializable
data class SmartPlaylistRule(
    val rules: List<SmartRuleType> = emptyList(),
    val decadeStartYear: Int? = null,
    val maxTracks: Int = 50
)

@Serializable
data class MusicPlaylist(
    val id: String,
    val name: String,
    val description: String = "",
    val providerId: ProviderId = ProviderId.LOCAL,
    val isSmart: Boolean = false,
    val isFavorite: Boolean = false,
    val smartRule: SmartPlaylistRule? = null,
    val trackIds: List<String> = emptyList(),
    val artworkUri: String? = null,
    val createdAtMs: Long = System.currentTimeMillis(),
    val externalUrl: String? = null
)

@Serializable
data class QueueEntry(
    val entryId: String,
    val track: MusicTrack,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Serializable
enum class RepeatMode {
    OFF, ALL, ONE
}

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String,
    val translation: String? = null
)

@Serializable
data class LyricsDocument(
    val trackId: String,
    val lines: List<LyricLine> = emptyList(),
    val plainText: String = "",
    val source: String = "Local",
    val offsetMs: Long = 0L,
    val isSynchronized: Boolean = false,
    val lrcFileUri: String? = null
)

@Serializable
data class DownloadSourceResult(
    val id: String,
    val sourceName: String,
    val providerId: ProviderId,
    val title: String,
    val artist: String,
    val album: String = "",
    val versionTag: String = "Original Release",
    val format: String,
    val bitrateKbps: Int? = null,
    val sampleRateHz: Int? = null,
    val bitDepth: Int? = null,
    val fileSizeLabel: String? = null,
    val isLossless: Boolean = false,
    val license: String,
    val priceLabel: String = "Check Source",
    val isDirectDownload: Boolean = false,
    val url: String,
    val matchConfidence: String = "Exact Match"
)

@Serializable
enum class HomeSectionId(val title: String) {
    HERO_BANNER("Studio Spotlight"),
    QUICK_ACCESS("Quick Access"),
    CONTINUE_LISTENING("Continue Listening"),
    RECENTLY_PLAYED("Recently Played"),
    LOCAL_HIGHLIGHTS("Local Library Highlights"),
    ONLINE_DISCOVERY("Online Discovery"),
    SMART_MIXES("Smart Mixes & Playlists")
}

enum class LibraryCategory(val label: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    ALBUM_ARTISTS("Album Artists"),
    GENRES("Genres"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists"),
    RECENTLY_ADDED("Recently Added"),
    RECENTLY_PLAYED("Recently Played"),
    MOST_PLAYED("Most Played"),
    FAVORITES("Favourites")
}

enum class TrackSortOption(val label: String) {
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    DURATION("Duration"),
    DATE_ADDED("Date Added"),
    YEAR("Release Year"),
    FILE_SIZE("File Size"),
    PLAY_COUNT("Play Count")
}
