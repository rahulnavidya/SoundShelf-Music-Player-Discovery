package com.example.lyrics

import com.example.core.model.LyricLine
import com.example.core.model.LyricsDocument
import java.util.Locale

object LrcParser {
    private val timestampRegex = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?\\]")
    private val offsetRegex = Regex("\\[offset:\\s*([+-]?\\d+)\\s*\\]", RegexOption.IGNORE_CASE)

    fun parse(trackId: String, rawContent: String, source: String = "Local LRC", fileUri: String? = null): LyricsDocument {
        if (rawContent.isBlank()) {
            return LyricsDocument(trackId = trackId, source = source, lrcFileUri = fileUri)
        }

        var offsetMs = 0L
        val parsedLines = mutableListOf<LyricLine>()
        val plainBuilder = mutableListOf<String>()

        rawContent.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty()) return@forEach

            val offsetMatch = offsetRegex.find(trimmed)
            if (offsetMatch != null) {
                offsetMs = offsetMatch.groupValues[1].toLongOrNull() ?: 0L
                return@forEach
            }

            val matches = timestampRegex.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val lyricText = timestampRegex.replace(trimmed, "").trim()
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val fractionRaw = match.groupValues[3]
                    val millis = when (fractionRaw.length) {
                        0 -> 0L
                        1 -> (fractionRaw.toLongOrNull() ?: 0L) * 100L
                        2 -> (fractionRaw.toLongOrNull() ?: 0L) * 10L
                        else -> fractionRaw.take(3).toLongOrNull() ?: 0L
                    }
                    val totalMs = (minutes * 60_000L) + (seconds * 1_000L) + millis
                    parsedLines.add(LyricLine(timestampMs = totalMs, text = lyricText))
                }
                if (lyricText.isNotEmpty()) plainBuilder.add(lyricText)
            } else if (!trimmed.startsWith("[")) {
                plainBuilder.add(trimmed)
            }
        }

        val sorted = parsedLines.sortedBy { it.timestampMs }
        return LyricsDocument(
            trackId = trackId,
            lines = sorted,
            plainText = plainBuilder.joinToString("\n"),
            source = source,
            offsetMs = offsetMs,
            isSynchronized = sorted.isNotEmpty(),
            lrcFileUri = fileUri
        )
    }

    fun serialize(doc: LyricsDocument): String {
        val sb = StringBuilder()
        if (doc.offsetMs != 0L) {
            sb.appendLine("[offset:${doc.offsetMs}]")
        }
        if (doc.lines.isNotEmpty()) {
            doc.lines.sortedBy { it.timestampMs }.forEach { line ->
                sb.appendLine("${formatTimestamp(line.timestampMs)}${line.text}")
            }
        } else if (doc.plainText.isNotBlank()) {
            sb.append(doc.plainText)
        }
        return sb.toString()
    }

    fun formatTimestamp(ms: Long): String {
        val safe = ms.coerceAtLeast(0L)
        val minutes = safe / 60_000L
        val seconds = (safe % 60_000L) / 1_000L
        val centis = (safe % 1_000L) / 10L
        return String.format(Locale.US, "[%02d:%02d.%02d]", minutes, seconds, centis)
    }

    fun findActiveLineIndex(lines: List<LyricLine>, positionMs: Long, offsetMs: Long): Int {
        if (lines.isEmpty()) return -1
        val adjustedPos = positionMs + offsetMs
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timestampMs <= adjustedPos) {
                active = i
            } else {
                break
            }
        }
        return active
    }
}
