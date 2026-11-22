package com.example.library

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.core.content.ContextCompat
import com.example.core.model.AudioQualityInfo
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderCapability
import com.example.core.model.ProviderId
import com.example.core.model.QualitySourceKind
import com.example.persistence.ExcludedFolderDao
import com.example.persistence.TrackDao
import com.example.persistence.toDomain
import com.example.persistence.toEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class LibraryScanReport(
    val totalTracks: Int,
    val newTracksAdded: Int,
    val missingMarked: Int,
    val duplicatesDetected: Int
)

class MediaStoreScanner(
    private val context: Context,
    private val trackDao: TrackDao,
    private val excludedFolderDao: ExcludedFolderDao
) {
    fun hasAudioReadPermission(): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Incremental MediaStore scan: preserves user play counts, favorites, ratings, and edited tags
     * while detecting new files and marking removed/missing files.
     */
    suspend fun scanMediaStoreIncremental(forceDeepInspect: Boolean = false): LibraryScanReport =
        withContext(Dispatchers.IO) {
            if (!hasAudioReadPermission()) {
                val existing = trackDao.getAllLocalTrackSnapshots().filter { !it.isMissingFile }
                return@withContext LibraryScanReport(existing.size, 0, 0, 0)
            }

            val excludedPaths = excludedFolderDao.getExcludedFolderPaths()
            val existingById = trackDao.getAllLocalTrackSnapshots().associateBy { it.id }

            val projection = buildList {
                add(MediaStore.Audio.Media._ID)
                add(MediaStore.Audio.Media.TITLE)
                add(MediaStore.Audio.Media.ARTIST)
                add(MediaStore.Audio.Media.ARTIST_ID)
                add(MediaStore.Audio.Media.ALBUM)
                add(MediaStore.Audio.Media.ALBUM_ID)
                add(MediaStore.Audio.Media.DURATION)
                add(MediaStore.Audio.Media.SIZE)
                add(MediaStore.Audio.Media.DATE_ADDED)
                add(MediaStore.Audio.Media.YEAR)
                add(MediaStore.Audio.Media.TRACK)
                add(MediaStore.Audio.Media.MIME_TYPE)
                add(MediaStore.Audio.Media.DATA)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    add(MediaStore.Audio.Media.ALBUM_ARTIST)
                    add(MediaStore.Audio.Media.GENRE)
                    add(MediaStore.Audio.Media.BITRATE)
                }
            }.toTypedArray()

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            val discoveredIds = mutableSetOf<String>()
            val toUpsert = mutableListOf<com.example.persistence.TrackEntity>()
            var newCount = 0

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumArtistCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ARTIST)
                } else -1
                val genreCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.GENRE)
                } else -1
                val bitrateCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                } else -1

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val filePath = cursor.getString(dataCol)
                    val folderPath = filePath?.let { File(it).parent } ?: ""

                    if (excludedPaths.any { excluded -> folderPath.startsWith(excluded, ignoreCase = true) }) {
                        continue
                    }

                    val durationMs = cursor.getLong(durCol)
                    if (durationMs < 3000L) continue // Skip tiny UI notification/ringtone clips

                    val trackId = "local_$mediaId"
                    discoveredIds.add(trackId)

                    val existing = existingById[trackId]
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        mediaId
                    )
                    val albumId = cursor.getLong(albumIdCol)
                    val artworkUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    if (existing != null && !forceDeepInspect && !existing.isMissingFile) {
                        continue // Incremental skip: already indexed
                    }

                    val rawTitle = cursor.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: filePath?.let { File(it).nameWithoutExtension }
                        ?: "Untitled Track"
                    val rawArtist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                        ?: "Unknown Artist"
                    val rawAlbum = cursor.getString(albumCol)?.takeIf { it.isNotBlank() }
                        ?: "Unknown Album"
                    val rawAlbumArtist = if (albumArtistCol >= 0) {
                        cursor.getString(albumArtistCol)?.takeIf { it.isNotBlank() } ?: rawArtist
                    } else rawArtist
                    val rawGenre = if (genreCol >= 0) {
                        cursor.getString(genreCol)?.takeIf { it.isNotBlank() } ?: "Unknown"
                    } else "Unknown"
                    val rawTrackNum = cursor.getInt(trackCol) % 1000
                    val rawDiscNum = (cursor.getInt(trackCol) / 1000).coerceAtLeast(1)
                    val rawYear = cursor.getInt(yearCol)
                    val sizeBytes = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol)
                    val mime = cursor.getString(mimeCol)

                    val inspected = if (forceDeepInspect || existing == null) {
                        AudioInspector.inspectUri(context, contentUri, filePath)
                    } else null

                    val fallbackBitrate = if (bitrateCol >= 0) {
                        val bps = cursor.getInt(bitrateCol)
                        if (bps > 0) bps / 1000 else null
                    } else null

                    val quality = inspected?.qualityInfo ?: AudioQualityInfo(
                        format = filePath?.substringAfterLast('.', "")?.uppercase()?.ifBlank { "Audio" } ?: "Audio",
                        codec = mime ?: "Audio",
                        bitrateKbps = fallbackBitrate,
                        fileSizeBytes = sizeBytes,
                        sourceKind = QualitySourceKind.ACTUAL_FILE
                    )

                    val track = MusicTrack(
                        id = trackId,
                        providerId = ProviderId.LOCAL,
                        providerTrackId = mediaId.toString(),
                        title = existing?.title ?: rawTitle,
                        artist = existing?.artist ?: rawArtist,
                        artistId = "local_artist_${cursor.getLong(artistIdCol)}",
                        album = existing?.album ?: rawAlbum,
                        albumId = "local_album_$albumId",
                        albumArtist = existing?.albumArtist ?: rawAlbumArtist,
                        genre = existing?.genre ?: (inspected?.genre ?: rawGenre),
                        trackNumber = existing?.trackNumber ?: (inspected?.trackNumber ?: rawTrackNum),
                        discNumber = existing?.discNumber ?: (inspected?.discNumber ?: rawDiscNum),
                        year = existing?.year ?: (inspected?.year ?: rawYear),
                        durationMs = durationMs,
                        artworkUri = existing?.artworkUri ?: artworkUri,
                        localContentUri = contentUri.toString(),
                        localFilePath = filePath,
                        folderPath = folderPath,
                        dateAddedSec = dateAdded,
                        fileSizeBytes = sizeBytes,
                        playCount = existing?.playCount ?: 0,
                        lastPlayedMs = existing?.lastPlayedMs ?: 0L,
                        isFavorite = existing?.isFavorite ?: false,
                        rating = existing?.rating ?: 0,
                        qualityInfo = quality,
                        composer = existing?.composer ?: inspected?.composer,
                        comment = existing?.comment,
                        copyright = existing?.copyright,
                        capabilities = setOf(
                            ProviderCapability.STREAM_PLAYBACK,
                            ProviderCapability.TRACK_DETAILS,
                            ProviderCapability.LYRICS,
                            ProviderCapability.PLAYLIST_MANAGEMENT
                        )
                    )
                    toUpsert.add(track.toEntity(isMissingFile = false))
                    if (existing == null) newCount++
                }
            }

            if (toUpsert.isNotEmpty()) {
                trackDao.upsertTracks(toUpsert)
            }

            // Detect missing MediaStore files (leave SAF-imported tracks intact unless missing)
            val missingIds = existingById.values
                .filter { it.id.startsWith("local_") && it.id !in discoveredIds && !it.isMissingFile }
                .map { it.id }
            if (missingIds.isNotEmpty()) {
                trackDao.markTracksMissing(missingIds)
            }

            val allActive = trackDao.getAllLocalTrackSnapshots().filter { !it.isMissingFile }.map { it.toDomain() }
            val duplicates = findDuplicates(allActive).size

            LibraryScanReport(
                totalTracks = allActive.size,
                newTracksAdded = newCount,
                missingMarked = missingIds.size,
                duplicatesDetected = duplicates
            )
        }

    suspend fun importSafAudioFile(uri: Uri): MusicTrack? = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val inspected = AudioInspector.inspectUri(context, uri, null)
        val displayName = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) c.getString(idx) else null
                } else null
            } ?: "Imported Track"

        val id = "saf_${UUID.nameUUIDFromBytes(uri.toString().toByteArray())}"
        val track = MusicTrack(
            id = id,
            providerId = ProviderId.LOCAL,
            providerTrackId = id,
            title = inspected.title?.takeIf { it.isNotBlank() } ?: displayName.substringBeforeLast('.'),
            artist = inspected.artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
            artistId = "saf_artist_${(inspected.artist ?: "unknown").lowercase().hashCode()}",
            album = inspected.album?.takeIf { it.isNotBlank() } ?: "Imported Files",
            albumId = "saf_album_${(inspected.album ?: "imported").lowercase().hashCode()}",
            albumArtist = inspected.albumArtist ?: (inspected.artist ?: "Unknown Artist"),
            genre = inspected.genre ?: "Unknown",
            trackNumber = inspected.trackNumber ?: 0,
            discNumber = inspected.discNumber ?: 1,
            year = inspected.year ?: 0,
            durationMs = inspected.durationMs ?: 0L,
            localContentUri = uri.toString(),
            folderPath = "SAF Imported",
            dateAddedSec = System.currentTimeMillis() / 1000L,
            fileSizeBytes = inspected.fileSizeBytes ?: 0L,
            qualityInfo = inspected.qualityInfo,
            composer = inspected.composer
        )
        trackDao.upsertTrack(track.toEntity())
        track
    }

    fun findDuplicates(tracks: List<MusicTrack>): List<List<MusicTrack>> {
        return tracks
            .groupBy { "${it.title.lowercase().trim()}|${it.artist.lowercase().trim()}|${it.durationMs / 2000L}" }
            .values
            .filter { it.size > 1 }
    }
}
