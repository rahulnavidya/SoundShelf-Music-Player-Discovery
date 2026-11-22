package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.core.designsystem.LocalSoundShelfTokens
import com.example.core.designsystem.NavigationStyle
import com.example.core.designsystem.SoundShelfTheme
import com.example.core.model.MusicTrack
import com.example.ui.SoundShelfViewModel
import com.example.ui.components.MiniPlayerBar
import com.example.ui.screens.AudioInfoScreen
import com.example.ui.screens.AudioLabScreen
import com.example.ui.screens.DiscoverScreen
import com.example.ui.screens.DownloadFinderScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsScreen
import com.example.ui.screens.MetadataEditorScreen
import com.example.ui.screens.NowPlayingScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.ThemeOnboardingDialog
import com.example.ui.screens.ThemeStudioScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object RouteHome
@Serializable object RouteLibrary
@Serializable object RouteSearch
@Serializable object RouteDiscover
@Serializable object RouteSettings
@Serializable object RouteNowPlaying
@Serializable object RouteQueue
@Serializable object RouteAudioLab
@Serializable object RouteThemeStudio
@Serializable object RouteDownloadFinder
@Serializable data class RouteAudioInfo(val trackId: String)
@Serializable data class RouteMetadataEditor(val trackId: String)
@Serializable data class RouteLyrics(val trackId: String)

private data class TopLevelTab(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

class MainActivity : ComponentActivity() {
    private val viewModel: SoundShelfViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleViewAudioIntent(intent)
        setContent {
            SoundShelfRoot(viewModel = viewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewAudioIntent(intent)
    }

    private fun handleViewAudioIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.importSafAudioFile(uri)
            }
        }
    }
}

