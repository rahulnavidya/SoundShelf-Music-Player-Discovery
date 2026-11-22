package com.example.downloadfinder

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.example.core.model.DownloadSourceResult
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.library.AudioInspector
import com.example.persistence.HistoryDao
import com.example.persistence.PlaylistDao
import com.example.persistence.TrackDao
import com.example.persistence.toEntity
import com.example.providers.ProviderManager
import com.example.providers.ProviderResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit

data class DownloadFinderQuery(
    val title: String,
    val artist: String,
    val album: String = "",
    val year: Int = 0,
    val version: String = "Any",
    val isrc: String = ""
)

data class DownloadedFileVerification(
    val importedTrack: MusicTrack,
    val sha256Checksum: String,
    val localPath: String
)

class DownloadFinderRepository(
    private val context: Context,
    private val providerManager: ProviderManager,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val historyDao: HistoryDao
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun detectVersionTag(title: String): String {
        val lower = title.lowercase()
        return when {
            "remaster" in lower -> "Remaster"
            "radio edit" in lower -> "Radio Edit"
            "extended" in lower -> "Extended Edition"
            "remix" in lower -> "Remix"
            "live" in lower -> "Live Recording"
            "instrumental" in lower -> "Instrumental"
            "acoustic" in lower -> "Acoustic"
            "clean" in lower -> "Clean Version"
            "explicit" in lower -> "Explicit Version"
            else -> "Original Release"
        }
    }

    suspend fun searchLegitimateSources(
        query: DownloadFinderQuery,
        isLocalOnlyMode: Boolean,
        enabledProviders: Set<ProviderId>
    ): List<DownloadSourceResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DownloadSourceResult>()
        val searchTerm = listOf(query.artist, query.title).filter { it.isNotBlank() }.joinToString(" ").trim()
        if (searchTerm.isBlank()) return@withContext emptyList()

        // 1. Check creator-permitted direct downloads on Audius & Jamendo (only if Connected Mode)
        if (!isLocalOnlyMode) {
            if (ProviderId.AUDIUS in enabledProviders) {
                val audiusRes = providerManager.audius.searchTracks(searchTerm, limit = 12)
                if (audiusRes is ProviderResult.Success) {
                    audiusRes.data.filter { it.isDownloadPermitted && !it.downloadUrl.isNullOrBlank() }.forEach { track ->
                        val conf = computeConfidence(query, track.title, track.artist, track.isrc)
                        results.add(
                            DownloadSourceResult(
                                id = "dl_audius_${track.providerTrackId}",
                                sourceName = "Audius Creator Download",
                                providerId = ProviderId.AUDIUS,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                versionTag = detectVersionTag(track.title),
                                format = "Original / Creator File",
                                bitrateKbps = track.qualityInfo.bitrateKbps,
                                isLossless = false,
                                license = track.licenseName ?: "Audius Creator Authorised",
                                priceLabel = "Free (Creator Permitted)",
                                isDirectDownload = true,
                                url = track.downloadUrl!!,
                                matchConfidence = conf
                            )
                        )
                    }
                }
            }

            if (ProviderId.JAMENDO in enabledProviders) {
                val jamendoRes = providerManager.jamendo.searchTracks(searchTerm, limit = 12)
                if (jamendoRes is ProviderResult.Success) {
                    jamendoRes.data.filter { it.isDownloadPermitted && !it.downloadUrl.isNullOrBlank() }.forEach { track ->
                        val conf = computeConfidence(query, track.title, track.artist, track.isrc)
                        results.add(
                            DownloadSourceResult(
                                id = "dl_jamendo_${track.providerTrackId}",
                                sourceName = "Jamendo CC Download",
                                providerId = ProviderId.JAMENDO,
                                title = track.title,
                                artist = track.artist,
                                album = track.album,
                                versionTag = detectVersionTag(track.title),
                                format = "MP3",
                                bitrateKbps = track.qualityInfo.bitrateKbps,
                                isLossless = false,
                                license = track.licenseName ?: "Creative Commons",
                                priceLabel = "Free (CC Licensed)",
                                isDirectDownload = true,
                                url = track.downloadUrl!!,
                                matchConfidence = conf
                            )
                        )
                    }
                }
            }
        }

        // 2. Always offer official authorised digital music store lookup pages (never auto-scraped or bypassed)
        val encoded = Uri.encode(searchTerm)
        val versionSuffix = if (query.version != "Any") " ${query.version}" else ""
        val fullQueryEncoded = Uri.encode("$searchTerm$versionSuffix")

        results.add(
            DownloadSourceResult(
                id = "store_qobuz_$encoded",
                sourceName = "Qobuz Store",
                providerId = ProviderId.EXTERNAL_STORE,
                title = query.title.ifBlank { searchTerm },
                artist = query.artist.ifBlank { "Search Qobuz" },
                album = query.album,
                versionTag = if (query.version == "Any") detectVersionTag(query.title) else query.version,
                format = "FLAC / ALAC / WAV / AIFF (Up to 24-bit/192kHz)",
                isLossless = true,
                license = "Commercial Purchase",
                priceLabel = "View Store Pricing",
                isDirectDownload = false,
                url = "https://www.qobuz.com/us-en/search?q=$fullQueryEncoded",
                matchConfidence = "Official Store Lookup"
            )
        )
        results.add(
            DownloadSourceResult(
                id = "store_bandcamp_$encoded",
                sourceName = "Bandcamp (Artist Direct)",
                providerId = ProviderId.EXTERNAL_STORE,
                title = query.title.ifBlank { searchTerm },
                artist = query.artist.ifBlank { "Search Bandcamp" },
                album = query.album,
                versionTag = if (query.version == "Any") detectVersionTag(query.title) else query.version,
                format = "FLAC / WAV / ALAC / MP3 / AAC",
                isLossless = true,
                license = "Artist-Authorised Download / Purchase",
                priceLabel = "Artist Set / NYP",
                isDirectDownload = false,
                url = "https://bandcamp.com/search?q=$fullQueryEncoded",
                matchConfidence = "Official Store Lookup"
            )
        )
        results.add(
            DownloadSourceResult(
                id = "store_7digital_$encoded",
                sourceName = "7digital",
                providerId = ProviderId.EXTERNAL_STORE,
                title = query.title.ifBlank { searchTerm },
                artist = query.artist.ifBlank { "Search 7digital" },
                album = query.album,
                versionTag = if (query.version == "Any") detectVersionTag(query.title) else query.version,
                format = "FLAC / M4A / MP3",
                isLossless = true,
                license = "Commercial Purchase",
                priceLabel = "View Store Pricing",
                isDirectDownload = false,
                url = "https://www.7digital.com/search?q=$fullQueryEncoded",
                matchConfidence = "Official Store Lookup"
            )
        )
        results
    }

    suspend fun downloadAuthorizedCopyAndImport(source: DownloadSourceResult): Result<DownloadedFileVerification> =
        withContext(Dispatchers.IO) {
            if (!source.isDirectDownload) {
                return@withContext Result.failure(IllegalStateException("Source requires purchase/download via official webpage."))
            }
            runCatching {
                val request = Request.Builder().url(source.url).build()
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Download failed (HTTP ${response.code})")
                    val body = response.body ?: error("Empty response body")
                    val contentType = response.header("Content-Type") ?: "audio/mpeg"
                    val ext = when {
                        contentType.contains("flac", true) -> "flac"
                        contentType.contains("wav", true) -> "wav"
                        contentType.contains("mp4", true) || contentType.contains("aac", true) -> "m4a"
                        else -> "mp3"
                    }
                    val safeName = "${source.artist}_${source.title}"
                        .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                        .take(60)
                    val outDir = File(
                        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir,
                        "SoundShelfDownloads"
                    ).apply { mkdirs() }
                    val outFile = File(outDir, "${safeName}_${System.currentTimeMillis()}.$ext")

                    val digest = MessageDigest.getInstance("SHA-256")
                    body.byteStream().use { input ->
                        FileOutputStream(outFile).use { output ->
                            val buffer = ByteArray(8192)
                            var bytes = input.read(buffer)
                            while (bytes >= 0) {
                                output.write(buffer, 0, bytes)
                                digest.update(buffer, 0, bytes)
                                bytes = input.read(buffer)
                            }
                        }
                    }

                    val checksum = digest.digest().joinToString("") { "%02x".format(it) }
                    val uri = Uri.fromFile(outFile)
                    val inspected = AudioInspector.inspectUri(context, uri, outFile.absolutePath)
                    val trackId = "dl_local_${UUID.randomUUID()}"
                    val track = MusicTrack(
                        id = trackId,
                        providerId = ProviderId.LOCAL,
                        providerTrackId = trackId,
                        title = inspected.title?.takeIf { it.isNotBlank() } ?: source.title,
                        artist = inspected.artist?.takeIf { it.isNotBlank() } ?: source.artist,
                        album = inspected.album?.takeIf { it.isNotBlank() } ?: source.album.ifBlank { "Downloads" },
                        genre = inspected.genre ?: "Downloaded",
                        year = inspected.year ?: 0,
                        durationMs = inspected.durationMs ?: 0L,
                        localContentUri = uri.toString(),
                        localFilePath = outFile.absolutePath,
                        folderPath = outDir.absolutePath,
                        dateAddedSec = System.currentTimeMillis() / 1000L,
                        fileSizeBytes = outFile.length(),
                        qualityInfo = inspected.qualityInfo,
                        licenseName = source.license,
                        isDownloadPermitted = true
                    )
                    trackDao.upsertTrack(track.toEntity())
                    DownloadedFileVerification(
                        importedTrack = track,
                        sha256Checksum = checksum,
                        localPath = outFile.absolutePath
                    )
                }
            }
        }

    /**
     * "Find a Better Version" safe replacement: preserves playlist memberships, play counts,
     * favorites, and playback history while upgrading the library track reference. Never auto-deletes the old file.
     */
    suspend fun replaceTrackPreservingAssociations(
        oldTrack: MusicTrack,
        upgradedTrack: MusicTrack
    ): MusicTrack = withContext(Dispatchers.IO) {
        val merged = upgradedTrack.copy(
            playCount = oldTrack.playCount,
            lastPlayedMs = oldTrack.lastPlayedMs,
            isFavorite = oldTrack.isFavorite,
            rating = oldTrack.rating
        )
        trackDao.upsertTrack(merged.toEntity())
        playlistDao.replaceTrackReferences(oldTrack.id, merged.id)
        historyDao.replaceTrackReferences(oldTrack.id, merged.id)
        trackDao.markTracksMissing(listOf(oldTrack.id))
        merged
    }

    private fun computeConfidence(
        query: DownloadFinderQuery,
        candidateTitle: String,
        candidateArtist: String,
        candidateIsrc: String?
    ): String {
        if (query.isrc.isNotBlank() && candidateIsrc != null && query.isrc.equals(candidateIsrc, true)) {
            return "ISRC Verified Match"
        }
        val titleMatch = candidateTitle.contains(query.title, ignoreCase = true)
        val artistMatch = query.artist.isBlank() || candidateArtist.contains(query.artist, ignoreCase = true)
        return when {
            titleMatch && artistMatch -> "Strong Title + Artist Match"
            titleMatch -> "Title Match (Verify Artist/Version)"
            else -> "Related Release"
        }
    }
}
