package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.model.LyricsDocument
import com.example.core.model.MusicTrack
import com.example.library.MetadataEditRequest
import com.example.lyrics.LrcParser
import com.example.ui.components.ThemedCard
import com.example.ui.components.TrackArtwork

@Composable
fun LyricsScreen(
    track: MusicTrack,
    lyricsDoc: LyricsDocument?,
    positionMs: Long,
    onBack: () -> Unit,
    onImportLrc: (Uri) -> Unit,
    onAdjustOffset: (Long) -> Unit,
    onStampLineAtPlayhead: (Int, Long) -> Unit,
    onSaveRawLyrics: (String) -> Unit,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val lrcPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportLrc)
    }

    var isEditMode by remember { mutableStateOf(false) }
    var autoScroll by remember { mutableStateOf(true) }
    var rawDraft by remember(lyricsDoc) {
        mutableStateOf(lyricsDoc?.let { LrcParser.serialize(it) } ?: "")
    }
    val listState = rememberLazyListState()
    val lines = lyricsDoc?.lines.orEmpty()
    val activeIdx = LrcParser.findActiveLineIndex(lines, positionMs, lyricsDoc?.offsetMs ?: 0L)

    LaunchedEffect(activeIdx, autoScroll) {
        if (autoScroll && activeIdx >= 0 && !isEditMode) {
            listState.animateScrollToItem((activeIdx - 2).coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("lyrics_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text("Lyrics Studio", style = MaterialTheme.typography.titleLarge)
                Text("${track.title} · ${lyricsDoc?.source ?: "None"}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { lrcPicker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Default.FileOpen, contentDescription = "Import .lrc file")
            }
            IconButton(onClick = { isEditMode = !isEditMode }) {
                Icon(Icons.Default.Edit, contentDescription = "Edit lyrics & timing")
            }
        }

        // Offset & Sync bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = { onAdjustOffset(-100L) }) { Text("-100ms") }
            Text(
                text = "Offset ${lyricsDoc?.offsetMs ?: 0L}ms",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(onClick = { onAdjustOffset(100L) }) { Text("+100ms") }
            TextButton(onClick = { autoScroll = !autoScroll }) {
                Text(if (autoScroll) "Auto-Sync On" else "Sync")
            }
        }

        if (isEditMode) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Edit LRC or Plain Text Lyrics", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = rawDraft,
                    onValueChange = { rawDraft = it },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    placeholder = { Text("[00:12.50]Paste or type lyrics lines...") }
                )
                Button(
                    onClick = {
                        onSaveRawLyrics(rawDraft)
                        isEditMode = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save Lyrics to Track")
                }
            }
        } else if (lines.isNotEmpty()) {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(lines) { idx, line ->
                    val isActive = idx == activeIdx
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSeekTo(line.timestampMs) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = line.text.ifBlank { "♪" },
                                style = if (isActive) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            )
                            line.translation?.let { tr ->
                                Text(tr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                        IconButton(onClick = { onStampLineAtPlayhead(idx, positionMs) }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Stamp timestamp at playhead")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    lyricsDoc?.plainText?.takeIf { it.isNotBlank() } ?: "No lyrics found for this track.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { lrcPicker.launch(arrayOf("*/*")) }) { Text("Import .lrc File") }
                    FilledTonalButton(onClick = { isEditMode = true }) { Text("Write Lyrics") }
                }
            }
        }
    }
}

@Composable
fun MetadataEditorScreen(
    track: MusicTrack,
    onBack: () -> Unit,
    onSave: (MetadataEditRequest) -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(track.id) { mutableStateOf(track.title) }
    var artist by remember(track.id) { mutableStateOf(track.artist) }
    var album by remember(track.id) { mutableStateOf(track.album) }
    var albumArtist by remember(track.id) { mutableStateOf(track.albumArtist) }
    var genre by remember(track.id) { mutableStateOf(track.genre) }
    var year by remember(track.id) { mutableStateOf(if (track.year > 0) track.year.toString() else "") }
    var trackNum by remember(track.id) { mutableStateOf(if (track.trackNumber > 0) track.trackNumber.toString() else "") }
    var discNum by remember(track.id) { mutableStateOf(track.discNumber.toString()) }
    var composer by remember(track.id) { mutableStateOf(track.composer ?: "") }
    var copyright by remember(track.id) { mutableStateOf(track.copyright ?: "") }
    var comment by remember(track.id) { mutableStateOf(track.comment ?: "") }
    var pickedArtUri by remember(track.id) { mutableStateOf<Uri?>(null) }

    // Compliant Android Photo Picker (zero broad storage permissions)
    val artPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) pickedArtUri = uri
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("metadata_editor_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Local Metadata Editor", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            IconButton(onClick = onUndo) { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last edit") }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ThemedCard {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TrackArtwork(
                            artworkUri = pickedArtUri?.toString() ?: track.artworkUri,
                            contentDescription = "Track artwork",
                            size = 72.dp
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Album Artwork", style = MaterialTheme.typography.titleMedium)
                            Text("Audio stream is preserved lossless — never re-encoded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(onClick = {
                                artPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Replace Cover")
                            }
                        }
                    }
                }
            }
            item { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = albumArtist, onValueChange = { albumArtist = it }, label = { Text("Album Artist") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = trackNum, onValueChange = { trackNum = it }, label = { Text("Track #") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = discNum, onValueChange = { discNum = it }, label = { Text("Disc #") }, modifier = Modifier.weight(1f))
                }
            }
            item { OutlinedTextField(value = composer, onValueChange = { composer = it }, label = { Text("Composer") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = copyright, onValueChange = { copyright = it }, label = { Text("Copyright") }, modifier = Modifier.fillMaxWidth()) }
            item { OutlinedTextField(value = comment, onValueChange = { comment = it }, label = { Text("Comment") }, modifier = Modifier.fillMaxWidth()) }
            item {
                Button(
                    onClick = {
                        onSave(
                            MetadataEditRequest(
                                title = title,
                                artist = artist,
                                album = album,
                                albumArtist = albumArtist,
                                genre = genre,
                                year = year.toIntOrNull() ?: 0,
                                trackNumber = trackNum.toIntOrNull() ?: 0,
                                discNumber = discNum.toIntOrNull() ?: 1,
                                composer = composer,
                                copyright = copyright,
                                comment = comment,
                                newArtworkUri = pickedArtUri
                            )
                        )
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Save Metadata")
                }
            }
        }
    }
}
