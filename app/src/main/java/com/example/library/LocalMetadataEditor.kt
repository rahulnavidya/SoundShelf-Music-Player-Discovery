package com.example.library

import android.content.Context
import android.net.Uri
import com.example.core.model.MusicTrack
import com.example.persistence.MetadataBackupDao
import com.example.persistence.MetadataBackupEntity
import com.example.persistence.TrackDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class MetadataEditRequest(
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
    val newArtworkUri: Uri? = null
)

/**
 * Safely updates local track metadata in the SoundShelf database and caches custom artwork
 * without re-encoding or risking audio stream corruption. Always stores a reversible undo snapshot.
 */
class LocalMetadataEditor(
    private val context: Context,
    private val trackDao: TrackDao,
    private val backupDao: MetadataBackupDao
) {
    suspend fun updateSingleTrack(track: MusicTrack, edit: MetadataEditRequest): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Save undo snapshot if none exists yet for this edit cycle
                backupDao.saveBackup(
                    MetadataBackupEntity(
                        trackId = track.id,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        albumArtist = track.albumArtist,
                        genre = track.genre,
                        year = track.year,
                        trackNumber = track.trackNumber,
                        discNumber = track.discNumber,
                        composer = track.composer,
                        copyright = track.copyright,
                        comment = track.comment,
                        artworkUri = track.artworkUri
                    )
                )

                val resolvedArtworkUri = edit.newArtworkUri?.let { cachePickedArtwork(track.id, it) }
                    ?: track.artworkUri

                trackDao.updateMetadata(
                    id = track.id,
                    title = edit.title.trim().ifBlank { track.title },
                    artist = edit.artist.trim().ifBlank { track.artist },
                    album = edit.album.trim().ifBlank { track.album },
                    albumArtist = edit.albumArtist.trim().ifBlank { edit.artist },
                    genre = edit.genre.trim().ifBlank { "Unknown" },
                    year = edit.year.coerceAtLeast(0),
                    trackNumber = edit.trackNumber.coerceAtLeast(0),
                    discNumber = edit.discNumber.coerceAtLeast(1),
                    composer = edit.composer?.trim()?.takeIf { it.isNotEmpty() },
                    copyright = edit.copyright?.trim()?.takeIf { it.isNotEmpty() },
                    comment = edit.comment?.trim()?.takeIf { it.isNotEmpty() },
                    artworkUri = resolvedArtworkUri
                )
            }
        }

    suspend fun batchUpdateAlbumArtistAndGenre(
        tracks: List<MusicTrack>,
        artist: String?,
        album: String?,
        albumArtist: String?,
        genre: String?,
        year: Int?
    ): Int = withContext(Dispatchers.IO) {
        var count = 0
        for (track in tracks) {
            val res = updateSingleTrack(
                track = track,
                edit = MetadataEditRequest(
                    title = track.title,
                    artist = artist?.takeIf { it.isNotBlank() } ?: track.artist,
                    album = album?.takeIf { it.isNotBlank() } ?: track.album,
                    albumArtist = albumArtist?.takeIf { it.isNotBlank() } ?: track.albumArtist,
                    genre = genre?.takeIf { it.isNotBlank() } ?: track.genre,
                    year = year ?: track.year,
                    trackNumber = track.trackNumber,
                    discNumber = track.discNumber,
                    composer = track.composer,
                    copyright = track.copyright,
                    comment = track.comment
                )
            )
            if (res.isSuccess) count++
        }
        count
    }

    suspend fun undoLastEdit(trackId: String): Boolean = withContext(Dispatchers.IO) {
        val backup = backupDao.getBackup(trackId) ?: return@withContext false
        trackDao.updateMetadata(
            id = backup.trackId,
            title = backup.title,
            artist = backup.artist,
            album = backup.album,
            albumArtist = backup.albumArtist,
            genre = backup.genre,
            year = backup.year,
            trackNumber = backup.trackNumber,
            discNumber = backup.discNumber,
            composer = backup.composer,
            copyright = backup.copyright,
            comment = backup.comment,
            artworkUri = backup.artworkUri
        )
        backupDao.clearBackup(trackId)
        true
    }

    suspend fun hasUndoSnapshot(trackId: String): Boolean =
        withContext(Dispatchers.IO) { backupDao.getBackup(trackId) != null }

    private fun cachePickedArtwork(trackId: String, sourceUri: Uri): String? {
        return runCatching {
            val dir = File(context.filesDir, "custom_artwork").apply { mkdirs() }
            val outFile = File(dir, "${trackId.replace(Regex("[^a-zA-Z0-9_]"), "_")}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(outFile).toString()
        }.getOrNull()
    }
}
