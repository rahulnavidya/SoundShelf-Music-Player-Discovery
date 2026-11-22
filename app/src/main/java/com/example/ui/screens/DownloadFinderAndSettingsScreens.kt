package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.AnimationIntensity
import com.example.core.designsystem.ArtworkPresentation
import com.example.core.designsystem.ButtonGeometry
import com.example.core.designsystem.CardStyle
import com.example.core.designsystem.InterfaceDensity
import com.example.core.designsystem.NavigationStyle
import com.example.core.designsystem.SoundShelfPalettes
import com.example.core.designsystem.SoundShelfThemeId
import com.example.core.designsystem.ThemeCustomizations
import com.example.core.designsystem.ThemeFontStyle
import com.example.core.model.ProviderId
import com.example.downloadfinder.DownloadFinderQuery
import com.example.persistence.UserPreferencesState
import com.example.providers.ProviderUiStatus
import com.example.ui.DownloadFinderUiState
import com.example.ui.components.ProviderBadge
import com.example.ui.components.ThemedCard

@Composable
fun DownloadFinderScreen(
    state: DownloadFinderUiState,
    isLocalOnlyMode: Boolean,
    onBack: () -> Unit,
    onSearch: (DownloadFinderQuery) -> Unit,
    onDownloadOrOpen: (com.example.core.model.DownloadSourceResult) -> Unit,
    onConfirmUpgrade: () -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember(state.query.title) { mutableStateOf(state.query.title) }
    var artist by remember(state.query.artist) { mutableStateOf(state.query.artist) }
    var album by remember(state.query.album) { mutableStateOf(state.query.album) }
    var isrc by remember(state.query.isrc) { mutableStateOf(state.query.isrc) }
    var version by remember(state.query.version) { mutableStateOf(state.query.version) }
    val versions = listOf("Any", "Original Release", "Remaster", "Live Recording", "Acoustic", "Extended Edition", "Instrumental")

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("download_finder_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Column {
                Text("Download Finder", style = MaterialTheme.typography.titleLarge)
                Text("Locate legitimate & higher-quality copies", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            state.targetTrackToUpgrade?.let { target ->
                item {
                    ThemedCard {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Find a Better Version", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Current Local Copy: ${target.title} · ${target.qualityInfo.summaryBadge()}", style = MaterialTheme.typography.bodyMedium)
                            Text("Original files are never auto-deleted. Upgrading preserves your playlists & play counts.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Song Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = artist, onValueChange = { artist = it }, label = { Text("Artist") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = album, onValueChange = { album = it }, label = { Text("Album (Optional)") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = isrc, onValueChange = { isrc = it }, label = { Text("ISRC") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(versions) { v ->
                                FilterChip(selected = version == v, onClick = { version = v }, label = { Text(v) })
                            }
                        }
                        Button(
                            onClick = {
                                onSearch(DownloadFinderQuery(title = title, artist = artist, album = album, version = version, isrc = isrc))
                            },
                            modifier = Modifier.fillMaxWidth().testTag("download_finder_search_button")
                        ) {
                            Text("Search Authorised Sources")
                        }
                        if (isLocalOnlyMode) {
                            Text(
                                "Local-Only Mode is active: direct Audius/Jamendo API queries are skipped; official store links remain available.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            if (state.isSearching) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            state.lastVerification?.let { ver ->
                item {
                    ThemedCard {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Verified Import Ready", style = MaterialTheme.typography.titleMedium)
                            }
                            Text("Format: ${ver.importedTrack.qualityInfo.summaryBadge()}", style = MaterialTheme.typography.bodyMedium)
                            Text("SHA-256: ${ver.sha256Checksum.take(24)}...", style = MaterialTheme.typography.labelMedium)
                            if (state.targetTrackToUpgrade != null) {
                                Button(onClick = onConfirmUpgrade) {
                                    Text("Replace Old Library Reference Safely")
                                }
                            }
                        }
                    }
                }
            }

            state.statusMessage?.let { msg ->
                item {
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }

            items(state.results, key = { it.id }) { source ->
                ThemedCard {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProviderBadge(source.providerId)
                            Text(source.sourceName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(source.priceLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                        Text("${source.title} — ${source.artist}", style = MaterialTheme.typography.bodyLarge)
                        Text("Version: ${source.versionTag} · ${source.matchConfidence}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Format: ${source.format} · Licence: ${source.license}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (state.isDownloadingId == source.id) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        } else {
                            FilledTonalButton(onClick = { onDownloadOrOpen(source) }) {
                                Icon(
                                    imageVector = if (source.isDirectDownload) Icons.Default.Download else Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(if (source.isDirectDownload) "Download & Verify Copy" else "Open Official Page")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeStudioScreen(
    currentThemeId: SoundShelfThemeId,
    customizations: ThemeCustomizations,
    onBack: () -> Unit,
    onSelectTheme: (SoundShelfThemeId) -> Unit,
    onUpdateCustomizations: ((ThemeCustomizations) -> ThemeCustomizations) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentSwatches = listOf(0xFFF2B84BL, 0xFF38BDF8L, 0xFFFF5E7EL, 0xFF2DD4BFL, 0xFFFACC15L, 0xFFA78BFAL, 0xFFEF4444L)
    val bgSwatches = listOf(0xFF0B0D13L, 0xFF000000L, 0xFF090E1CL, 0xFF151210L, 0xFF120821L, 0xFFF8F9FAL)

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("theme_studio_screen")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text("Theme Studio (15 Identities)", style = MaterialTheme.typography.titleLarge)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Curated Visual Themes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(SoundShelfThemeId.entries, key = { it.name }) { themeId ->
                val pal = SoundShelfPalettes.get(themeId)
                val selected = themeId == currentThemeId
                Surface(
                    onClick = { onSelectTheme(themeId) },
                    shape = RoundedCornerShape(14.dp),
                    color = pal.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) pal.primary else pal.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(pal.background)
                                .padding(6.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(pal.primary))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(themeId.displayName, style = MaterialTheme.typography.titleMedium, color = pal.textPrimary)
                            Text(themeId.subtitle, style = MaterialTheme.typography.bodyMedium, color = pal.textSecondary)
                        }
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Active theme", tint = pal.primary)
                        }
                    }
                }
            }

            item {
                ThemedCard {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Custom Theme Editor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        Text("Accent Colour Override", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(accentSwatches) { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .clickable { onUpdateCustomizations { it.copy(customAccentHex = hex) } }
                                )
                            }
                            item {
                                OutlinedButton(onClick = { onUpdateCustomizations { it.copy(customAccentHex = null) } }) { Text("Theme Default") }
                            }
                        }

                        Text("Background Override", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(bgSwatches) { hex ->
                                Surface(
                                    modifier = Modifier.size(34.dp).clickable { onUpdateCustomizations { it.copy(customBackgroundHex = hex) } },
                                    shape = CircleShape,
                                    color = Color(hex),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {}
                            }
                            item {
                                OutlinedButton(onClick = { onUpdateCustomizations { it.copy(customBackgroundHex = null) } }) { Text("Reset") }
                            }
                        }

                        Text("Typography Pairing", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ThemeFontStyle.entries) { font ->
                                FilterChip(
                                    selected = customizations.fontStyle == font,
                                    onClick = { onUpdateCustomizations { it.copy(fontStyle = font) } },
                                    label = { Text(font.label) }
                                )
                            }
                        }

                        Text("Button & Corner Geometry", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ButtonGeometry.entries) { geo ->
                                FilterChip(
                                    selected = customizations.buttonGeometry == geo,
                                    onClick = { onUpdateCustomizations { it.copy(buttonGeometry = geo) } },
                                    label = { Text(geo.label) }
                                )
                            }
                        }

                        Text("Card Surface Treatment", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(CardStyle.entries) { cs ->
                                FilterChip(
                                    selected = customizations.cardStyle == cs,
                                    onClick = { onUpdateCustomizations { it.copy(cardStyle = cs) } },
                                    label = { Text(cs.label) }
                                )
                            }
                        }

                        Text("Artwork Presentation", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(ArtworkPresentation.entries) { art ->
                                FilterChip(
                                    selected = customizations.artworkPresentation == art,
                                    onClick = { onUpdateCustomizations { it.copy(artworkPresentation = art) } },
                                    label = { Text(art.label) }
                                )
                            }
                        }

                        Text("Navigation & Density", style = MaterialTheme.typography.labelMedium)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(NavigationStyle.entries) { nav ->
                                FilterChip(
                                    selected = customizations.navigationStyle == nav,
                                    onClick = { onUpdateCustomizations { it.copy(navigationStyle = nav) } },
                                    label = { Text(nav.label) }
                                )
                            }
                            items(InterfaceDensity.entries) { den ->
                                FilterChip(
                                    selected = customizations.density == den,
                                    onClick = { onUpdateCustomizations { it.copy(density = den) } },
                                    label = { Text(den.label) }
                                )
                            }
                            items(AnimationIntensity.entries) { anim ->
                                FilterChip(
                                    selected = customizations.animationIntensity == anim,
                                    onClick = { onUpdateCustomizations { it.copy(animationIntensity = anim) } },
                                    label = { Text(anim.label) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    prefs: UserPreferencesState,
    providerStatuses: List<ProviderUiStatus>,
    onOpenThemeStudio: () -> Unit,
    onOpenAudioLab: () -> Unit,
    onOpenDownloadFinder: () -> Unit,
    onSetLocalOnlyMode: (Boolean) -> Unit,
    onSetProviderEnabled: (ProviderId, Boolean) -> Unit,
    onSaveJamendoClientId: (String) -> Unit,
    onDisconnectSpotify: () -> Unit,
    onOpenInSpotify: (String) -> Unit,
    onOpenInYouTubeMusic: (String) -> Unit,
    onSetHaptics: (Boolean) -> Unit,
    onSetHistoryTracking: (Boolean) -> Unit,
    onSetOnlineLyrics: (Boolean) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var jamendoIdDraft by remember(prefs.jamendoClientId) { mutableStateOf(prefs.jamendoClientId) }

    LazyColumn(
        modifier = modifier.fillMaxSize().testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Settings & Services", style = MaterialTheme.typography.headlineMedium)
        }

        item {
            ThemedCard {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Privacy & Edition Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Local-Only Offline Mode", style = MaterialTheme.typography.bodyLarge)
                            Text("Blocks all online provider network requests. Pure local player.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = prefs.isLocalOnlyMode, onCheckedChange = onSetLocalOnlyMode)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Haptic Feedback")
                        Switch(checked = prefs.hapticsEnabled, onCheckedChange = onSetHaptics)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Track Listening History")
                        Switch(checked = prefs.historyTrackingEnabled, onCheckedChange = onSetHistoryTracking)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Online Community LRC Lookup")
                        Switch(checked = prefs.onlineLyricsEnabled, onCheckedChange = onSetOnlineLyrics)
                    }
                    OutlinedButton(onClick = onClearHistory, modifier = Modifier.fillMaxWidth()) {
                        Text("Clear Listening History")
                    }
                    Text(
                        "Zero ads · Zero telemetry · Zero analytics SDKs · No mandatory account.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onOpenThemeStudio, modifier = Modifier.weight(1f)) { Text("Theme Studio") }
                FilledTonalButton(onClick = onOpenAudioLab, modifier = Modifier.weight(1f)) { Text("Audio Lab") }
                OutlinedButton(onClick = onOpenDownloadFinder, modifier = Modifier.weight(1f)) { Text("Downloads") }
            }
        }

        item {
            Text("Connected Services & Providers", style = MaterialTheme.typography.titleLarge)
        }

        items(providerStatuses, key = { it.descriptor.id.name }) { item ->
            val pid = item.descriptor.id
            ThemedCard {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                ProviderBadge(pid)
                                Text(item.descriptor.displayName, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(
                                "Status: ${item.status.label} · Auth: ${item.descriptor.authType.label}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Switch(
                            checked = pid in prefs.enabledProviders && !prefs.isLocalOnlyMode,
                            enabled = !prefs.isLocalOnlyMode,
                            onCheckedChange = { onSetProviderEnabled(pid, it) }
                        )
                    }
                    Text(item.descriptor.description, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Privacy: ${item.descriptor.privacyNotice}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Restrictions: ${item.descriptor.restrictionNotice}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (pid == ProviderId.JAMENDO) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = jamendoIdDraft,
                                onValueChange = { jamendoIdDraft = it },
                                label = { Text("Jamendo Client ID") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Button(onClick = { onSaveJamendoClientId(jamendoIdDraft) }) { Text("Save") }
                        }
                    }

                    if (pid == ProviderId.SPOTIFY) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onOpenInSpotify("") }) { Text("Launch Spotify") }
                            OutlinedButton(onClick = onDisconnectSpotify) { Text("Clear Session") }
                        }
                    }

                    if (pid == ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL) {
                        OutlinedButton(onClick = { onOpenInYouTubeMusic("https://music.youtube.com") }) {
                            Text("Open Official YouTube Music")
                        }
                    }
                }
            }
        }
    }
}
