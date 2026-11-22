package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.persistence.UserPreferencesState
import com.example.providers.ProviderResult
import com.example.providers.ProviderUiStatus
import com.example.ui.SearchUiState
import com.example.ui.components.ProviderBadge
import com.example.ui.components.ThemedCard
import com.example.ui.components.TrackArtwork
import com.example.ui.components.TrackRow

@Composable
fun SearchScreen(
    searchState: SearchUiState,
    prefs: UserPreferencesState,
    currentTrackId: String?,
    onQueryChange: (String) -> Unit,
    onExecuteOnlineSearch: () -> Unit,
    onSelectProviderFilter: (ProviderId?) -> Unit,
    onClearRecentSearches: () -> Unit,
    onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    onToggleFavorite: (MusicTrack) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAddToQueue: (MusicTrack) -> Unit,
    onShowAudioInfo: (MusicTrack) -> Unit,
    onFindDownload: (MusicTrack) -> Unit,
    onOpenInSpotify: (String) -> Unit,
    onOpenInYouTubeMusic: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize().testTag("search_screen")) {
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onQueryChange,
            placeholder = { Text("Search songs, artists, albums, genres, years...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchState.query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onExecuteOnlineSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .testTag("search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = searchState.selectedProviderFilter == null,
                    onClick = { onSelectProviderFilter(null) },
                    label = { Text("All Enabled") }
                )
            }
            items(prefs.enabledProviders.toList()) { pid ->
                FilterChip(
                    selected = searchState.selectedProviderFilter == pid,
                    onClick = { onSelectProviderFilter(pid) },
                    label = { Text(pid.badgeLabel) }
                )
            }
        }

        if (searchState.query.isNotBlank() && !prefs.isLocalOnlyMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onExecuteOnlineSearch, modifier = Modifier.weight(1f).testTag("search_online_button")) {
                    Text("Search Online Catalogues")
                }
            }
        }

        if (searchState.isSearchingOnline) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 120.dp)) {
            if (searchState.query.isBlank()) {
                if (prefs.recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
                            TextButton(onClick = onClearRecentSearches) { Text("Clear") }
                        }
                    }
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(prefs.recentSearches) { recent ->
                                AssistChip(
                                    onClick = {
                                        onQueryChange(recent)
                                        onExecuteOnlineSearch()
                                    },
                                    label = { Text(recent) }
                                )
                            }
                        }
                    }
                }
                item {
                    ThemedCard(modifier = Modifier.padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Unified Local & Provider Search", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Local files match instantaneously as you type across title, artist, album, genre, path, and year. Online providers are queried only when you submit.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Local Results
                if (searchState.selectedProviderFilter == null || searchState.selectedProviderFilter == ProviderId.LOCAL) {
                    if (searchState.localTracks.isNotEmpty()) {
                        item {
                            Text(
                                "Local Library (${searchState.localTracks.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        itemsIndexed(searchState.localTracks, key = { _, t -> t.id }) { idx, track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                hapticsEnabled = prefs.hapticsEnabled,
                                onClick = { onPlayTracks(searchState.localTracks, idx) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = {},
                                onShowAudioInfo = { onShowAudioInfo(track) },
                                onFindDownload = { onFindDownload(track) }
                            )
                        }
                    }
                }

                // Online Provider Results (isolated per provider with explicit source attribution)
                searchState.onlineAggregates.forEach { agg ->
                    item(key = "hdr_${agg.providerId.name}") {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ProviderBadge(agg.providerId)
                            Text(agg.providerId.displayName, style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    if (agg.providerId == ProviderId.SPOTIFY || agg.providerId == ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL) {
                        item(key = "handoff_${agg.providerId.name}") {
                            ThemedCard(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(agg.errorMessage ?: "External App Handoff", style = MaterialTheme.typography.bodyMedium)
                                    OutlinedButton(onClick = {
                                        if (agg.providerId == ProviderId.SPOTIFY) onOpenInSpotify(searchState.query)
                                        else onOpenInYouTubeMusic(searchState.query)
                                    }) {
                                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Open \"${searchState.query}\" in ${agg.providerId.badgeLabel}")
                                    }
                                }
                            }
                        }
                    } else if (agg.errorMessage != null && agg.tracks.isEmpty()) {
                        item(key = "err_${agg.providerId.name}") {
                            Text(
                                agg.errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        itemsIndexed(agg.tracks, key = { _, t -> t.id }) { idx, track ->
                            TrackRow(
                                track = track,
                                isCurrent = track.id == currentTrackId,
                                hapticsEnabled = prefs.hapticsEnabled,
                                onClick = { onPlayTracks(agg.tracks, idx) },
                                onToggleFavorite = { onToggleFavorite(track) },
                                onPlayNext = { onPlayNext(track) },
                                onAddToQueue = { onAddToQueue(track) },
                                onAddToPlaylist = {},
                                onShowAudioInfo = { onShowAudioInfo(track) },
                                onFindDownload = { onFindDownload(track) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverScreen(
    prefs: UserPreferencesState,
    providerStatuses: List<ProviderUiStatus>,
    discoveryTracks: Map<ProviderId, List<MusicTrack>>,
    discoveryErrors: Map<ProviderId, String>,
    isLoading: Boolean,
    currentTrackId: String?,
    onRefreshDiscovery: () -> Unit,
    onPlayTracks: (List<MusicTrack>, Int) -> Unit,
    onToggleFavorite: (MusicTrack) -> Unit,
    onPlayNext: (MusicTrack) -> Unit,
    onAddToQueue: (MusicTrack) -> Unit,
    onShowAudioInfo: (MusicTrack) -> Unit,
    onOpenDownloadFinder: () -> Unit,
    onFindDownloadForTrack: (MusicTrack) -> Unit,
    onOpenConnectedServices: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("discover_screen"),
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text("Discover", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Modular online catalogues & legitimate download discovery",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!prefs.isLocalOnlyMode) {
                    IconButton(onClick = onRefreshDiscovery) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh discovery")
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenDownloadFinder, modifier = Modifier.weight(1f).testTag("open_download_finder_button")) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Download Finder")
                }
                OutlinedButton(onClick = onOpenConnectedServices, modifier = Modifier.weight(1f).testTag("open_services_button")) {
                    Text("Connected Services")
                }
            }
        }

        if (prefs.isLocalOnlyMode) {
            item {
                ThemedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Local-Only Mode Active", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Zero network requests are made while Local-Only Mode is enabled.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            if (isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            if (discoveryTracks.isEmpty() && discoveryErrors.isEmpty()) {
                item {
                    ThemedCard(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = onRefreshDiscovery
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Load Provider Charts", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap to fetch real-time trending tracks from your enabled providers (Audius, Jamendo).",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            discoveryTracks.forEach { (providerId, tracks) ->
                item(key = "disc_hdr_${providerId.name}") {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProviderBadge(providerId)
                        Text("${providerId.displayName} Charts", style = MaterialTheme.typography.titleLarge)
                    }
                }
                itemsIndexed(tracks, key = { _, t -> t.id }) { idx, track ->
                    TrackRow(
                        track = track,
                        isCurrent = track.id == currentTrackId,
                        hapticsEnabled = prefs.hapticsEnabled,
                        onClick = { onPlayTracks(tracks, idx) },
                        onToggleFavorite = { onToggleFavorite(track) },
                        onPlayNext = { onPlayNext(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = {},
                        onShowAudioInfo = { onShowAudioInfo(track) },
                        onFindDownload = { onFindDownloadForTrack(track) }
                    )
                }
            }

            discoveryErrors.forEach { (pid, msg) ->
                item(key = "disc_err_${pid.name}") {
                    ThemedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            ProviderBadge(pid)
                            Spacer(Modifier.height(4.dp))
                            Text(msg, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Provider Capabilities",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        items(providerStatuses, key = { it.descriptor.id.name }) { status ->
            ThemedCard(
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = onOpenConnectedServices
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProviderBadge(status.descriptor.id)
                        Text(status.descriptor.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        Text(status.status.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        status.descriptor.restrictionNotice,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
