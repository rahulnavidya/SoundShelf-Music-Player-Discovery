package com.example.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.model.MusicTrack

@Database(
    entities = [
        TrackEntity::class,
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        PlaybackHistoryEntity::class,
        ExcludedFolderEntity::class,
        LyricsAssociationEntity::class,
        MetadataBackupEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SoundShelfDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun metadataBackupDao(): MetadataBackupDao

    companion object {
        @Volatile
        private var INSTANCE: SoundShelfDatabase? = null

        fun getInstance(context: Context): SoundShelfDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SoundShelfDatabase::class.java,
                    "soundshelf.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}

object M3uPlaylistCodec {
    fun exportToM3u8(playlistName: String, tracks: List<MusicTrack>): String {
        val sb = StringBuilder()
        sb.appendLine("#EXTM3U")
        sb.appendLine("#PLAYLIST:$playlistName")
        for (track in tracks) {
            val seconds = (track.durationMs / 1000L).coerceAtLeast(0L)
            sb.appendLine("#EXTINF:$seconds,${track.artist} - ${track.title}")
            val location = track.localFilePath ?: track.localContentUri ?: "soundshelf://${track.providerId.name.lowercase()}/${track.providerTrackId}"
            sb.appendLine(location)
        }
        return sb.toString()
    }

    data class ParsedM3uEntry(
        val title: String?,
        val artist: String?,
        val durationSec: Long?,
        val location: String
    )

    fun parseM3u(content: String): Pair<String?, List<ParsedM3uEntry>> {
        var playlistTitle: String? = null
        var pendingTitle: String? = null
        var pendingArtist: String? = null
        var pendingDuration: Long? = null
        val entries = mutableListOf<ParsedM3uEntry>()

        content.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach
            when {
                line.startsWith("#PLAYLIST:", ignoreCase = true) -> {
                    playlistTitle = line.substringAfter(":").trim().ifEmpty { null }
                }
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    val info = line.substringAfter(":")
                    val durStr = info.substringBefore(",").trim()
                    pendingDuration = durStr.toLongOrNull()
                    val display = info.substringAfter(",", "").trim()
                    if (display.contains(" - ")) {
                        pendingArtist = display.substringBefore(" - ").trim()
                        pendingTitle = display.substringAfter(" - ").trim()
                    } else if (display.isNotEmpty()) {
                        pendingTitle = display
                    }
                }
                line.startsWith("#") -> Unit
                else -> {
                    entries.add(
                        ParsedM3uEntry(
                            title = pendingTitle,
                            artist = pendingArtist,
                            durationSec = pendingDuration,
                            location = line
                        )
                    )
                    pendingTitle = null
                    pendingArtist = null
                    pendingDuration = null
                }
            }
        }
        return playlistTitle to entries
    }
}
