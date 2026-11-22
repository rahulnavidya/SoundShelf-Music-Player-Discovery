package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.designsystem.LocalSoundShelfTokens
import com.example.core.model.HomeSectionId
import com.example.core.model.LibraryCategory
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.persistence.HomeConfig
import com.example.persistence.PlaybackHistoryEntity
import com.example.persistence.UserPreferencesState
import com.example.playback.PlaybackUiState
import com.example.ui.components.ProviderBadge
import com.example.ui.components.ThemedCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.formatDuration

@Composable
fun HomeScreen(
    prefs: UserPreferencesState,
    localTracks: List<MusicTrack>,
    playlists: List<MusicPlaylist>,
    history: List<PlaybackHistoryEntity>,
    playbackState: PlaybackUiState,
    discoveryTracks: Map<ProviderId, List<MusicTrack>>,
    discoveryErrors: Map<ProviderId, String>,
    isLoadingDiscovery: Boolean,
    onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    onResumeSaved: () -> Unit,
    onRefreshDiscovery: () -> Unit,
    onOpenLibraryCategory: (LibraryCategory) -> Unit,
    onOpenDownloadFinder: () -> Unit,
    onOpenThemeStudio: () -> Unit,
    onUpdateHomeConfig: ((HomeConfig) -> HomeConfig) -> Unit,
    onTrackClick: (MusicTrack, List<MusicTrack>) -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = LocalSoundShelfTokens.current
    var showCustomizeDialog by remember { mutableStateOf(false) }

    val favoriteTracks = remember(localTracks) { localTracks.filter { it.isFavorite } }
    val recentlyAdded = remember(localTracks) { localTracks.sortedByDescending { it.dateAddedSec }.take(12) }
    val mostPlayed = remember(localTracks) { localTracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }.take(10) }
    val downloadedTracks = remember(localTracks) { localTracks.filter { it.folderPath.contains("SoundShelfDownloads", true) } }
    val missingArtCount = remember(localTracks) { localTracks.count { it.artworkUri.isNullOrBlank() } }
    val topArtists = remember(localTracks) {
        localTracks.groupBy { it.artist }.entries
            .sortedByDescending { (_, list) -> list.sumOf { it.playCount }.coerceAtLeast(list.size) }
            .take(8)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(tokens.itemSpacing + 4.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SoundShelf",
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (prefs.isLocalOnlyMode) "Offline Studio · Local-Only Mode" else "Your Music. Your Library.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onOpenThemeStudio, modifier = Modifier.testTag("home_theme_button")) {
                        Icon(Icons.Default.Palette, contentDescription = "Theme Studio")
                    }
                    IconButton(onClick = { showCustomizeDialog = true }, modifier = Modifier.testTag("home_customize_button")) {
                        Icon(Icons.Default.Tune, contentDescription = "Customize Home")
                    }
                }
            }
        }

        prefs.homeConfig.sectionOrder.forEach { section ->
            if (section in prefs.homeConfig.hiddenSections) return@forEach

            when (section) {
                HomeSectionId.HERO_BANNER -> {
                    item(key = section.name) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(168.dp)
                                .clip(RoundedCornerShape(tokens.cardCornerRadius))
                                .clickable {
                                    if (localTracks.isNotEmpty()) onPlayTracks(localTracks.shuffled(), 0)
                                    else onOpenLibraryCategory(LibraryCategory.SONGS)
                                }
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_hero_studio),
                                contentDescription = "SoundShelf Studio Showcase",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.82f),
                                                Color.Black.copy(alpha = 0.25f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                SurfaceBadge(
                                    text = if (localTracks.isEmpty()) "READY FOR YOUR MUSIC" else "${localTracks.size} LOCAL TRACKS INDEXED"
                                )
                                Column {
                                    Text(
                                        text = if (playbackState.currentTrack != null) "Resume Your Session" else "Pure Listening Craft",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                if (playbackState.currentTrack != null) onResumeSaved()
                                                else if (localTracks.isNotEmpty()) onPlayTracks(localTracks.shuffled(), 0)
                                                else onOpenLibraryCategory(LibraryCategory.SONGS)
                                            },
                                            modifier = Modifier.testTag("hero_primary_action")
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                when {
                                                    playbackState.currentTrack != null -> "Continue"
                                                    localTracks.isNotEmpty() -> "Shuffle All"
                                                    else -> "Scan Library"
                                                }
                                            )
                                        }
                                        FilledTonalButton(onClick = onOpenDownloadFinder) {
                                            Icon(Icons.Default.Download, contentDescription = null)
                                            Spacer(Modifier.width(4.dp))
                                            Text("Find Copies")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSectionId.QUICK_ACCESS -> {
                    item(key = section.name) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader("Quick Access")
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickAccessTile(
                                    title = "Favourites",
                                    subtitle = "${favoriteTracks.size} songs",
                                    icon = Icons.Default.Favorite,
                                    onClick = { onOpenLibraryCategory(LibraryCategory.FAVORITES) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickAccessTile(
                                    title = "Recently Added",
                                    subtitle = "${recentlyAdded.size} tracks",
                                    icon = Icons.Default.LibraryMusic,
                                    onClick = { onOpenLibraryCategory(LibraryCategory.RECENTLY_ADDED) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickAccessTile(
                                    title = "My Playlists",
                                    subtitle = "${playlists.size} lists",
                                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                                    onClick = { onOpenLibraryCategory(LibraryCategory.PLAYLISTS) },
                                    modifier = Modifier.weight(1f)
                                )
                                QuickAccessTile(
                                    title = "Downloaded",
                                    subtitle = "${downloadedTracks.size} verified",
                                    icon = Icons.Default.Download,
                                    onClick = onOpenDownloadFinder,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                HomeSectionId.CONTINUE_LISTENING -> {
                    playbackState.currentTrack?.let { activeTrack ->
                        item(key = section.name) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                SectionHeader("Continue Listening")
                                Spacer(Modifier.height(8.dp))
                                ThemedCard(onClick = onResumeSaved) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TrackArtwork(
                                            artworkUri = activeTrack.artworkUri,
                                            contentDescription = activeTrack.title,
                                            size = 64.dp,
                                            isSpinning = playbackState.isPlaying
                                        )
                                        Spacer(Modifier.width(14.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = activeTrack.title,
                                                style = MaterialTheme.typography.titleLarge,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${activeTrack.artist} · ${activeTrack.album}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = "${formatDuration(playbackState.positionMs)} / ${formatDuration(playbackState.durationMs)} · ${activeTrack.qualityInfo.summaryBadge()}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Resume",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSectionId.RECENTLY_PLAYED -> {
                    if (prefs.homeConfig.showRecentlyPlayed && history.isNotEmpty()) {
                        item(key = section.name) {
                            Column {
                                SectionHeader("Recently Played", modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(8.dp))
                                val distinctRecent = remember(history, localTracks) {
                                    val byId = localTracks.associateBy { it.id }
                                    history.distinctBy { it.trackId }.take(10).mapNotNull { h -> byId[h.trackId] }
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(distinctRecent, key = { it.id }) { track ->
                                        TrackCardSquare(track = track, onClick = { onTrackClick(track, distinctRecent) })
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSectionId.LOCAL_HIGHLIGHTS -> {
                    item(key = section.name) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SectionHeader("Local Music Highlights", modifier = Modifier.padding(horizontal = 16.dp))
                            if (localTracks.isEmpty()) {
                                ThemedCard(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    onClick = { onOpenLibraryCategory(LibraryCategory.SONGS) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.FolderOpen,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Build Your Local Shelf", style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "Grant audio access or import audio files directly via Storage Access Framework.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                if (recentlyAdded.isNotEmpty()) {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(recentlyAdded.take(8), key = { it.id }) { track ->
                                            TrackCardSquare(track = track, onClick = { onTrackClick(track, recentlyAdded) })
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    AssistChip(
                                        onClick = { onOpenLibraryCategory(LibraryCategory.ARTISTS) },
                                        label = { Text("Top Artists (${topArtists.size})") }
                                    )
                                    if (mostPlayed.isNotEmpty()) {
                                        AssistChip(
                                            onClick = { onOpenLibraryCategory(LibraryCategory.MOST_PLAYED) },
                                            label = { Text("Heavy Rotation (${mostPlayed.size})") }
                                        )
                                    }
                                    if (missingArtCount > 0) {
                                        AssistChip(
                                            onClick = { onOpenLibraryCategory(LibraryCategory.SONGS) },
                                            label = { Text("$missingArtCount Missing Art") }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSectionId.ONLINE_DISCOVERY -> {
                    if (prefs.homeConfig.showOnlineDiscovery) {
                        item(key = section.name) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader(
                                        if (prefs.homeConfig.separateLocalAndOnline) "Online Discovery (Separated)" else "Online Discovery"
                                    )
                                    if (!prefs.isLocalOnlyMode) {
                                        IconButton(onClick = onRefreshDiscovery) {
                                            Icon(Icons.Default.Refresh, contentDescription = "Refresh online discovery")
                                        }
                                    }
                                }

                                if (prefs.isLocalOnlyMode) {
                                    ThemedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                            Spacer(Modifier.width(12.dp))
                                            Text(
                                                "Local-Only Mode is enabled. All local library sections stay 100% available offline.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                } else if (discoveryTracks.isEmpty() && discoveryErrors.isEmpty()) {
                                    ThemedCard(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        onClick = onRefreshDiscovery
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    if (isLoadingDiscovery) "Fetching Audius & Jamendo Charts..." else "Tap to Load Online Charts",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    "SoundShelf only contacts enabled providers when you request discovery.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    discoveryTracks.forEach { (providerId, tracks) ->
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                ProviderBadge(providerId)
                                                Text(
                                                    text = "Trending on ${providerId.displayName}",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                            }
                                            LazyRow(
                                                contentPadding = PaddingValues(horizontal = 16.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                items(tracks, key = { it.id }) { track ->
                                                    TrackCardSquare(track = track, onClick = { onTrackClick(track, tracks) })
                                                }
                                            }
                                        }
                                    }
                                    discoveryErrors.forEach { (providerId, err) ->
                                        ThemedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                ProviderBadge(providerId)
                                                Spacer(Modifier.height(4.dp))
                                                Text(err, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HomeSectionId.SMART_MIXES -> {
                    if (playlists.isNotEmpty()) {
                        item(key = section.name) {
                            Column {
                                SectionHeader("Playlists & Smart Mixes", modifier = Modifier.padding(horizontal = 16.dp))
                                Spacer(Modifier.height(8.dp))
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(playlists, key = { it.id }) { pl ->
                                        ThemedCard(
                                            modifier = Modifier.width(156.dp),
                                            onClick = { onOpenLibraryCategory(LibraryCategory.PLAYLISTS) }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                TrackArtwork(
                                                    artworkUri = pl.artworkUri,
                                                    contentDescription = pl.name,
                                                    size = 132.dp
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Text(pl.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                Text(
                                                    if (pl.isSmart) "Smart Rule" else "${pl.trackIds.size} tracks",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomizeDialog) {
        HomeCustomizationDialog(
            config = prefs.homeConfig,
            onDismiss = { showCustomizeDialog = false },
            onUpdate = onUpdateHomeConfig
        )
    }
}

@Composable
private fun SurfaceBadge(text: String) {
    androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

@Composable
private fun QuickAccessTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ThemedCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrackCardSquare(track: MusicTrack, onClick: () -> Unit) {
    ThemedCard(
        modifier = Modifier.width(148.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            TrackArtwork(
                artworkUri = track.artworkUri,
                contentDescription = track.title,
                size = 128.dp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.providerId != ProviderId.LOCAL) {
                Spacer(Modifier.height(4.dp))
                ProviderBadge(track.providerId)
            }
        }
    }
}

@Composable
private fun HomeCustomizationDialog(
    config: HomeConfig,
    onDismiss: () -> Unit,
    onUpdate: ((HomeConfig) -> HomeConfig) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customise Home Screen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Separate Local & Online", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = config.separateLocalAndOnline,
                        onCheckedChange = { checked -> onUpdate { it.copy(separateLocalAndOnline = checked) } }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show Online Discovery", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = config.showOnlineDiscovery,
                        onCheckedChange = { checked -> onUpdate { it.copy(showOnlineDiscovery = checked) } }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("Section Order & Visibility", style = MaterialTheme.typography.titleMedium)
                config.sectionOrder.forEachIndexed { idx, section ->
                    val hidden = section in config.hiddenSections
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(section.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(
                            enabled = idx > 0,
                            onClick = {
                                onUpdate { c ->
                                    val m = c.sectionOrder.toMutableList()
                                    val item = m.removeAt(idx)
                                    m.add(idx - 1, item)
                                    c.copy(sectionOrder = m)
                                }
                            }
                        ) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
                        IconButton(
                            enabled = idx < config.sectionOrder.lastIndex,
                            onClick = {
                                onUpdate { c ->
                                    val m = c.sectionOrder.toMutableList()
                                    val item = m.removeAt(idx)
                                    m.add(idx + 1, item)
                                    c.copy(sectionOrder = m)
                                }
                            }
                        ) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
                        IconButton(
                            onClick = {
                                onUpdate { c ->
                                    val nextHidden = if (hidden) c.hiddenSections - section else c.hiddenSections + section
                                    c.copy(hiddenSections = nextHidden)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (hidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle visibility"
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun ThemeOnboardingDialog(
    onExploreThemes: () -> Unit,
    onCreateOwn: () -> Unit,
    onSkip: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text("Welcome to SoundShelf") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Your Music. Your Library. Your Experience.\nChoose how SoundShelf looks and feels — no account required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onExploreThemes,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_explore_themes")
                ) {
                    Text("Explore 15 Curated Themes")
                }
                FilledTonalButton(
                    onClick = onCreateOwn,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_create_own")
                ) {
                    Text("Create My Own Theme")
                }
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth().testTag("onboarding_skip")
                ) {
                    Text("Skip for Now (Midnight Studio)")
                }
            }
        },
        confirmButton = {}
    )
}
