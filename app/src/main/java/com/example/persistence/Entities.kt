package com.example.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.AudioQualityInfo
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderCapability
import com.example.core.model.ProviderId
import com.example.core.model.QualitySourceKind
import com.example.core.model.SmartPlaylistRule
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val jsonCodec = Json { ignoreUnknownKeys = true }

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val providerId: String,
    val providerTrackId: String,
    val title: String,
    val artist: String,
    val artistId: String,
    val album: String,
    val albumId: String,
    val albumArtist: String,
    val genre: String,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val durationMs: Long,
    val artworkUri: String?,
    val localContentUri: String?,
    val localFilePath: String?,
    val folderPath: String,
    val dateAddedSec: Long,
    val fileSizeBytes: Long,
    val playCount: Int,
    val lastPlayedMs: Long,
    val isFavorite: Boolean,
    val rating: Int,
    val format: String,
    val codec: String,
    val bitrateKbps: Int?,
    val bitrateMode: String?,
    val sampleRateHz: Int?,
    val bitDepth: Int?,
    val channelCount: Int?,
    val encoder: String?,
    val qualitySourceKind: String,
    val isLossless: Boolean,
    val licenseName: String?,
    val licenseUrl: String?,
    val isDownloadPermitted: Boolean,
    val downloadUrl: String?,
    val externalPageUrl: String?,
    val isrc: String?,
    val comment: String?,
    val composer: String?,
    val copyright: String?,
    val embeddedLyrics: String?,
    val isMissingFile: Boolean = false
)

fun TrackEntity.toDomain(): MusicTrack {
    val provider = runCatching { ProviderId.valueOf(providerId) }.getOrDefault(ProviderId.LOCAL)
    val qSource = runCatching { QualitySourceKind.valueOf(qualitySourceKind) }.getOrDefault(QualitySourceKind.UNKNOWN)
    return MusicTrack(
        id = id,
        providerId = provider,
        providerTrackId = providerTrackId,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        albumArtist = albumArtist,
        genre = genre,
        trackNumber = trackNumber,
        discNumber = discNumber,
        year = year,
        durationMs = durationMs,
        artworkUri = artworkUri,
        localContentUri = localContentUri,
        localFilePath = localFilePath,
        folderPath = folderPath,
        dateAddedSec = dateAddedSec,
        fileSizeBytes = fileSizeBytes,
        playCount = playCount,
        lastPlayedMs = lastPlayedMs,
        isFavorite = isFavorite,
        rating = rating,
        qualityInfo = AudioQualityInfo(
            format = format,
            codec = codec,
            bitrateKbps = bitrateKbps,
            bitrateMode = bitrateMode,
            sampleRateHz = sampleRateHz,
            bitDepth = bitDepth,
            channelCount = channelCount,
            fileSizeBytes = if (fileSizeBytes > 0) fileSizeBytes else null,
            encoder = encoder,
            sourceKind = qSource,
            isLossless = isLossless
        ),
        licenseName = licenseName,
        licenseUrl = licenseUrl,
        isDownloadPermitted = isDownloadPermitted,
        downloadUrl = downloadUrl,
        externalPageUrl = externalPageUrl,
        isrc = isrc,
        comment = comment,
        composer = composer,
        copyright = copyright,
        embeddedLyrics = embeddedLyrics,
        capabilities = if (provider == ProviderId.LOCAL) {
            setOf(
                ProviderCapability.STREAM_PLAYBACK,
                ProviderCapability.TRACK_DETAILS,
                ProviderCapability.LYRICS,
                ProviderCapability.PLAYLIST_MANAGEMENT
            )
        } else {
            setOf(ProviderCapability.STREAM_PLAYBACK, ProviderCapability.TRACK_DETAILS)
        },
        canJoinUnifiedQueue = provider != ProviderId.SPOTIFY && provider != ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL
    )
}

fun MusicTrack.toEntity(isMissingFile: Boolean = false): TrackEntity = TrackEntity(
    id = id,
    providerId = providerId.name,
    providerTrackId = providerTrackId,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    albumArtist = albumArtist,
    genre = genre,
    trackNumber = trackNumber,
    discNumber = discNumber,
    year = year,
    durationMs = durationMs,
    artworkUri = artworkUri,
    localContentUri = localContentUri,
    localFilePath = localFilePath,
    folderPath = folderPath,
    dateAddedSec = dateAddedSec,
    fileSizeBytes = fileSizeBytes,
    playCount = playCount,
    lastPlayedMs = lastPlayedMs,
    isFavorite = isFavorite,
    rating = rating,
    format = qualityInfo.format,
    codec = qualityInfo.codec,
    bitrateKbps = qualityInfo.bitrateKbps,
    bitrateMode = qualityInfo.bitrateMode,
    sampleRateHz = qualityInfo.sampleRateHz,
    bitDepth = qualityInfo.bitDepth,
    channelCount = qualityInfo.channelCount,
    encoder = qualityInfo.encoder,
    qualitySourceKind = qualityInfo.sourceKind.name,
    isLossless = qualityInfo.isLossless,
    licenseName = licenseName,
    licenseUrl = licenseUrl,
    isDownloadPermitted = isDownloadPermitted,
    downloadUrl = downloadUrl,
    externalPageUrl = externalPageUrl,
    isrc = isrc,
    comment = comment,
    composer = composer,
    copyright = copyright,
    embeddedLyrics = embeddedLyrics,
    isMissingFile = isMissingFile
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val providerId: String,
    val isSmart: Boolean,
    val isFavorite: Boolean,
    val smartRuleJson: String?,
    val artworkUri: String?,
    val createdAtMs: Long
)

fun PlaylistEntity.toDomain(trackIds: List<String> = emptyList()): MusicPlaylist = MusicPlaylist(
    id = id,
    name = name,
    description = description,
    providerId = runCatching { ProviderId.valueOf(providerId) }.getOrDefault(ProviderId.LOCAL),
    isSmart = isSmart,
    isFavorite = isFavorite,
    smartRule = smartRuleJson?.let { runCatching { jsonCodec.decodeFromString<SmartPlaylistRule>(it) }.getOrNull() },
    trackIds = trackIds,
    artworkUri = artworkUri,
    createdAtMs = createdAtMs
)

fun MusicPlaylist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    providerId = providerId.name,
    isSmart = isSmart,
    isFavorite = isFavorite,
    smartRuleJson = smartRule?.let { jsonCodec.encodeToString(it) },
    artworkUri = artworkUri,
    createdAtMs = createdAtMs
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "position"]
)
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val position: Int,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackId: String,
    val providerId: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUri: String?,
    val playedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val displayName: String,
    val addedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "lyrics_associations")
data class LyricsAssociationEntity(
    @PrimaryKey val trackId: String,
    val lrcContent: String,
    val offsetMs: Long = 0L,
    val source: String = "Local LRC",
    val lrcFileUri: String? = null,
    val updatedAtMs: Long = System.currentTimeMillis()
)

@Entity(tableName = "metadata_backups")
data class MetadataBackupEntity(
    @PrimaryKey val trackId: String,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val genre: String,
    val year: Int,
    val trackNumber: Int,
    val discNumber: Int,
    val composer: String?,
    val copyright: String?,
    val comment: String?,
    val artworkUri: String?,
    val savedAtMs: Long = System.currentTimeMillis()
)
