package com.example

import com.example.core.designsystem.SoundShelfPalettes
import com.example.core.designsystem.SoundShelfThemeId
import com.example.core.model.AudioQualityInfo
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.core.model.SmartPlaylistRule
import com.example.core.model.SmartRuleType
import com.example.library.PlaylistEngine
import com.example.lyrics.LrcParser
import com.example.persistence.M3uPlaylistCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundShelfCoreTest {

    @Test
    fun `all 15 curated themes are registered`() {
        assertEquals(15, SoundShelfThemeId.entries.size)
        SoundShelfThemeId.entries.forEach { themeId ->
            val palette = SoundShelfPalettes.get(themeId)
            assertEquals(themeId, palette.id)
        }
    }

    @Test
    fun `lrc parser handles timestamps offset and active line lookup`() {
        val raw = """
            [offset:250]
            [00:05.00]First verse
            [00:12.50]Second verse
            [00:25.00]Chorus line
        """.trimIndent()

        val doc = LrcParser.parse("t1", raw)
        assertTrue(doc.isSynchronized)
        assertEquals(250L, doc.offsetMs)
        assertEquals(3, doc.lines.size)
        assertEquals(0, LrcParser.findActiveLineIndex(doc.lines, 6000L, 0L))
        assertEquals(1, LrcParser.findActiveLineIndex(doc.lines, 12300L, doc.offsetMs))
        assertEquals(2, LrcParser.findActiveLineIndex(doc.lines, 30000L, 0L))
    }

    @Test
    fun `m3u8 export and parse round trip preserves track metadata`() {
        val tracks = listOf(
            MusicTrack(
                id = "local_1",
                title = "Midnight Echo",
                artist = "Studio Ensemble",
                durationMs = 215_000L,
                localFilePath = "/storage/emulated/0/Music/midnight_echo.flac"
            )
        )
        val exported = M3uPlaylistCodec.exportToM3u8("Late Night", tracks)
        val (name, entries) = M3uPlaylistCodec.parseM3u(exported)
        assertEquals("Late Night", name)
        assertEquals(1, entries.size)
        assertEquals("Midnight Echo", entries.first().title)
        assertEquals("Studio Ensemble", entries.first().artist)
        assertEquals("/storage/emulated/0/Music/midnight_echo.flac", entries.first().location)
    }

    @Test
    fun `smart playlist engine evaluates hi-res and duration rules locally`() {
        val library = listOf(
            MusicTrack(
                id = "1",
                title = "Lossless Master",
                artist = "A",
                durationMs = 240_000L,
                year = 2022,
                qualityInfo = AudioQualityInfo(format = "FLAC", isLossless = true, sampleRateHz = 96000, bitDepth = 24)
            ),
            MusicTrack(
                id = "2",
                title = "Short Sketch",
                artist = "B",
                durationMs = 90_000L,
                year = 2015,
                qualityInfo = AudioQualityInfo(format = "MP3", isLossless = false, bitrateKbps = 192)
            )
        )
        val hiRes = PlaylistEngine.evaluateSmartPlaylist(SmartPlaylistRule(rules = listOf(SmartRuleType.HI_RES_AUDIO)), library)
        assertEquals(listOf("1"), hiRes.map { it.id })

        val shortTracks = PlaylistEngine.evaluateSmartPlaylist(SmartPlaylistRule(rules = listOf(SmartRuleType.SHORT_TRACKS)), library)
        assertEquals(listOf("2"), shortTracks.map { it.id })
    }

    @Test
    fun `restricted providers cannot join unified playback queue`() {
        val localTrack = MusicTrack(id = "l1", providerId = ProviderId.LOCAL, title = "Local", artist = "A")
        val audiusTrack = MusicTrack(id = "a1", providerId = ProviderId.AUDIUS, title = "Audius", artist = "B")
        val spotifyTrack = MusicTrack(id = "s1", providerId = ProviderId.SPOTIFY, title = "Spotify", artist = "C")
        val ytTrack = MusicTrack(id = "y1", providerId = ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL, title = "YTM", artist = "D")

        assertTrue(localTrack.canJoinUnifiedQueue)
        assertTrue(audiusTrack.canJoinUnifiedQueue)
        assertFalse(spotifyTrack.canJoinUnifiedQueue)
        assertFalse(ytTrack.canJoinUnifiedQueue)
    }
}
