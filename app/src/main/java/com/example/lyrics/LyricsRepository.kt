package com.example.lyrics

import android.content.Context
import android.net.Uri
import com.example.core.model.LyricLine
import com.example.core.model.LyricsDocument
import com.example.core.model.MusicTrack
import com.example.persistence.LyricsAssociationEntity
import com.example.persistence.LyricsDao
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class LrcLibResponse(
    val id: Long? = null,
    val trackName: String? = null,
    val artistName: String? = null,
    val plainLyrics: String? = null,
    val syncedLyrics: String? = null
)

interface LrcLibApi {
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null
    ): LrcLibResponse
}

class LyricsRepository(
    private val context: Context,
    private val lyricsDao: LyricsDao
) {
    private val lrcLibApi: LrcLibApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://lrclib.net/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(LrcLibApi::class.java)
    }

    suspend fun loadLyrics(
        track: MusicTrack,
        allowOnlineLookup: Boolean
    ): LyricsDocument = withContext(Dispatchers.IO) {
        // 1. User-saved or imported LRC in Room
        lyricsDao.getLyricsForTrack(track.id)?.let { saved ->
            val parsed = LrcParser.parse(track.id, saved.lrcContent, saved.source, saved.lrcFileUri)
            return@withContext parsed.copy(offsetMs = saved.offsetMs)
        }

        // 2. Local sidecar .lrc file matching audio filename
        track.localFilePath?.let { audioPath ->
            val sidecar = File(audioPath.substringBeforeLast('.') + ".lrc")
            if (sidecar.exists() && sidecar.canRead()) {
                val content = runCatching { sidecar.readText() }.getOrNull()
                if (!content.isNullOrBlank()) {
                    return@withContext LrcParser.parse(
                        trackId = track.id,
                        rawContent = content,
                        source = "Local Sidecar (.lrc)",
                        fileUri = Uri.fromFile(sidecar).toString()
                    )
                }
            }
        }

        // 3. Embedded lyrics metadata if present
        if (!track.embeddedLyrics.isNullOrBlank()) {
            return@withContext LrcParser.parse(
                trackId = track.id,
                rawContent = track.embeddedLyrics,
                source = "Embedded Track Lyrics"
            )
        }

        // 4. Optional open LRCLIB lookup when Connected Mode + online lyrics are enabled
        if (allowOnlineLookup && track.title.isNotBlank() && track.artist != "Unknown Artist") {
            val remote = runCatching {
                lrcLibApi.getLyrics(
                    trackName = track.title,
                    artistName = track.artist,
                    albumName = track.album.takeIf { it != "Unknown Album" }
                )
            }.getOrNull()

            if (remote != null) {
                val raw = remote.syncedLyrics ?: remote.plainLyrics
                if (!raw.isNullOrBlank()) {
                    return@withContext LrcParser.parse(
                        trackId = track.id,
                        rawContent = raw,
                        source = "LRCLIB (Open Community LRC)"
                    )
                }
            }
        }

        LyricsDocument(trackId = track.id, source = "None")
    }

    suspend fun importLrcFromUri(trackId: String, uri: Uri): Result<LyricsDocument> =
        withContext(Dispatchers.IO) {
            runCatching {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    ?: error("Could not read LRC file")
                val doc = LrcParser.parse(trackId, content, "Imported .lrc", uri.toString())
                lyricsDao.saveLyrics(
                    LyricsAssociationEntity(
                        trackId = trackId,
                        lrcContent = content,
                        offsetMs = doc.offsetMs,
                        source = doc.source,
                        lrcFileUri = uri.toString()
                    )
                )
                doc
            }
        }

    suspend fun saveEditedLyrics(doc: LyricsDocument) = withContext(Dispatchers.IO) {
        val serialized = LrcParser.serialize(doc)
        lyricsDao.saveLyrics(
            LyricsAssociationEntity(
                trackId = doc.trackId,
                lrcContent = serialized,
                offsetMs = doc.offsetMs,
                source = if (doc.source == "None") "User LRC" else doc.source,
                lrcFileUri = doc.lrcFileUri
            )
        )
    }

    suspend fun updateOffset(doc: LyricsDocument, newOffsetMs: Long): LyricsDocument {
        val updated = doc.copy(offsetMs = newOffsetMs)
        saveEditedLyrics(updated)
        return updated
    }

    suspend fun updateLineTimestamp(doc: LyricsDocument, index: Int, newTimestampMs: Long): LyricsDocument {
        val mutable = doc.lines.toMutableList()
        if (index !in mutable.indices) return doc
        mutable[index] = mutable[index].copy(timestampMs = newTimestampMs.coerceAtLeast(0L))
        val updated = doc.copy(lines = mutable.sortedBy { it.timestampMs }, isSynchronized = true)
        saveEditedLyrics(updated)
        return updated
    }
}
