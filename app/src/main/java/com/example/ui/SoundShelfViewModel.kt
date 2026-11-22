package com.example.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SoundShelfApp
import com.example.core.designsystem.NowPlayingLayoutMode
import com.example.core.designsystem.SoundShelfThemeId
import com.example.core.designsystem.ThemeCustomizations
import com.example.core.model.DownloadSourceResult
import com.example.core.model.LibraryCategory
import com.example.core.model.LyricsDocument
import com.example.core.model.MusicAlbum
import com.example.core.model.MusicArtist
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.core.model.SmartPlaylistRule
import com.example.core.model.TrackSortOption
import com.example.downloadfinder.DownloadFinderQuery
import com.example.downloadfinder.DownloadedFileVerification
import com.example.library.LibraryScanReport
import com.example.library.MetadataEditRequest
import com.example.lyrics.LrcParser
import com.example.persistence.AudioEngineSettings
import com.example.persistence.ExcludedFolderEntity
import com.example.persistence.HomeConfig
import com.example.persistence.M3uPlaylistCodec
import com.example.persistence.PlaybackHistoryEntity
import com.example.persistence.UserPreferencesState
import com.example.persistence.toDomain
import com.example.playback.AudioEffectCapabilities
import com.example.playback.PlaybackEvent
import com.example.playback.PlaybackUiState
import com.example.providers.OnlineSearchAggregate
import com.example.providers.ProviderResult
import com.example.providers.ProviderUiStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val selectedProviderFilter: ProviderId? = null,
    val localTracks: List<MusicTrack> = emptyList(),
    val localAlbums: List<MusicAlbum> = emptyList(),
    val localArtists: List<MusicArtist> = emptyList(),
    val onlineAggregates: List<OnlineSearchAggregate> = emptyList(),
    val isSearchingOnline: Boolean = false
)

data class DownloadFinderUiState(
    val query: DownloadFinderQuery = DownloadFinderQuery(title = "", artist = ""),
    val targetTrackToUpgrade: MusicTrack? = null,
    val results: List<DownloadSourceResult> = emptyList(),
    val isSearching: Boolean = false,
    val isDownloadingId: String? = null,
    val lastVerification: DownloadedFileVerification? = null,
    val statusMessage: String? = null
)

class SoundShelfViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as SoundShelfApp
    private val db = app.database
    val prefsRepo = app.preferencesRepository
    val playbackController = app.playbackController

    val preferences: StateFlow<UserPreferencesState> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserPreferencesState()
    )

    val localTracks: StateFlow<List<MusicTrack>> = db.trackDao().observeLocalTracks()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val playlists: StateFlow<List<MusicPlaylist>> = combine(
        db.playlistDao().observePlaylists(),
        db.playlistDao().observeAllCrossRefs()
    ) { playlistEntities, crossRefs ->
        val refsByPlaylist = crossRefs.groupBy { it.playlistId }
        playlistEntities.map { pe ->
            val trackIds = refsByPlaylist[pe.id]?.sortedBy { it.position }?.map { it.trackId } ?: emptyList()
            pe.toDomain(trackIds)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val history: StateFlow<List<PlaybackHistoryEntity>> = db.historyDao().observeRecentHistory()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = db.excludedFolderDao().observeExcludedFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val playbackState: StateFlow<PlaybackUiState> = playbackController.state
    val effectCapabilities: StateFlow<AudioEffectCapabilities> = app.audioEffectsManager.capabilities

    private val _scanReport = MutableStateFlow<LibraryScanReport?>(null)
    val scanReport: StateFlow<LibraryScanReport?> = _scanReport.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _libraryCategory = MutableStateFlow(LibraryCategory.SONGS)
    val libraryCategory: StateFlow<LibraryCategory> = _libraryCategory.asStateFlow()

    private val _sortOption = MutableStateFlow(TrackSortOption.TITLE)
    val sortOption: StateFlow<TrackSortOption> = _sortOption.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _discoveryTracks = MutableStateFlow<Map<ProviderId, List<MusicTrack>>>(emptyMap())
    val discoveryTracks: StateFlow<Map<ProviderId, List<MusicTrack>>> = _discoveryTracks.asStateFlow()

    private val _discoveryErrors = MutableStateFlow<Map<ProviderId, String>>(emptyMap())
    val discoveryErrors: StateFlow<Map<ProviderId, String>> = _discoveryErrors.asStateFlow()

    private val _isLoadingDiscovery = MutableStateFlow(false)
    val isLoadingDiscovery: StateFlow<Boolean> = _isLoadingDiscovery.asStateFlow()

    private val _providerStatuses = MutableStateFlow<List<ProviderUiStatus>>(emptyList())
    val providerStatuses: StateFlow<List<ProviderUiStatus>> = _providerStatuses.asStateFlow()

    private val _searchState = MutableStateFlow(SearchUiState())
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    private val _currentLyrics = MutableStateFlow<LyricsDocument?>(null)
    val currentLyrics: StateFlow<LyricsDocument?> = _currentLyrics.asStateFlow()

    private val _downloadFinderState = MutableStateFlow(DownloadFinderUiState())
    val downloadFinderState: StateFlow<DownloadFinderUiState> = _downloadFinderState.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    init {
        viewModelScope.launch {
            playbackController.events.collect { event ->
                when (event) {
                    is PlaybackEvent.Message -> _snackbarMessages.tryEmit(event.text)
                    is PlaybackEvent.OpenExternalProvider -> openTrackExternally(event.track)
                }
            }
        }
        viewModelScope.launch {
            preferences.collect { prefs ->
                refreshProviderStatuses(prefs)
            }
        }
        viewModelScope.launch {
            if (app.mediaStoreScanner.hasAudioReadPermission()) {
                scanLocalLibrary(forceDeepInspect = false)
            }
        }
    }

    fun scanLocalLibrary(forceDeepInspect: Boolean = false) {
        viewModelScope.launch {
            _isScanning.value = true
            val report = app.mediaStoreScanner.scanMediaStoreIncremental(forceDeepInspect)
            _scanReport.value = report
            _isScanning.value = false
            if (forceDeepInspect) {
                _snackbarMessages.tryEmit("Scan complete: ${report.totalTracks} tracks (${report.newTracksAdded} new)")
            }
        }
    }

    fun importSafAudioFile(uri: Uri) {
        viewModelScope.launch {
            val track = app.mediaStoreScanner.importSafAudioFile(uri)
            if (track != null) {
                _snackbarMessages.tryEmit("Imported \"${track.title}\" to SoundShelf")
            } else {
                _snackbarMessages.tryEmit("Unable to read audio file")
            }
        }
    }

    fun excludeFolder(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val label = path.substringAfterLast('/').ifBlank { path }
            db.excludedFolderDao().addExcludedFolder(ExcludedFolderEntity(folderPath = path, displayName = label))
            scanLocalLibrary(forceDeepInspect = false)
        }
    }

    fun removeExcludedFolder(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            db.excludedFolderDao().removeExcludedFolder(path)
            scanLocalLibrary(forceDeepInspect = false)
        }
    }

    fun setLibraryCategory(cat: LibraryCategory) {
        _libraryCategory.value = cat
    }

    fun setSortOption(option: TrackSortOption) {
        if (_sortOption.value == option) {
            _sortAscending.value = !_sortAscending.value
        } else {
            _sortOption.value = option
            _sortAscending.value = true
        }
    }

    fun sortedTracks(tracks: List<MusicTrack>): List<MusicTrack> {
        val comparator: Comparator<MusicTrack> = when (_sortOption.value) {
            TrackSortOption.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
            TrackSortOption.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
            TrackSortOption.ALBUM -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.album }
            TrackSortOption.DURATION -> compareBy { it.durationMs }
            TrackSortOption.DATE_ADDED -> compareBy { it.dateAddedSec }
            TrackSortOption.YEAR -> compareBy { it.year }
            TrackSortOption.FILE_SIZE -> compareBy { it.fileSizeBytes }
            TrackSortOption.PLAY_COUNT -> compareBy { it.playCount }
        }
        val sorted = tracks.sortedWith(comparator)
        return if (_sortAscending.value) sorted else sorted.reversed()
    }

    fun toggleFavoriteTrack(track: MusicTrack) {
        viewModelScope.launch(Dispatchers.IO) {
            if (track.providerId == ProviderId.LOCAL) {
                db.trackDao().setFavorite(track.id, !track.isFavorite)
            } else {
                // Save remote favorite reference locally without expiring URL assumptions
                val saved = track.copy(isFavorite = !track.isFavorite)
                db.trackDao().upsertTrack(com.example.persistence.TrackEntity(
                    id = saved.id,
                    providerId = saved.providerId.name,
                    providerTrackId = saved.providerTrackId,
                    title = saved.title,
                    artist = saved.artist,
                    artistId = saved.artistId,
                    album = saved.album,
                    albumId = saved.albumId,
                    albumArtist = saved.albumArtist,
                    genre = saved.genre,
                    trackNumber = saved.trackNumber,
                    discNumber = saved.discNumber,
                    year = saved.year,
                    durationMs = saved.durationMs,
                    artworkUri = saved.artworkUri,
                    localContentUri = saved.localContentUri,
                    localFilePath = saved.localFilePath,
                    folderPath = saved.folderPath,
                    dateAddedSec = saved.dateAddedSec,
                    fileSizeBytes = saved.fileSizeBytes,
                    playCount = saved.playCount,
                    lastPlayedMs = saved.lastPlayedMs,
                    isFavorite = saved.isFavorite,
                    rating = saved.rating,
                    format = saved.qualityInfo.format,
                    codec = saved.qualityInfo.codec,
                    bitrateKbps = saved.qualityInfo.bitrateKbps,
                    bitrateMode = saved.qualityInfo.bitrateMode,
                    sampleRateHz = saved.qualityInfo.sampleRateHz,
                    bitDepth = saved.qualityInfo.bitDepth,
                    channelCount = saved.qualityInfo.channelCount,
                    encoder = saved.qualityInfo.encoder,
                    qualitySourceKind = saved.qualityInfo.sourceKind.name,
                    isLossless = saved.qualityInfo.isLossless,
                    licenseName = saved.licenseName,
                    licenseUrl = saved.licenseUrl,
                    isDownloadPermitted = saved.isDownloadPermitted,
                    downloadUrl = saved.downloadUrl,
                    externalPageUrl = saved.externalPageUrl,
                    isrc = saved.isrc,
                    comment = saved.comment,
                    composer = saved.composer,
                    copyright = saved.copyright,
                    embeddedLyrics = saved.embeddedLyrics
                ))
            }
        }
    }

    fun setTrackRating(trackId: String, rating: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            db.trackDao().setRating(trackId, rating.coerceIn(0, 5))
        }
    }

    fun refreshDiscovery() {
        val prefs = preferences.value
        if (prefs.isLocalOnlyMode) {
            _discoveryTracks.value = emptyMap()
            _discoveryErrors.value = emptyMap()
            return
        }
        viewModelScope.launch {
            _isLoadingDiscovery.value = true
            val results = app.providerManager.fetchDiscoveryTracks(
                isLocalOnlyMode = prefs.isLocalOnlyMode,
                enabledProviders = prefs.enabledProviders
            )
            val tracksMap = mutableMapOf<ProviderId, List<MusicTrack>>()
            val errMap = mutableMapOf<ProviderId, String>()
            for ((providerId, result) in results) {
                when (result) {
                    is ProviderResult.Success -> tracksMap[providerId] = result.data
                    is ProviderResult.Error -> errMap[providerId] = result.message
                    is ProviderResult.Unsupported -> errMap[providerId] = result.reason
                }
            }
            _discoveryTracks.value = tracksMap
            _discoveryErrors.value = errMap
            _isLoadingDiscovery.value = false
        }
    }

    private fun refreshProviderStatuses(prefs: UserPreferencesState) {
        viewModelScope.launch {
            _providerStatuses.value = app.providerManager.getProviderStatuses(
                isLocalOnlyMode = prefs.isLocalOnlyMode,
                enabledProviders = prefs.enabledProviders
            )
        }
    }

    fun updateSearchQuery(query: String) {
        val local = localTracks.value
        val q = query.trim()
        val matchedTracks = if (q.isEmpty()) emptyList() else local.filter {
            it.title.contains(q, true) ||
                it.artist.contains(q, true) ||
                it.album.contains(q, true) ||
                it.albumArtist.contains(q, true) ||
                it.genre.contains(q, true) ||
                it.folderPath.contains(q, true) ||
                it.year.toString() == q
        }
        val matchedAlbums = matchedTracks.groupBy { it.albumId.ifBlank { it.album } }.map { (id, list) ->
            MusicAlbum(
                id = id,
                providerId = ProviderId.LOCAL,
                title = list.first().album,
                artist = list.first().albumArtist,
                year = list.first().year,
                artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                trackCount = list.size,
                totalDurationMs = list.sumOf { it.durationMs }
            )
        }
        val matchedArtists = matchedTracks.groupBy { it.artist }.map { (name, list) ->
            MusicArtist(
                id = "local_artist_$name",
                providerId = ProviderId.LOCAL,
                name = name,
                artworkUri = list.firstNotNullOfOrNull { it.artworkUri },
                trackCount = list.size,
                albumCount = list.map { it.album }.distinct().size
            )
        }
        _searchState.value = _searchState.value.copy(
            query = query,
            localTracks = matchedTracks,
            localAlbums = matchedAlbums,
            localArtists = matchedArtists
        )
    }

    fun setSearchProviderFilter(providerId: ProviderId?) {
        _searchState.value = _searchState.value.copy(selectedProviderFilter = providerId)
    }

    fun executeOnlineSearch() {
        val q = _searchState.value.query.trim()
        if (q.isEmpty()) return
        val prefs = preferences.value
        viewModelScope.launch {
            prefsRepo.addRecentSearch(q)
            if (prefs.isLocalOnlyMode) {
                _snackbarMessages.tryEmit("Local-Only Mode is active. Showing local search results only.")
                return@launch
            }
            _searchState.value = _searchState.value.copy(isSearchingOnline = true)
            val aggregates = app.providerManager.searchOnlineProviders(
                query = q,
                isLocalOnlyMode = prefs.isLocalOnlyMode,
                enabledProviders = prefs.enabledProviders,
                filterProvider = _searchState.value.selectedProviderFilter
            )
            _searchState.value = _searchState.value.copy(
                onlineAggregates = aggregates,
                isSearchingOnline = false
            )
        }
    }

    // Playlist operations
    fun createPlaylist(name: String, description: String = "", tracks: List<MusicTrack> = emptyList(), smartRule: SmartPlaylistRule? = null) {
        viewModelScope.launch {
            app.playlistEngine.createPlaylist(name, description, tracks, smartRule)
            _snackbarMessages.tryEmit("Created playlist \"$name\"")
        }
    }

    fun saveQueueAsPlaylist(name: String) {
        val tracks = playbackState.value.queue.map { it.track }
        if (tracks.isEmpty()) return
        createPlaylist(name, description = "Saved from Playback Queue", tracks = tracks)
    }

    fun renamePlaylist(playlist: MusicPlaylist, newName: String) {
        viewModelScope.launch { app.playlistEngine.renamePlaylist(playlist, newName) }
    }

    fun duplicatePlaylist(playlist: MusicPlaylist) {
        viewModelScope.launch {
            app.playlistEngine.duplicatePlaylist(playlist)
            _snackbarMessages.tryEmit("Duplicated \"${playlist.name}\"")
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch { app.playlistEngine.deletePlaylist(playlistId) }
    }

    fun toggleFavoritePlaylist(playlist: MusicPlaylist) {
        viewModelScope.launch { app.playlistEngine.toggleFavoritePlaylist(playlist) }
    }

    fun addTrackToPlaylist(playlist: MusicPlaylist, track: MusicTrack) {
        viewModelScope.launch {
            app.playlistEngine.addTrackToPlaylist(playlist, track)
            _snackbarMessages.tryEmit("Added to \"${playlist.name}\"")
        }
    }

    fun removeTrackFromPlaylist(playlist: MusicPlaylist, trackId: String) {
        viewModelScope.launch { app.playlistEngine.removeTrackFromPlaylist(playlist, trackId) }
    }

    fun moveTrackInPlaylist(playlist: MusicPlaylist, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { app.playlistEngine.moveTrackInPlaylist(playlist, fromIndex, toIndex) }
    }

    fun exportPlaylistM3u8(playlist: MusicPlaylist, targetUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val libraryMap = localTracks.value.associateBy { it.id }
            val resolved = resolvePlaylistTracks(playlist, libraryMap)
            val content = M3uPlaylistCodec.exportToM3u8(playlist.name, resolved)
            runCatching {
                app.contentResolver.openOutputStream(targetUri)?.use { out ->
                    out.write(content.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                _snackbarMessages.tryEmit("Exported \"${playlist.name}.m3u8\"")
            }.onFailure {
                _snackbarMessages.tryEmit("Export failed: ${it.localizedMessage}")
            }
        }
    }

    fun importM3uUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val content = runCatching {
                app.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (content.isNullOrBlank()) {
                _snackbarMessages.tryEmit("Unable to read playlist file")
                return@launch
            }
            val imported = app.playlistEngine.importM3uPlaylist(content, "Imported Playlist", localTracks.value)
            _snackbarMessages.tryEmit("Imported \"${imported.name}\" (${imported.trackIds.size} matched tracks)")
        }
    }

    fun resolvePlaylistTracks(playlist: MusicPlaylist, libraryMap: Map<String, MusicTrack>): List<MusicTrack> {
        return if (playlist.isSmart && playlist.smartRule != null) {
            app.playlistEngine.evaluateSmartPlaylist(playlist.smartRule, localTracks.value)
        } else {
            playlist.trackIds.mapNotNull { libraryMap[it] }
        }
    }

    // Metadata Editor
    fun updateTrackMetadata(track: MusicTrack, request: MetadataEditRequest) {
        viewModelScope.launch {
            app.metadataEditor.updateSingleTrack(track, request)
                .onSuccess { _snackbarMessages.tryEmit("Updated metadata for \"${request.title}\"") }
                .onFailure { _snackbarMessages.tryEmit("Metadata update failed") }
        }
    }

    fun undoTrackMetadata(trackId: String) {
        viewModelScope.launch {
            if (app.metadataEditor.undoLastEdit(trackId)) {
                _snackbarMessages.tryEmit("Restored previous metadata snapshot")
            }
        }
    }

    // Lyrics Studio
    fun loadLyricsForTrack(track: MusicTrack) {
        viewModelScope.launch {
            val prefs = preferences.value
            val allowOnline = !prefs.isLocalOnlyMode && prefs.onlineLyricsEnabled
            _currentLyrics.value = app.lyricsRepository.loadLyrics(track, allowOnline)
        }
    }

    fun importLrcForTrack(trackId: String, uri: Uri) {
        viewModelScope.launch {
            app.lyricsRepository.importLrcFromUri(trackId, uri)
                .onSuccess {
                    _currentLyrics.value = it
                    _snackbarMessages.tryEmit("Imported synchronized .lrc file")
                }
                .onFailure { _snackbarMessages.tryEmit("Failed to parse .lrc file") }
        }
    }

    fun adjustLyricsOffset(deltaMs: Long) {
        val doc = _currentLyrics.value ?: return
        viewModelScope.launch {
            _currentLyrics.value = app.lyricsRepository.updateOffset(doc, doc.offsetMs + deltaMs)
        }
    }

    fun updateLyricLineTimestamp(index: Int, newTimestampMs: Long) {
        val doc = _currentLyrics.value ?: return
        viewModelScope.launch {
            _currentLyrics.value = app.lyricsRepository.updateLineTimestamp(doc, index, newTimestampMs)
        }
    }

    fun savePlainOrLrcLyrics(trackId: String, rawText: String) {
        viewModelScope.launch {
            val parsed = LrcParser.parse(trackId, rawText, source = "Edited in Lyrics Studio")
            app.lyricsRepository.saveEditedLyrics(parsed)
            _currentLyrics.value = parsed
            _snackbarMessages.tryEmit("Saved lyrics")
        }
    }

    // Download Finder
    fun prepareDownloadFinderForTrack(track: MusicTrack) {
        val version = app.downloadFinderRepository.detectVersionTag(track.title)
        val q = DownloadFinderQuery(
            title = track.title,
            artist = track.artist,
            album = if (track.album == "Unknown Album") "" else track.album,
            year = track.year,
            version = version,
            isrc = track.isrc ?: ""
        )
        _downloadFinderState.value = DownloadFinderUiState(
            query = q,
            targetTrackToUpgrade = if (track.providerId == ProviderId.LOCAL) track else null
        )
        searchDownloadSources(q)
    }

    fun searchDownloadSources(query: DownloadFinderQuery) {
        val prefs = preferences.value
        viewModelScope.launch {
            _downloadFinderState.value = _downloadFinderState.value.copy(
                query = query,
                isSearching = true,
                statusMessage = null
            )
            val results = app.downloadFinderRepository.searchLegitimateSources(
                query = query,
                isLocalOnlyMode = prefs.isLocalOnlyMode,
                enabledProviders = prefs.enabledProviders
            )
            _downloadFinderState.value = _downloadFinderState.value.copy(
                results = results,
                isSearching = false
            )
        }
    }

    fun downloadOrOpenSource(source: DownloadSourceResult) {
        if (!source.isDirectDownload) {
            openUrlInBrowser(source.url)
            return
        }
        viewModelScope.launch {
            _downloadFinderState.value = _downloadFinderState.value.copy(isDownloadingId = source.id)
            app.downloadFinderRepository.downloadAuthorizedCopyAndImport(source)
                .onSuccess { verification ->
                    _downloadFinderState.value = _downloadFinderState.value.copy(
                        isDownloadingId = null,
                        lastVerification = verification,
                        statusMessage = "Downloaded & verified (${verification.importedTrack.qualityInfo.summaryBadge()})"
                    )
                    _snackbarMessages.tryEmit("Imported \"${verification.importedTrack.title}\" to Downloads")
                }
                .onFailure { err ->
                    _downloadFinderState.value = _downloadFinderState.value.copy(
                        isDownloadingId = null,
                        statusMessage = err.localizedMessage ?: "Download failed"
                    )
                }
        }
    }

    fun confirmReplaceWithBetterVersion() {
        val state = _downloadFinderState.value
        val oldTrack = state.targetTrackToUpgrade ?: return
        val newTrack = state.lastVerification?.importedTrack ?: return
        viewModelScope.launch {
            app.downloadFinderRepository.replaceTrackPreservingAssociations(oldTrack, newTrack)
            _downloadFinderState.value = state.copy(
                targetTrackToUpgrade = null,
                statusMessage = "Replaced library reference with higher-quality copy while preserving playlists & play history. Original file was kept untouched."
            )
            _snackbarMessages.tryEmit("Library reference upgraded safely")
        }
    }

    // Audio, Theme, Provider & Privacy Settings
    fun updateAudioSettings(transform: (AudioEngineSettings) -> AudioEngineSettings) {
        viewModelScope.launch {
            val updated = transform(preferences.value.audioSettings)
            prefsRepo.updateAudioSettings(updated)
            playbackController.applyAudioSettings(updated)
        }
    }

    fun applyEqPreset(presetIndex: Int) {
        val bands = app.audioEffectsManager.getPresetBandLevels(presetIndex) ?: return
        updateAudioSettings { it.copy(equalizerEnabled = true, equalizerBandsMb = bands) }
    }

    fun selectTheme(id: SoundShelfThemeId) {
        viewModelScope.launch { prefsRepo.setTheme(id) }
    }

    fun updateThemeCustomizations(transform: (ThemeCustomizations) -> ThemeCustomizations) {
        viewModelScope.launch {
            prefsRepo.updateThemeCustomizations(transform(preferences.value.themeCustomizations))
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { prefsRepo.completeOnboarding() }
    }

    fun setNowPlayingLayout(mode: NowPlayingLayoutMode) {
        viewModelScope.launch { prefsRepo.setNowPlayingLayout(mode) }
    }

    fun updateHomeConfig(transform: (HomeConfig) -> HomeConfig) {
        viewModelScope.launch { prefsRepo.updateHomeConfig(transform(preferences.value.homeConfig)) }
    }

    fun setLocalOnlyMode(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setLocalOnlyMode(enabled)
            if (enabled) {
                _discoveryTracks.value = emptyMap()
                _discoveryErrors.value = emptyMap()
            } else {
                refreshDiscovery()
            }
        }
    }

    fun setProviderEnabled(providerId: ProviderId, enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setProviderEnabled(providerId, enabled) }
    }

    fun setJamendoClientId(clientId: String) {
        viewModelScope.launch {
            prefsRepo.setJamendoClientId(clientId)
            refreshProviderStatuses(preferences.value.copy(jamendoClientId = clientId))
        }
    }

    fun disconnectSpotify() {
        app.providerManager.spotify.disconnectAccount()
        refreshProviderStatuses(preferences.value)
        _snackbarMessages.tryEmit("Disconnected Spotify session")
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db.historyDao().clearHistory()
            _snackbarMessages.tryEmit("Listening history cleared")
        }
    }

    fun openTrackExternally(track: MusicTrack) {
        when (track.providerId) {
            ProviderId.SPOTIFY -> {
                val target = track.externalPageUrl ?: "${track.artist} ${track.title}"
                if (!app.providerManager.spotify.openInSpotify(target)) {
                    _snackbarMessages.tryEmit("Could not open Spotify")
                }
            }
            ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL -> {
                val target = track.externalPageUrl ?: "${track.artist} ${track.title}"
                if (!app.providerManager.ytMusicExperimental.openInOfficialYouTubeMusic(target)) {
                    _snackbarMessages.tryEmit("Could not open YouTube Music")
                }
            }
            else -> {
                track.externalPageUrl?.let { openUrlInBrowser(it) }
            }
        }
    }

    fun openInYouTubeMusic(queryOrUrl: String) {
        app.providerManager.ytMusicExperimental.openInOfficialYouTubeMusic(queryOrUrl)
    }

    fun openInSpotify(queryOrUrl: String) {
        app.providerManager.spotify.openInSpotify(queryOrUrl)
    }

    fun openUrlInBrowser(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        }
    }
}