@Composable
fun SoundShelfRoot(viewModel: SoundShelfViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val localTracks by viewModel.localTracks.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val excludedFolders by viewModel.excludedFolders.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val effectCapabilities by viewModel.effectCapabilities.collectAsStateWithLifecycle()
    val scanReport by viewModel.scanReport.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val libraryCategory by viewModel.libraryCategory.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val discoveryTracks by viewModel.discoveryTracks.collectAsStateWithLifecycle()
    val discoveryErrors by viewModel.discoveryErrors.collectAsStateWithLifecycle()
    val isLoadingDiscovery by viewModel.isLoadingDiscovery.collectAsStateWithLifecycle()
    val providerStatuses by viewModel.providerStatuses.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val currentLyrics by viewModel.currentLyrics.collectAsStateWithLifecycle()
    val downloadFinderState by viewModel.downloadFinderState.collectAsStateWithLifecycle()

    var inspectedTrackCache by remember { mutableStateOf<MusicTrack?>(null) }

    val sortedAllTracks = remember(localTracks, sortOption) {
        viewModel.sortedTracks(localTracks)
    }
    val libraryMap = remember(localTracks) { localTracks.associateBy { it.id } }

    SoundShelfTheme(
        themeId = prefs.themeId,
        customizations = prefs.themeCustomizations
    ) {
        val tokens = LocalSoundShelfTokens.current
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            viewModel.snackbarMessages.collect { msg ->
                snackbarHostState.showSnackbar(msg)
            }
        }

        val tabs = remember {
            listOf(
                TopLevelTab(RouteHome, "Home", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
                TopLevelTab(RouteLibrary, "Library", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic, "tab_library"),
                TopLevelTab(RouteSearch, "Search", Icons.Filled.Search, Icons.Outlined.Search, "tab_search"),
                TopLevelTab(RouteDiscover, "Discover", Icons.Filled.Explore, Icons.Outlined.Explore, "tab_discover"),
                TopLevelTab(RouteSettings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
            )
        }

        val showTopLevelChrome = currentDestination?.let { dest ->
            dest.hasRoute<RouteHome>() ||
                dest.hasRoute<RouteLibrary>() ||
                dest.hasRoute<RouteSearch>() ||
                dest.hasRoute<RouteDiscover>() ||
                dest.hasRoute<RouteSettings>()
        } ?: true

        val isTopLevelSelected: (Any) -> Boolean = { tabRoute ->
            when (tabRoute) {
                is RouteHome -> currentDestination?.hasRoute<RouteHome>() == true
                is RouteLibrary -> currentDestination?.hasRoute<RouteLibrary>() == true
                is RouteSearch -> currentDestination?.hasRoute<RouteSearch>() == true
                is RouteDiscover -> currentDestination?.hasRoute<RouteDiscover>() == true
                is RouteSettings -> currentDestination?.hasRoute<RouteSettings>() == true
                else -> false
            }
        }

        val navigateTopLevel: (Any) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(RouteHome) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }

        val openTrackInfo: (MusicTrack) -> Unit = { track ->
            inspectedTrackCache = track
            navController.navigate(RouteAudioInfo(track.id))
        }
        val openMetadataEditor: (MusicTrack) -> Unit = { track ->
            inspectedTrackCache = track
            navController.navigate(RouteMetadataEditor(track.id))
        }
        val openDownloadFinderFor: (MusicTrack) -> Unit = { track ->
            viewModel.prepareDownloadFinderForTrack(track)
            navController.navigate(RouteDownloadFinder)
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(tokens.palette.background, tokens.palette.backgroundGradientEnd)
                    )
                )
        ) {
            val useNavRail = maxWidth >= 680.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useNavRail && showTopLevelChrome) {
                    NavigationRail(
                        containerColor = tokens.palette.surface,
                        modifier = Modifier.fillMaxHeight().statusBarsPadding().navigationBarsPadding()
                    ) {
                        tabs.forEach { tab ->
                            val selected = isTopLevelSelected(tab.route)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(tab.route) },
                                icon = {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (showTopLevelChrome) {
                            Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
                                playbackState.currentTrack?.let { currentTrack ->
                                    MiniPlayerBar(
                                        track = currentTrack,
                                        isPlaying = playbackState.isPlaying,
                                        positionMs = playbackState.positionMs,
                                        durationMs = playbackState.durationMs,
                                        onExpand = { navController.navigate(RouteNowPlaying) },
                                        onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                                        onNext = { viewModel.playbackController.skipToNext() },
                                        onOpenQueue = { navController.navigate(RouteQueue) }
                                    )
                                }
                                if (!useNavRail) {
                                    if (tokens.navigationStyle == NavigationStyle.FLOATING_PILL) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(28.dp),
                                            color = tokens.palette.surfaceElevated,
                                            tonalElevation = 8.dp
                                        ) {
                                            NavigationBar(
                                                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                                                windowInsets = WindowInsets(0, 0, 0, 0)
                                            ) {
                                                tabs.forEach { tab ->
                                                    val selected = isTopLevelSelected(tab.route)
                                                    NavigationBarItem(
                                                        selected = selected,
                                                        onClick = { navigateTopLevel(tab.route) },
                                                        icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                                                        label = { Text(tab.label) },
                                                        modifier = Modifier.testTag(tab.testTag)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        NavigationBar(
                                            containerColor = tokens.palette.surface,
                                            windowInsets = WindowInsets(0, 0, 0, 0)
                                        ) {
                                            tabs.forEach { tab ->
                                                val selected = isTopLevelSelected(tab.route)
                                                NavigationBarItem(
                                                    selected = selected,
                                                    onClick = { navigateTopLevel(tab.route) },
                                                    icon = { Icon(if (selected) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                                                    label = { Text(tab.label) },
                                                    modifier = Modifier.testTag(tab.testTag)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .let { mod -> if (showTopLevelChrome) mod.statusBarsPadding() else mod }
                    ) {
                        NavHost(navController = navController, startDestination = RouteHome) {
                            composable<RouteHome> {
                                HomeScreen(
                                    prefs = prefs,
                                    localTracks = localTracks,
                                    playlists = playlists,
                                    history = history,
                                    playbackState = playbackState,
                                    discoveryTracks = discoveryTracks,
                                    discoveryErrors = discoveryErrors,
                                    isLoadingDiscovery = isLoadingDiscovery,
                                    onPlayTracks = { list, idx -> viewModel.playbackController.playTrackList(list, idx) },
                                    onResumeSaved = {
                                        if (playbackState.currentTrack != null) {
                                            navController.navigate(RouteNowPlaying)
                                        }
                                    },
                                    onRefreshDiscovery = { viewModel.refreshDiscovery() },
                                    onOpenLibraryCategory = { cat ->
                                        viewModel.setLibraryCategory(cat)
                                        navigateTopLevel(RouteLibrary)
                                    },
                                    onOpenDownloadFinder = { navController.navigate(RouteDownloadFinder) },
                                    onOpenThemeStudio = { navController.navigate(RouteThemeStudio) },
                                    onUpdateHomeConfig = { transform -> viewModel.updateHomeConfig(transform) },
                                    onTrackClick = { clicked, list ->
                                        val idx = list.indexOfFirst { it.id == clicked.id }.coerceAtLeast(0)
                                        viewModel.playbackController.playTrackList(list, idx)
                                    }
                                )
                            }

                            composable<RouteLibrary> {
                                LibraryScreen(
                                    localTracks = localTracks,
                                    sortedAllTracks = sortedAllTracks,
                                    playlists = playlists,
                                    excludedFolders = excludedFolders,
                                    selectedCategory = libraryCategory,
                                    sortOption = sortOption,
                                    isScanning = isScanning,
                                    scanReport = scanReport,
                                    currentTrackId = playbackState.currentTrack?.id,
                                    hapticsEnabled = prefs.hapticsEnabled,
                                    onSelectCategory = viewModel::setLibraryCategory,
                                    onSelectSort = viewModel::setSortOption,
                                    onScanLibrary = viewModel::scanLocalLibrary,
                                    onImportSafAudio = viewModel::importSafAudioFile,
                                    onImportM3u = viewModel::importM3uUri,
                                    onExportPlaylistM3u8 = viewModel::exportPlaylistM3u8,
                                    onExcludeFolder = viewModel::excludeFolder,
                                    onRemoveExcludedFolder = viewModel::removeExcludedFolder,
                                    onPlayTracks = { list, idx -> viewModel.playbackController.playTrackList(list, idx) },
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onToggleFavorite = viewModel::toggleFavoriteTrack,
                                    onCreatePlaylist = { name, rule -> viewModel.createPlaylist(name, smartRule = rule) },
                                    onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
                                    onDeletePlaylist = viewModel::deletePlaylist,
                                    onDuplicatePlaylist = viewModel::duplicatePlaylist,
                                    onResolvePlaylistTracks = { pl -> viewModel.resolvePlaylistTracks(pl, libraryMap) },
                                    onShowAudioInfo = openTrackInfo,
                                    onEditMetadata = openMetadataEditor,
                                    onFindDownload = openDownloadFinderFor
                                )
                            }

                            composable<RouteSearch> {
                                SearchScreen(
                                    searchState = searchState,
                                    prefs = prefs,
                                    currentTrackId = playbackState.currentTrack?.id,
                                    onQueryChange = viewModel::updateSearchQuery,
                                    onExecuteOnlineSearch = viewModel::executeOnlineSearch,
                                    onSelectProviderFilter = viewModel::setSearchProviderFilter,
                                    onClearRecentSearches = {
                                        scope.launch { viewModel.prefsRepo.clearRecentSearches() }
                                    },
                                    onPlayTracks = { list, idx -> viewModel.playbackController.playTrackList(list, idx) },
                                    onToggleFavorite = viewModel::toggleFavoriteTrack,
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onShowAudioInfo = openTrackInfo,
                                    onFindDownload = openDownloadFinderFor,
                                    onOpenInSpotify = viewModel::openInSpotify,
                                    onOpenInYouTubeMusic = viewModel::openInYouTubeMusic
                                )
                            }

                            composable<RouteDiscover> {
                                DiscoverScreen(
                                    prefs = prefs,
                                    providerStatuses = providerStatuses,
                                    discoveryTracks = discoveryTracks,
                                    discoveryErrors = discoveryErrors,
                                    isLoading = isLoadingDiscovery,
                                    currentTrackId = playbackState.currentTrack?.id,
                                    onRefreshDiscovery = viewModel::refreshDiscovery,
                                    onPlayTracks = { list, idx -> viewModel.playbackController.playTrackList(list, idx) },
                                    onToggleFavorite = viewModel::toggleFavoriteTrack,
                                    onPlayNext = { viewModel.playbackController.playNext(it) },
                                    onAddToQueue = { viewModel.playbackController.addToQueue(it) },
                                    onShowAudioInfo = openTrackInfo,
                                    onOpenDownloadFinder = { navController.navigate(RouteDownloadFinder) },
                                    onFindDownloadForTrack = openDownloadFinderFor,
                                    onOpenConnectedServices = { navigateTopLevel(RouteSettings) }
                                )
                            }

                            composable<RouteSettings> {
                                SettingsScreen(
                                    prefs = prefs,
                                    providerStatuses = providerStatuses,
                                    onOpenThemeStudio = { navController.navigate(RouteThemeStudio) },
                                    onOpenAudioLab = { navController.navigate(RouteAudioLab) },
                                    onOpenDownloadFinder = { navController.navigate(RouteDownloadFinder) },
                                    onSetLocalOnlyMode = viewModel::setLocalOnlyMode,
                                    onSetProviderEnabled = viewModel::setProviderEnabled,
                                    onSaveJamendoClientId = viewModel::setJamendoClientId,
                                    onDisconnectSpotify = viewModel::disconnectSpotify,
                                    onOpenInSpotify = viewModel::openInSpotify,
                                    onOpenInYouTubeMusic = viewModel::openInYouTubeMusic,
                                    onSetHaptics = { enabled -> scope.launch { viewModel.prefsRepo.setHapticsEnabled(enabled) } },
                                    onSetHistoryTracking = { enabled -> scope.launch { viewModel.prefsRepo.setHistoryTrackingEnabled(enabled) } },
                                    onSetOnlineLyrics = { enabled -> scope.launch { viewModel.prefsRepo.setOnlineLyricsEnabled(enabled) } },
                                    onClearHistory = viewModel::clearPlaybackHistory
                                )
                            }

                            composable<RouteNowPlaying> {
                                NowPlayingScreen(
                                    playbackState = playbackState,
                                    layoutMode = prefs.nowPlayingLayout,
                                    lyricsDoc = currentLyrics,
                                    hapticsEnabled = prefs.hapticsEnabled,
                                    onBack = { navController.popBackStack() },
                                    onSelectLayoutMode = viewModel::setNowPlayingLayout,
                                    onTogglePlayPause = { viewModel.playbackController.togglePlayPause() },
                                    onNext = { viewModel.playbackController.skipToNext() },
                                    onPrevious = { viewModel.playbackController.skipToPrevious() },
                                    onSeekTo = { viewModel.playbackController.seekTo(it) },
                                    onToggleShuffle = { viewModel.playbackController.toggleShuffle() },
                                    onCycleRepeat = { viewModel.playbackController.cycleRepeatMode() },
                                    onToggleFavorite = viewModel::toggleFavoriteTrack,
                                    onOpenQueue = { navController.navigate(RouteQueue) },
                                    onOpenLyrics = {
                                        playbackState.currentTrack?.let { t ->
                                            viewModel.loadLyricsForTrack(t)
                                            navController.navigate(RouteLyrics(t.id))
                                        }
                                    },
                                    onOpenAudioLab = { navController.navigate(RouteAudioLab) },
                                    onOpenAudioInfo = openTrackInfo,
                                    onOpenMetadataEditor = openMetadataEditor,
                                    onOpenDownloadFinder = openDownloadFinderFor,
                                    onSetSleepTimer = { mins -> viewModel.playbackController.startSleepTimer(mins) },
                                    onLoadLyrics = viewModel::loadLyricsForTrack
                                )
                            }

                            composable<RouteQueue> {
                                QueueScreen(
                                    playbackState = playbackState,
                                    onBack = { navController.popBackStack() },
                                    onJumpToIndex = { viewModel.playbackController.jumpToQueueIndex(it) },
                                    onMoveItem = { f, t -> viewModel.playbackController.moveQueueItem(f, t) },
                                    onRemoveItem = { viewModel.playbackController.removeFromQueue(it) },
                                    onClearQueue = { viewModel.playbackController.clearQueue() },
                                    onSaveQueueAsPlaylist = viewModel::saveQueueAsPlaylist
                                )
                            }

                            composable<RouteAudioLab> {
                                AudioLabScreen(
                                    settings = prefs.audioSettings,
                                    capabilities = effectCapabilities,
                                    onBack = { navController.popBackStack() },
                                    onUpdateSettings = viewModel::updateAudioSettings,
                                    onApplyEqPreset = viewModel::applyEqPreset
                                )
                            }

                            composable<RouteThemeStudio> {
                                ThemeStudioScreen(
                                    currentThemeId = prefs.themeId,
                                    customizations = prefs.themeCustomizations,
                                    onBack = { navController.popBackStack() },
                                    onSelectTheme = viewModel::selectTheme,
                                    onUpdateCustomizations = viewModel::updateThemeCustomizations
                                )
                            }

                            composable<RouteDownloadFinder> {
                                DownloadFinderScreen(
                                    state = downloadFinderState,
                                    isLocalOnlyMode = prefs.isLocalOnlyMode,
                                    onBack = { navController.popBackStack() },
                                    onSearch = viewModel::searchDownloadSources,
                                    onDownloadOrOpen = viewModel::downloadOrOpenSource,
                                    onConfirmUpgrade = viewModel::confirmReplaceWithBetterVersion
                                )
                            }

                            composable<RouteAudioInfo> { entry ->
                                val id = entry.arguments?.getString("trackId")
                                val track = libraryMap[id] ?: playbackState.currentTrack ?: inspectedTrackCache
                                if (track != null) {
                                    AudioInfoScreen(track = track, onBack = { navController.popBackStack() })
                                }
                            }

                            composable<RouteMetadataEditor> { entry ->
                                val id = entry.arguments?.getString("trackId")
                                val track = libraryMap[id] ?: playbackState.currentTrack ?: inspectedTrackCache
                                if (track != null) {
                                    MetadataEditorScreen(
                                        track = track,
                                        onBack = { navController.popBackStack() },
                                        onSave = { req -> viewModel.updateTrackMetadata(track, req) },
                                        onUndo = { viewModel.undoTrackMetadata(track.id) }
                                    )
                                }
                            }

                            composable<RouteLyrics> { entry ->
                                val id = entry.arguments?.getString("trackId")
                                val track = libraryMap[id] ?: playbackState.currentTrack ?: inspectedTrackCache
                                if (track != null) {
                                    LyricsScreen(
                                        track = track,
                                        lyricsDoc = currentLyrics,
                                        positionMs = playbackState.positionMs,
                                        onBack = { navController.popBackStack() },
                                        onImportLrc = { uri -> viewModel.importLrcForTrack(track.id, uri) },
                                        onAdjustOffset = viewModel::adjustLyricsOffset,
                                        onStampLineAtPlayhead = viewModel::updateLyricLineTimestamp,
                                        onSaveRawLyrics = { raw -> viewModel.savePlainOrLrcLyrics(track.id, raw) },
                                        onSeekTo = { ms -> viewModel.playbackController.seekTo(ms) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!prefs.onboardingCompleted) {
                ThemeOnboardingDialog(
                    onExploreThemes = {
                        viewModel.completeOnboarding()
                        navController.navigate(RouteThemeStudio)
                    },
                    onCreateOwn = {
                        viewModel.completeOnboarding()
                        navController.navigate(RouteThemeStudio)
                    },
                    onSkip = { viewModel.completeOnboarding() }
                )
            }
        }
    }
}
