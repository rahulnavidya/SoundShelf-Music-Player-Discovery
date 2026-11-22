package com.example.persistence

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks WHERE providerId = 'LOCAL' AND isMissingFile = 0 ORDER BY title COLLATE NOCASE ASC")
    fun observeLocalTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE isMissingFile = 0")
    fun observeAllKnownTracks(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks WHERE id = :id LIMIT 1")
    suspend fun getTrackById(id: String): TrackEntity?

    @Query("SELECT * FROM tracks WHERE providerId = 'LOCAL'")
    suspend fun getAllLocalTrackSnapshots(): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<TrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTrack(track: TrackEntity)

    @Query("UPDATE tracks SET isMissingFile = 1 WHERE id IN (:ids)")
    suspend fun markTracksMissing(ids: List<String>)

    @Query("DELETE FROM tracks WHERE id IN (:ids)")
    suspend fun deleteTracksByIds(ids: List<String>)

    @Query("UPDATE tracks SET isFavorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)

    @Query("UPDATE tracks SET rating = :rating WHERE id = :id")
    suspend fun setRating(id: String, rating: Int)

    @Query("UPDATE tracks SET playCount = playCount + 1, lastPlayedMs = :timestampMs WHERE id = :id")
    suspend fun incrementPlayCount(id: String, timestampMs: Long)

    @Query("UPDATE tracks SET title = :title, artist = :artist, album = :album, albumArtist = :albumArtist, genre = :genre, year = :year, trackNumber = :trackNumber, discNumber = :discNumber, composer = :composer, copyright = :copyright, comment = :comment, artworkUri = :artworkUri WHERE id = :id")
    suspend fun updateMetadata(
        id: String,
        title: String,
        artist: String,
        album: String,
        albumArtist: String,
        genre: String,
        year: Int,
        trackNumber: Int,
        discNumber: Int,
        composer: String?,
        copyright: String?,
        comment: String?,
        artworkUri: String?
    )
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAtMs DESC")
    fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlist_tracks ORDER BY playlistId, position ASC")
    fun observeAllCrossRefs(): Flow<List<PlaylistTrackCrossRef>>

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun getCrossRefsForPlaylist(playlistId: String): List<PlaylistTrackCrossRef>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRefs(refs: List<PlaylistTrackCrossRef>)

    @Query("UPDATE playlist_tracks SET trackId = :newTrackId WHERE trackId = :oldTrackId")
    suspend fun replaceTrackReferences(oldTrackId: String, newTrackId: String)

    @Transaction
    suspend fun replacePlaylistTrackOrder(playlistId: String, trackIds: List<String>) {
        clearPlaylistTracks(playlistId)
        val refs = trackIds.mapIndexed { index, trackId ->
            PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId, position = index)
        }
        if (refs.isNotEmpty()) insertCrossRefs(refs)
    }
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM playback_history ORDER BY playedAtMs DESC LIMIT 200")
    fun observeRecentHistory(): Flow<List<PlaybackHistoryEntity>>

    @Insert
    suspend fun insertHistory(entry: PlaybackHistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("UPDATE playback_history SET trackId = :newTrackId WHERE trackId = :oldTrackId")
    suspend fun replaceTrackReferences(oldTrackId: String, newTrackId: String)
}

@Dao
interface ExcludedFolderDao {
    @Query("SELECT * FROM excluded_folders ORDER BY displayName ASC")
    fun observeExcludedFolders(): Flow<List<ExcludedFolderEntity>>

    @Query("SELECT folderPath FROM excluded_folders")
    suspend fun getExcludedFolderPaths(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addExcludedFolder(folder: ExcludedFolderEntity)

    @Query("DELETE FROM excluded_folders WHERE folderPath = :path")
    suspend fun removeExcludedFolder(path: String)
}

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_associations WHERE trackId = :trackId LIMIT 1")
    fun observeLyricsForTrack(trackId: String): Flow<LyricsAssociationEntity?>

    @Query("SELECT * FROM lyrics_associations WHERE trackId = :trackId LIMIT 1")
    suspend fun getLyricsForTrack(trackId: String): LyricsAssociationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLyrics(entity: LyricsAssociationEntity)

    @Query("DELETE FROM lyrics_associations WHERE trackId = :trackId")
    suspend fun deleteLyrics(trackId: String)
}

@Dao
interface MetadataBackupDao {
    @Query("SELECT * FROM metadata_backups WHERE trackId = :trackId LIMIT 1")
    suspend fun getBackup(trackId: String): MetadataBackupEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveBackup(backup: MetadataBackupEntity)

    @Query("DELETE FROM metadata_backups WHERE trackId = :trackId")
    suspend fun clearBackup(trackId: String)
}
