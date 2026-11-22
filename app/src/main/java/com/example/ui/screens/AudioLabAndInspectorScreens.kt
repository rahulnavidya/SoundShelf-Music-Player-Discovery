package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.MusicTrack
import com.example.persistence.AudioEngineSettings
import com.example.playback.AudioEffectCapabilities
import com.example.ui.components.ProviderBadge
import com.example.ui.components.ThemedCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDuration
import kotlin.math.roundToInt

@Composable
fun AudioInfoScreen(
    track: MusicTrack,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val q = track.qualityInfo
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("audio_info_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Audio Quality Inspector", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ThemedCard {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        TrackArtwork(artworkUri = track.artworkUri, contentDescription = track.title, size = 64.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(track.title, style = MaterialTheme.typography.titleLarge)
                            Text("${track.artist} · ${track.album}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProviderBadge(track.providerId)
                                Text(
                                    text = "Source: ${q.sourceKind.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Technical Audio Properties", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InfoRow("Measurement Basis", q.sourceKind.label)
                        InfoRow("Container / Format", q.format)
                        InfoRow("Codec", q.codec)
                        InfoRow("Bitrate", q.bitrateKbps?.let { "$it kbps" } ?: "Not reported")
                        InfoRow("Bitrate Mode", q.bitrateMode ?: "Not detectable")
                        InfoRow("Sample Rate", q.sampleRateHz?.let { "$it Hz" } ?: "Not reported")
                        InfoRow("Bit Depth", q.bitDepth?.let { "$it-bit" } ?: "Not detectable")
                        InfoRow("Channels", q.channelCount?.let { if (it == 2) "2 (Stereo)" else if (it == 1) "1 (Mono)" else "$it ch" } ?: "Not reported")
                        InfoRow("Duration", formatDuration(track.durationMs))
                        InfoRow("File Size", if (track.fileSizeBytes > 0) formatFileSize(track.fileSizeBytes) else "N/A (Stream)")
                        InfoRow("Lossless Status", if (q.isLossless) "Lossless Container" else "Lossy / Stream")
                    }
                }
            }

            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Metadata Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        InfoRow("Title", track.title)
                        InfoRow("Artist", track.artist)
                        InfoRow("Album", track.album)
                        InfoRow("Album Artist", track.albumArtist)
                        InfoRow("Genre", track.genre)
                        InfoRow("Track / Disc", "${if (track.trackNumber > 0) track.trackNumber else "-"} / Disc ${track.discNumber}")
                        InfoRow("Release Year", if (track.year > 0) track.year.toString() else "Not tagged")
                        track.composer?.let { InfoRow("Composer", it) }
                        track.copyright?.let { InfoRow("Copyright", it) }
                        track.licenseName?.let { InfoRow("Licence", it) }
                        track.localFilePath?.let { InfoRow("Local Path", it) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    }
}

@Composable
fun AudioLabScreen(
    settings: AudioEngineSettings,
    capabilities: AudioEffectCapabilities,
    onBack: () -> Unit,
    onUpdateSettings: ((AudioEngineSettings) -> AudioEngineSettings) -> Unit,
    onApplyEqPreset: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("audio_lab_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Audio Lab & Equalizer", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Playback Timing & Dynamics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Speed: ${String.format("%.2fx", settings.playbackSpeed)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.playbackSpeed,
                            onValueChange = { spd -> onUpdateSettings { it.copy(playbackSpeed = (spd * 20).roundToInt() / 20f) } },
                            valueRange = 0.5f..2.0f
                        )
                        Text("Pitch: ${String.format("%.2fx", settings.playbackPitch)}", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.playbackPitch,
                            onValueChange = { p -> onUpdateSettings { it.copy(playbackPitch = (p * 20).roundToInt() / 20f) } },
                            valueRange = 0.7f..1.3f
                        )
                        Text("Crossfade: ${settings.crossfadeSeconds}s", style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = settings.crossfadeSeconds.toFloat(),
                            onValueChange = { cf -> onUpdateSettings { it.copy(crossfadeSeconds = cf.roundToInt()) } },
                            valueRange = 0f..12f
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Gapless Playback")
                            Switch(checked = settings.gaplessPlayback, onCheckedChange = { g -> onUpdateSettings { it.copy(gaplessPlayback = g) } })
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("ReplayGain / Normalisation")
                            Switch(checked = settings.volumeNormalization, onCheckedChange = { n -> onUpdateSettings { it.copy(volumeNormalization = n, replayGainEnabled = n) } })
                        }
                    }
                }
            }

            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Hardware Equalizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    if (capabilities.equalizerSupported) "${capabilities.bandCount} device bands active (Session #${capabilities.audioSessionId})"
                                    else "Start playback to attach to Android AudioSession",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.equalizerEnabled,
                                enabled = capabilities.equalizerSupported,
                                onCheckedChange = { eq -> onUpdateSettings { it.copy(equalizerEnabled = eq) } }
                            )
                        }

                        if (capabilities.presets.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(capabilities.presets) { idx, presetName ->
                                    AssistChip(onClick = { onApplyEqPreset(idx) }, label = { Text(presetName) })
                                }
                            }
                        }

                        if (capabilities.equalizerSupported && capabilities.bands.isNotEmpty()) {
                            capabilities.bands.forEach { band ->
                                val levelMb = settings.equalizerBandsMb.getOrElse(band.index) { band.currentLevelMb }
                                Text(
                                    "${band.centerFreqHz} Hz : ${levelMb / 100f} dB",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Slider(
                                    value = levelMb.toFloat(),
                                    enabled = settings.equalizerEnabled,
                                    onValueChange = { v ->
                                        onUpdateSettings { prev ->
                                            val updated = prev.equalizerBandsMb.toMutableList()
                                            while (updated.size <= band.index) updated.add(0)
                                            updated[band.index] = v.roundToInt()
                                            prev.copy(equalizerBandsMb = updated)
                                        }
                                    },
                                    valueRange = capabilities.minBandLevelMb.toFloat()..capabilities.maxBandLevelMb.toFloat()
                                )
                            }
                        }
                    }
                }
            }

            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Bass & Loudness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (capabilities.bassBoostSupported) "Bass Boost: ${settings.bassBoostStrength / 10}%" else "Bass Boost not supported by this output device",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = settings.bassBoostStrength.toFloat(),
                            enabled = capabilities.bassBoostSupported,
                            onValueChange = { b -> onUpdateSettings { it.copy(bassBoostStrength = b.roundToInt()) } },
                            valueRange = 0f..1000f
                        )
                        Text(
                            if (capabilities.loudnessEnhancerSupported) "Loudness Gain: +${settings.loudnessGainMb / 100f} dB" else "Loudness Enhancer awaiting audio session",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = settings.loudnessGainMb.toFloat(),
                            enabled = capabilities.loudnessEnhancerSupported,
                            onValueChange = { l -> onUpdateSettings { it.copy(loudnessGainMb = l.roundToInt()) } },
                            valueRange = 0f..1200f
                        )
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format("%.2f MB", mb) else "${bytes / 1024L} KB"
}
