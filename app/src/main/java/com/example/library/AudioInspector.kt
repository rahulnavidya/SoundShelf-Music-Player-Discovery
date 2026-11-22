package com.example.library

import android.content.Context
import android.media.AudioFormat
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import com.example.core.model.AudioQualityInfo
import com.example.core.model.QualitySourceKind
import java.io.File

data class InspectedAudioDetails(
    val qualityInfo: AudioQualityInfo,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val composer: String? = null,
    val durationMs: Long? = null,
    val fileSizeBytes: Long? = null
)

object AudioInspector {

    fun inspectUri(context: Context, uri: Uri, knownFilePath: String? = null): InspectedAudioDetails {
        val retriever = MediaMetadataRetriever()
        val extractor = MediaExtractor()

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var albumArtist: String? = null
        var genre: String? = null
        var year: Int? = null
        var trackNum: Int? = null
        var discNum: Int? = null
        var composer: String? = null
        var durationMs: Long? = null
        var retrieverBitrateBps: Int? = null
        var retrieverSampleRate: Int? = null
        var retrieverBitsPerSample: Int? = null
        var mimeType: String? = null

        try {
            retriever.setDataSource(context, uri)
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.take(4)?.toIntOrNull()
            trackNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
                ?.substringBefore("/")?.trim()?.toIntOrNull()
            discNum = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
                ?.substringBefore("/")?.trim()?.toIntOrNull()
            composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            retrieverBitrateBps = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()
            mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                retrieverSampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull()
                retrieverBitsPerSample = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE)?.toIntOrNull()
            }
        } catch (_: Exception) {
            // Gracefully handle unreadable containers
        } finally {
            runCatching { retriever.release() }
        }

        var codecMime: String? = mimeType
        var sampleRate: Int? = retrieverSampleRate
        var channelCount: Int? = null
        var bitrateBps: Int? = retrieverBitrateBps
        var bitDepth: Int? = retrieverBitsPerSample

        try {
            extractor.setDataSource(context, uri, null)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val trackMime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (trackMime.startsWith("audio/")) {
                    codecMime = trackMime
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (bitrateBps == null && format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrateBps = format.getInteger(MediaFormat.KEY_BIT_RATE)
                    }
                    if (bitDepth == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
                        format.containsKey(MediaFormat.KEY_PCM_ENCODING)
                    ) {
                        bitDepth = when (format.getInteger(MediaFormat.KEY_PCM_ENCODING)) {
                            AudioFormat.ENCODING_PCM_8BIT -> 8
                            AudioFormat.ENCODING_PCM_16BIT -> 16
                            AudioFormat.ENCODING_PCM_24BIT_PACKED -> 24
                            AudioFormat.ENCODING_PCM_32BIT -> 32
                            else -> null
                        }
                    }
                    break
                }
            }
        } catch (_: Exception) {
            // Keep retriever fallback values
        } finally {
            runCatching { extractor.release() }
        }

        val fileSize = resolveFileSize(context, uri, knownFilePath)
        val ext = knownFilePath?.substringAfterLast('.', "")?.lowercase()
            ?: context.contentResolver.getType(uri)?.substringAfterLast('/')?.lowercase()
            ?: ""

        val detectedFormat = inferContainerFormat(ext, codecMime)
        val detectedCodec = inferCodecLabel(codecMime, detectedFormat)
        val isLossless = detectedFormat in setOf("FLAC", "WAV", "ALAC", "AIFF") ||
            codecMime?.contains("flac", ignoreCase = true) == true ||
            codecMime?.contains("alac", ignoreCase = true) == true ||
            codecMime?.contains("raw", ignoreCase = true) == true

        val computedBitrateKbps = bitrateBps?.let { it / 1000 } ?: run {
            if (fileSize != null && fileSize > 0 && durationMs != null && durationMs > 0) {
                ((fileSize * 8L) / durationMs).toInt()
            } else null
        }

        return InspectedAudioDetails(
            qualityInfo = AudioQualityInfo(
                format = detectedFormat,
                codec = detectedCodec,
                bitrateKbps = computedBitrateKbps,
                bitrateMode = if (isLossless) "Lossless / VBR" else null,
                sampleRateHz = sampleRate,
                bitDepth = bitDepth,
                channelCount = channelCount,
                fileSizeBytes = fileSize,
                encoder = null,
                sourceKind = QualitySourceKind.ACTUAL_FILE,
                isLossless = isLossless
            ),
            title = title,
            artist = artist,
            album = album,
            albumArtist = albumArtist,
            genre = genre,
            year = year,
            trackNumber = trackNum,
            discNumber = discNum,
            composer = composer,
            durationMs = durationMs,
            fileSizeBytes = fileSize
        )
    }

    private fun resolveFileSize(context: Context, uri: Uri, knownFilePath: String?): Long? {
        if (!knownFilePath.isNullOrBlank()) {
            val f = File(knownFilePath)
            if (f.exists()) return f.length()
        }
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val idx = c.getColumnIndex(OpenableColumns.SIZE)
                    if (idx >= 0 && !c.isNull(idx)) c.getLong(idx) else null
                } else null
            }
        }.getOrNull()
    }

    private fun inferContainerFormat(ext: String, mime: String?): String {
        return when {
            ext == "flac" || mime?.contains("flac", true) == true -> "FLAC"
            ext == "mp3" || mime == "audio/mpeg" -> "MP3"
            ext == "m4a" || ext == "mp4" || mime == "audio/mp4" -> if (mime?.contains("alac", true) == true) "ALAC" else "M4A"
            ext == "aac" || mime?.contains("aac", true) == true -> "AAC"
            ext == "opus" || mime?.contains("opus", true) == true -> "Opus"
            ext == "ogg" || ext == "oga" || mime?.contains("vorbis", true) == true -> "OGG"
            ext == "wav" || mime?.contains("wav", true) == true || mime?.contains("raw", true) == true -> "WAV"
            ext == "alac" || mime?.contains("alac", true) == true -> "ALAC"
            ext.isNotBlank() -> ext.uppercase()
            else -> "Unknown"
        }
    }

    private fun inferCodecLabel(mime: String?, format: String): String {
        return when {
            mime == null -> format
            mime.contains("flac", true) -> "FLAC"
            mime.contains("mpeg", true) -> "MPEG-1/2 Layer III"
            mime.contains("mp4a-latm", true) || mime.contains("aac", true) -> "AAC-LC"
            mime.contains("opus", true) -> "Opus"
            mime.contains("vorbis", true) -> "Vorbis"
            mime.contains("alac", true) -> "Apple Lossless (ALAC)"
            mime.contains("raw", true) -> "Linear PCM"
            mime.contains("mp4", true) -> "AAC / MP4 Audio"
            else -> mime
        }
    }
}
