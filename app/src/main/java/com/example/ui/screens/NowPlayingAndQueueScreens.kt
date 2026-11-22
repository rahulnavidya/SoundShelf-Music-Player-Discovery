package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.LocalSoundShelfTokens
import com.example.core.designsystem.NowPlayingLayoutMode
import com.example.core.model.LyricsDocument
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.core.model.RepeatMode
import com.example.lyrics.LrcParser
import com.example.playback.PlaybackUiState
import com.example.ui.components.ProviderBadge
import com.example.ui.components.ThemedCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDuration
import com.example.ui.components.rememberExtractedArtworkBrush

@Composable
fun NowPlayingScreen(
    playbackState: PlaybackUiState,
    layoutMode: NowPlayingLayoutMode,
    lyricsDoc: LyricsDocument?,
    hapticsEnabled: Boolean,
    onBack: () -> Unit,
    onSelectLayoutMode: (NowPlayingLayoutMode) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: (MusicTrack) -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    onOpenAudioLab: () -> Unit,
    onOpenAudioInfo: (MusicTrack) -> Unit,
    onOpenMetadataEditor: (MusicTrack) -> Unit,
    onOpenDownloadFinder: (MusicTrack) -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onLoadLyrics: (MusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSoundShelfTokens.current
    val haptic = LocalHapticFeedback.current
    val track = playbackState.currentTrack

    LaunchedEffect(track?.id) {
        track?.let(onLoadLyrics)
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var showLayoutPicker by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    val backgroundBrush = when (layoutMode) {
        NowPlayingLayoutMode.AMOLED -> Brush.verticalGradient(listOf(Color.Black, Color.Black))
        NowPlayingLayoutMode.ARTWORK, NowPlayingLayoutMode.CLASSIC -> rememberExtractedArtworkBrush(
            artworkUri = track?.artworkUri,
            fallbackPrimary = tokens.palette.primary,
            fallbackEnd = tokens.palette.background
        )
        else -> Brush.verticalGradient(listOf(tokens.palette.background, tokens.palette.backgroundGradientEnd))
    }

    val textColor = if (layoutMode == NowPlayingLayoutMode.AMOLED) Color.White else MaterialTheme.colorScheme.onBackground
    val subTextColor = if (layoutMode == NowPlayingLayoutMode.AMOLED) Color(0xFFB0B8C5) else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("now_playing_screen")
    ) {
        if (track == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Nothing Playing", style = MaterialTheme.typography.headlineMedium, color = textColor)
                Spacer(Modifier.height(8.dp))
                Text("Pick any local track or Audius stream to start listening.", style = MaterialTheme.typography.bodyMedium, color = subTextColor, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Return to Shelf") }
            }
            return@Box
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close Now Playing", tint = textColor)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { showLayoutPicker = true }
                ) {
                    Text(
                        text = layoutMode.label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = track.album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Player options", tint = textColor)
                    }
                    DropdownMenu(expanded = showMoreMenu, onDismissRequest = { showMoreMenu = false }) {
                        DropdownMenuItem(text = { Text("Switch Player Layout") }, onClick = { showMoreMenu = false; showLayoutPicker = true })
                        DropdownMenuItem(text = { Text("Audio Quality Info") }, onClick = { showMoreMenu = false; onOpenAudioInfo(track) })
                        if (track.providerId == ProviderId.LOCAL) {
                            DropdownMenuItem(text = { Text("Edit Tags & Artwork") }, onClick = { showMoreMenu = false; onOpenMetadataEditor(track) })
                        }
                        DropdownMenuItem(text = { Text("Find Legitimate / Better Copy") }, onClick = { showMoreMenu = false; onOpenDownloadFinder(track) })
                        DropdownMenuItem(text = { Text("Sleep Timer") }, onClick = { showMoreMenu = false; showSleepTimerDialog = true })
                    }
                }
            }

            // Middle Showcase (adapts cleanly to the 5 player visual modes)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val artSize = (minOf(maxWidth, maxHeight) * if (layoutMode == NowPlayingLayoutMode.ARTWORK) 0.92f else 0.78f)
                    .coerceIn(180.dp, 360.dp)

                if (layoutMode == NowPlayingLayoutMode.LYRICS) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TrackArtwork(artworkUri = track.artworkUri, contentDescription = track.title, size = 56.dp, isSpinning = playbackState.isPlaying)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.titleLarge, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artist, style = MaterialTheme.typography.bodyMedium, color = subTextColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        val lines = lyricsDoc?.lines.orEmpty()
                        val activeIdx = LrcParser.findActiveLineIndex(lines, playbackState.positionMs, lyricsDoc?.offsetMs ?: 0L)
                        if (lines.isNotEmpty()) {
                            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                itemsIndexed(lines) { idx, line ->
                                    val isActive = idx == activeIdx
                                    Text(
                                        text = line.text.ifBlank { "♪" },
                                        style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                                        color = if (isActive) MaterialTheme.colorScheme.primary else subTextColor.copy(alpha = 0.65f),
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.clickable { onSeekTo(line.timestampMs) }
                                    )
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().clickable { onOpenLyrics() }, contentAlignment = Alignment.Center) {
                                Text(
                                    lyricsDoc?.plainText?.takeIf { it.isNotBlank() } ?: "No synced lyrics loaded.\nTap to open Lyrics Studio & import .lrc",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = subTextColor,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    TrackArtwork(
                        artworkUri = track.artworkUri,
                        contentDescription = "${track.title} artwork",
                        size = artSize,
                        isSpinning = playbackState.isPlaying
                    )
                }
            }

            // Track Metadata & Quick Badges
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodyLarge,
                            color = subTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = {
                            if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleFavorite(track)
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (track.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favourite",
                            tint = if (track.isFavorite) MaterialTheme.colorScheme.secondary else textColor
                        )
                    }
                }

                if (layoutMode != NowPlayingLayoutMode.MINIMAL) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProviderBadge(track.providerId)
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.clickable { onOpenAudioInfo(track) }
                        ) {
                            Text(
                                text = track.qualityInfo.summaryBadge(),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (playbackState.playbackSpeed != 1.0f) {
                            Text(
                                text = "${playbackState.playbackSpeed}x",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        playbackState.sleepTimerRemainingMs?.let { rem ->
                            Text(
                                text = "Sleep ${formatDuration(rem)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Seek bar
            var isSeeking by remember { mutableStateOf(false) }
            var sliderFraction by remember { mutableFloatStateOf(0f) }
            val duration = playbackState.durationMs.coerceAtLeast(1L)
            val currentFraction = if (isSeeking) sliderFraction else (playbackState.positionMs.toFloat() / duration.toFloat()).coerceIn(0f, 1f)

            Column {
                Slider(
                    value = currentFraction,
                    onValueChange = {
                        isSeeking = true
                        sliderFraction = it
                    },
                    onValueChangeFinished = {
                        onSeekTo((sliderFraction * duration).toLong())
                        isSeeking = false
                    },
                    modifier = Modifier.fillMaxWidth().testTag("now_playing_seekbar")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatDuration(if (isSeeking) (sliderFraction * duration).toLong() else playbackState.positionMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = subTextColor
                    )
                    Text(
                        text = "-${formatDuration((duration - playbackState.positionMs).coerceAtLeast(0L))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = subTextColor
                    )
                }
            }

            // Primary Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = if (playbackState.shuffleEnabled) MaterialTheme.colorScheme.primary else subTextColor
                    )
                }
                IconButton(onClick = onPrevious, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = textColor, modifier = Modifier.size(34.dp))
                }
                FilledIconButton(
                    onClick = {
                        if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onTogglePlayPause()
                    },
                    modifier = Modifier.size(68.dp).testTag("now_playing_play_pause"),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = textColor, modifier = Modifier.size(34.dp))
                }
                IconButton(onClick = onCycleRepeat, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(
                        imageVector = if (playbackState.repeatMode == RepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat mode",
                        tint = if (playbackState.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else subTextColor
                    )
                }
            }

            // Secondary Action Dock
            if (layoutMode != NowPlayingLayoutMode.MINIMAL) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    IconButton(onClick = onOpenLyrics, modifier = Modifier.minimumInteractiveComponentSize().testTag("np_lyrics_button")) {
                        Icon(Icons.Default.Lyrics, contentDescription = "Lyrics Studio", tint = textColor)
                    }
                    IconButton(onClick = onOpenAudioLab, modifier = Modifier.minimumInteractiveComponentSize().testTag("np_audiolab_button")) {
                        Icon(Icons.Default.Equalizer, contentDescription = "Audio Lab", tint = textColor)
                    }
                    IconButton(onClick = onOpenQueue, modifier = Modifier.minimumInteractiveComponentSize().testTag("np_queue_button")) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "Queue", tint = textColor)
                    }
                    IconButton(onClick = { onOpenAudioInfo(track) }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.Info, contentDescription = "Audio Info", tint = textColor)
                    }
                }
            }
        }
    }

    if (showLayoutPicker) {
        AlertDialog(
            onDismissRequest = { showLayoutPicker = false },
            title = { Text("Now Playing Layout") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NowPlayingLayoutMode.entries.forEach { mode ->
                        ThemedCard(onClick = {
                            onSelectLayoutMode(mode)
                            showLayoutPicker = false
                        }) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(
                                    if (mode == layoutMode) "✓ ${mode.label}" else mode.label,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (mode == layoutMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(mode.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLayoutPicker = false }) { Text("Close") } }
        )
    }

    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("Sleep Timer") },
            text = {
                Column {
                    listOf(15, 30, 45, 60).forEach { mins ->
                        TextButton(onClick = { onSetSleepTimer(mins); showSleepTimerDialog = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("$mins minutes")
                        }
                    }
                    TextButton(onClick = { onSetSleepTimer(null); showSleepTimerDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Turn Off Timer")
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
fun QueueScreen(
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onJumpToIndex: (Int) -> Unit,
    onMoveItem: (Int, Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onClearQueue: () -> Unit,
    onSaveQueueAsPlaylist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("queue_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text("Playback Queue", style = MaterialTheme.typography.titleLarge)
                Text("${playbackState.queue.size} tracks", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { showSaveDialog = true }, enabled = playbackState.queue.isNotEmpty()) {
                Icon(Icons.Default.Save, contentDescription = "Save queue as playlist")
            }
            IconButton(onClick = onClearQueue, enabled = playbackState.queue.isNotEmpty()) {
                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear queue")
            }
        }

        if (playbackState.queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Your playback queue is empty.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(playbackState.queue, key = { _, e -> e.entryId }) { idx, entry ->
                    val isCurrent = idx == playbackState.currentIndex
                    val statusTag = when {
                        idx < playbackState.currentIndex -> "Played"
                        isCurrent -> "Now Playing"
                        else -> "Up Next"
                    }
                    ThemedCard(onClick = { onJumpToIndex(idx) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TrackArtwork(artworkUri = entry.track.artworkUri, contentDescription = entry.track.title, size = 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.track.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(statusTag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(entry.track.artist, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(enabled = idx > 0, onClick = { onMoveItem(idx, idx - 1) }) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Move up")
                            }
                            IconButton(enabled = idx < playbackState.queue.lastIndex, onClick = { onMoveItem(idx, idx + 1) }) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Move down")
                            }
                            IconButton(onClick = { onRemoveItem(idx) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("Queue Mix") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Queue as Playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = { onSaveQueueAsPlaylist(name); showSaveDialog = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }
}
