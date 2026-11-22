package com.example.ui.screens

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.example.R
import com.example.core.model.LibraryCategory
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.SmartPlaylistRule
import com.example.core.model.SmartRuleType
import com.example.core.model.TrackSortOption
import com.example.library.LibraryScanReport
import com.example.persistence.ExcludedFolderEntity
import com.example.ui.components.ThemedCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackRow

@Composable
fun LibraryScreen(
    localTracks: List<MusicTrack>,
    sortedAllTracks: List<MusicTrack>,
    playlists: List<MusicPlaylist>,
    excludedFolders: List<ExcludedFolderEntity>,
    selectedCategory: LibraryCategory,
    sortOption: TrackSortOption,
    isScanning: Boolean,
    scanReport: LibraryScanReport?,
    currentTrackId: String?,
    hapticsEnabled: Boolean,
    onSelectCategory: (LibraryCategory) -> Unit,
    onSelectSort: (TrackSortOption) -> Unit,
    onScanLibrary: (Boolean) -> Unit,
    onImportSafAudio: (Uri) -> Unit,
    onImportM3u: (Uri) -> Unit,
    onExportPlaylistM3u8: (MusicPlaylist, Uri) -> Unit,
    onExcludeFolder: (String) -> Unit,
    onRemoveExcludedFolder: (String) -> Unit,
    onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAddToQueue: (MusicTrack) -> Unit,
    onToggleFavorite: (MusicTrack) -> Unit,
    onCreatePlaylist: (String, SmartPlaylistRule?) -> Unit,
    onAddTrackToPlaylist: (MusicPlaylist, MusicTrack) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onDuplicatePlaylist: (MusicPlaylist) -> Unit,
    onResolvePlaylistTracks: (MusicPlaylist) -> List<MusicTrack>,
    onShowAudioInfo: (MusicTrack) -> Unit,
    onEditMetadata: (MusicTrack) -> Unit,
    onFindDownload: (MusicTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED)
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) onScanLibrary(true)
    }
    val audioFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportSafAudio)
    }
    val m3uImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportM3u)
    }
    var pendingExportPlaylist by remember { mutableStateOf<MusicPlaylist?>(null) }
    val m3uExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        val pl = pendingExportPlaylist
        if (uri != null && pl != null) {
            onExportPlaylistM3u8(pl, uri)
        }
        pendingExportPlaylist = null
    }

    var showSortMenu by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showExcludedDialog by remember { mutableStateOf(false) }
    var trackForPlaylistPicker by remember { mutableStateOf<MusicTrack?>(null) }
    var drillDownGroup by remember { mutableStateOf<Pair<String, List<MusicTrack>>?>(null) }

    Column(modifier = modifier.fillMaxSize().testTag("library_screen")) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Library", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${localTracks.size} local tracks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = { audioFilePicker.launch(arrayOf("audio/*")) }, modifier = Modifier.testTag("import_audio_button")) {
                    Icon(Icons.Default.AudioFile, contentDescription = "Import audio via SAF")
                }
                IconButton(onClick = { showExcludedDialog = true }) {
                    Icon(Icons.Default.FolderOff, contentDescription = "Excluded folders")
                }
                Box {
                    IconButton(onClick = { showSortMenu = true }, modifier = Modifier.testTag("library_sort_button")) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort library")
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        TrackSortOption.entries.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(if (opt == sortOption) "✓ ${opt.label}" else opt.label) },
                                onClick = { showSortMenu = false; onSelectSort(opt) }
                            )
                        }
                    }
                }
                IconButton(onClick = { onScanLibrary(true) }, modifier = Modifier.testTag("library_rescan_button")) {
                    Icon(Icons.Default.Refresh, contentDescription = "Rescan local library")
                }
            }
        }

        if (isScanning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        scanReport?.let { report ->
            if (report.duplicatesDetected > 0 || report.missingMarked > 0) {
                Text(
                    text = "Index: ${report.totalTracks} active · ${report.duplicatesDetected} duplicate groups · ${report.missingMarked} missing",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
        }

        // Category filter row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(LibraryCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = {
                        drillDownGroup = null
                        onSelectCategory(category)
                    },
                    label = { Text(category.label) }
                )
            }
        }

        if (drillDownGroup != null) {
            val (groupTitle, groupTracks) = drillDownGroup!!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(groupTitle, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                TextButton(onClick = { drillDownGroup = null }) { Text("Back") }
            }
            LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                itemsIndexed(groupTracks, key = { _, t -> t.id }) { idx, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        hapticsEnabled = hapticsEnabled,
                        onClick = { onPlayTracks(groupTracks, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { trackForPlaylistPicker = track },
                        onShowAudioInfo = { onShowAudioInfo(track) },
                        onEditMetadata = { onEditMetadata(track) },
                        onFindDownload = { onFindDownload(track) }
                    )
                }
            }
        } else if (localTracks.isEmpty() && selectedCategory != LibraryCategory.PLAYLISTS) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_library),
                        contentDescription = "Empty local library",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                    Text(
                        text = if (hasPermission) "No Local Audio Files Found Yet" else "Access Your Device Music",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = "SoundShelf reads audio files locally using MediaStore and Android Storage Access Framework — completely offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (!hasPermission) {
                            Button(onClick = { permLauncher.launch(audioPermission) }, modifier = Modifier.testTag("grant_audio_permission_button")) {
                                Text("Scan Device Audio")
                            }
                        } else {
                            Button(onClick = { onScanLibrary(true) }) {
                                Text("Rescan MediaStore")
                            }
                        }
                        OutlinedButton(onClick = { audioFilePicker.launch(arrayOf("audio/*")) }) {
                            Text("Pick Audio File")
                        }
                    }
                }
            }
        } else {
            when (selectedCategory) {
                LibraryCategory.SONGS,
                LibraryCategory.RECENTLY_ADDED,
                LibraryCategory.RECENTLY_PLAYED,
                LibraryCategory.MOST_PLAYED,
                LibraryCategory.FAVORITES -> {
                    val displayed = remember(selectedCategory, sortedAllTracks, localTracks) {
                        when (selectedCategory) {
                            LibraryCategory.RECENTLY_ADDED -> localTracks.sortedByDescending { it.dateAddedSec }
                            LibraryCategory.RECENTLY_PLAYED -> localTracks.filter { it.lastPlayedMs > 0 }.sortedByDescending { it.lastPlayedMs }
                            LibraryCategory.MOST_PLAYED -> localTracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
                            LibraryCategory.FAVORITES -> localTracks.filter { it.isFavorite }
                            else -> sortedAllTracks
                        }
                    }
                    LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { if (displayed.isNotEmpty()) onPlayTracks(displayed, 0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Play All")
                                }
                                FilledTonalButton(
                                    onClick = { if (displayed.isNotEmpty()) onPlayTracks(displayed.shuffled(), 0) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Shuffle, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Shuffle")
                                }
                            }
                        }
                        itemsIndexed(displayed, key = { _, t -> t.id }) { idx, track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                hapticsEnabled = hapticsEnabled,
                                onClick = { onPlayTracks(displayed, idx) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = { trackForPlaylistPicker = track },
                                onShowAudioInfo = { onShowAudioInfo(track) },
                                onEditMetadata = { onEditMetadata(track) },
                                onFindDownload = { onFindDownload(track) }
                            )
                        }
                    }
                }

                LibraryCategory.ALBUMS -> {
                    val albums = remember(localTracks) { localTracks.groupBy { it.album } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(albums.entries.toList(), key = { it.key }) { (albumName, tracks) ->
                            GroupRowCard(
                                title = albumName,
                                subtitle = "${tracks.first().albumArtist} · ${tracks.size} tracks",
                                artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                                fallbackIcon = Icons.Default.Album,
                                onClick = { drillDownGroup = albumName to tracks.sortedBy { it.trackNumber } }
                            )
                        }
                    }
                }

                LibraryCategory.ARTISTS, LibraryCategory.ALBUM_ARTISTS -> {
                    val artists = remember(localTracks, selectedCategory) {
                        if (selectedCategory == LibraryCategory.ALBUM_ARTISTS) {
                            localTracks.groupBy { it.albumArtist }
                        } else {
                            localTracks.groupBy { it.artist }
                        }
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(artists.entries.toList(), key = { it.key }) { (artistName, tracks) ->
                            GroupRowCard(
                                title = artistName,
                                subtitle = "${tracks.size} songs · ${tracks.map { it.album }.distinct().size} albums",
                                artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                                fallbackIcon = Icons.Default.Person,
                                onClick = { drillDownGroup = artistName to tracks }
                            )
                        }
                    }
                }

                LibraryCategory.GENRES -> {
                    val genres = remember(localTracks) { localTracks.groupBy { it.genre } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(genres.entries.toList(), key = { it.key }) { (genre, tracks) ->
                            GroupRowCard(
                                title = genre,
                                subtitle = "${tracks.size} tracks",
                                artworkUri = tracks.firstNotNullOfOrNull { it.artworkUri },
                                fallbackIcon = Icons.Default.AutoAwesome,
                                onClick = { drillDownGroup = genre to tracks }
                            )
                        }
                    }
                }

                LibraryCategory.FOLDERS -> {
                    val folders = remember(localTracks) { localTracks.groupBy { it.folderPath.ifBlank { "Device Audio" } } }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folders.entries.toList(), key = { it.key }) { (folder, tracks) ->
                            ThemedCard(onClick = { drillDownGroup = folder.substringAfterLast('/') to tracks }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(folder.substringAfterLast('/'), style = MaterialTheme.typography.titleMedium)
                                        Text("${tracks.size} tracks · $folder", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { onExcludeFolder(folder) }) {
                                        Icon(Icons.Default.FolderOff, contentDescription = "Exclude folder")
                                    }
                                }
                            }
                        }
                    }
                }

                LibraryCategory.PLAYLISTS -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 120.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { showCreatePlaylistDialog = true }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("New / Smart Playlist")
                                }
                                OutlinedButton(onClick = { m3uImportLauncher.launch(arrayOf("*/*")) }) {
                                    Icon(Icons.Default.FileOpen, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Import M3U")
                                }
                            }
                        }
                        items(playlists, key = { it.id }) { pl ->
                            val resolved = onResolvePlaylistTracks(pl)
                            ThemedCard(onClick = { drillDownGroup = pl.name to resolved }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TrackArtwork(artworkUri = pl.artworkUri ?: resolved.firstOrNull()?.artworkUri, contentDescription = pl.name, size = 52.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pl.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (pl.isSmart) "Smart Playlist · ${resolved.size} matched" else "${resolved.size} tracks",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = {
                                        pendingExportPlaylist = pl
                                        m3uExportLauncher.launch("${pl.name}.m3u8")
                                    }) { Icon(Icons.Default.Download, contentDescription = "Export M3U8") }
                                    IconButton(onClick = { onDuplicatePlaylist(pl) }) { Icon(Icons.Default.Add, contentDescription = "Duplicate") }
                                    IconButton(onClick = { onDeletePlaylist(pl.id) }) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = { name, rule ->
                onCreatePlaylist(name, rule)
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showExcludedDialog) {
        AlertDialog(
            onDismissRequest = { showExcludedDialog = false },
            title = { Text("Excluded Folders") },
            text = {
                if (excludedFolders.isEmpty()) {
                    Text("No folders excluded. Tap the folder-off icon on any folder in Folders view to hide it from scans.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        excludedFolders.forEach { ef ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(ef.folderPath, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { onRemoveExcludedFolder(ef.folderPath) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Restore folder")
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showExcludedDialog = false }) { Text("Close") } }
        )
    }

    trackForPlaylistPicker?.let { track ->
        AlertDialog(
            onDismissRequest = { trackForPlaylistPicker = null },
            title = { Text("Add to Playlist") },
            text = {
                val staticLists = playlists.filter { !it.isSmart }
                if (staticLists.isEmpty()) {
                    Text("Create a static playlist first.")
                } else {
                    Column {
                        staticLists.forEach { pl ->
                            TextButton(
                                onClick = {
                                    onAddTrackToPlaylist(pl, track)
                                    trackForPlaylistPicker = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(pl.name) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { trackForPlaylistPicker = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun GroupRowCard(
    title: String,
    subtitle: String,
    artworkUri: String?,
    fallbackIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ThemedCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!artworkUri.isNullOrBlank()) {
                TrackArtwork(artworkUri = artworkUri, contentDescription = title, size = 50.dp)
            } else {
                Icon(fallbackIcon, contentDescription = null, modifier = Modifier.size(44.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (String, SmartPlaylistRule?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var isSmart by remember { mutableStateOf(false) }
    val selectedRules = remember { mutableStateListOf<SmartRuleType>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isSmart = !isSmart }
                ) {
                    Checkbox(checked = isSmart, onCheckedChange = { isSmart = it })
                    Text("Smart Playlist (Dynamic Local Rules)")
                }
                if (isSmart) {
                    SmartRuleType.entries.take(6).forEach { rule ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (rule in selectedRules) selectedRules.remove(rule) else selectedRules.add(rule)
                                }
                        ) {
                            Checkbox(
                                checked = rule in selectedRules,
                                onCheckedChange = { checked ->
                                    if (checked) selectedRules.add(rule) else selectedRules.remove(rule)
                                }
                            )
                            Text(rule.label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val rule = if (isSmart && selectedRules.isNotEmpty()) SmartPlaylistRule(selectedRules.toList()) else null
                onCreate(name.ifBlank { "My Playlist" }, rule)
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
