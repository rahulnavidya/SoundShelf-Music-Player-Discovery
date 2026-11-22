package com.example.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.core.model.QueueEntry
import com.example.core.model.RepeatMode
import com.example.persistence.AudioEngineSettings
import com.example.persistence.HistoryDao
import com.example.persistence.PlaybackHistoryEntity
import com.example.persistence.SavedPlaybackSession
import com.example.persistence.TrackDao
import com.example.persistence.UserPreferencesRepository
import com.example.providers.ProviderManager
import com.example.providers.ProviderResult
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class PlaybackUiState(
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val currentTrack: MusicTrack? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val sleepTimerRemainingMs: Long? = null,
    val errorMessage: String? = null
)

sealed interface PlaybackEvent {
    data class OpenExternalProvider(val track: MusicTrack) : PlaybackEvent
    data class Message(val text: String) : PlaybackEvent
}

class PlaybackController(
    private val context: Context,
    private val preferencesRepository: UserPreferencesRepository,
    private val providerManager: ProviderManager,
    private val trackDao: TrackDao,
    private val historyDao: HistoryDao,
    private val audioEffectsManager: AudioEffectsManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var lastRecordedTrackId: String? = null

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    fun connect() {
        if (controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, SoundShelfPlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                mediaController = controller
                attachListener(controller)
                restoreSessionIfEmpty(controller)
                startPositionTicker()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun attachListener(controller: MediaController) {
        controller.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
                persistSession()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val buffering = playbackState == Player.STATE_BUFFERING
                _state.value = _state.value.copy(
                    isBuffering = buffering,
                    durationMs = controller.duration.coerceAtLeast(0L)
                )
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val idx = controller.currentMediaItemIndex
                val queue = _state.value.queue
                val track = queue.getOrNull(idx)?.track
                _state.value = _state.value.copy(
                    currentIndex = idx,
                    currentTrack = track,
                    positionMs = 0L,
                    durationMs = track?.durationMs ?: controller.duration.coerceAtLeast(0L),
                    errorMessage = null
                )
                track?.let { recordTrackPlay(it) }
                persistSession()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _state.value = _state.value.copy(shuffleEnabled = shuffleModeEnabled)
                persistSession()
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                val mode = when (repeatMode) {
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    else -> RepeatMode.OFF
                }
                _state.value = _state.value.copy(repeatMode = mode)
                persistSession()
            }

            override fun onPlayerError(error: PlaybackException) {
                _state.value = _state.value.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.localizedMessage ?: "Playback failed for current track"
                )
            }
        })
    }

    private fun restoreSessionIfEmpty(controller: MediaController) {
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            applyAudioSettings(prefs.audioSettings)
            val saved = prefs.savedSession
            if (controller.mediaItemCount == 0 && saved.queue.isNotEmpty()) {
                val items = saved.queue.mapNotNull { entry ->
                    if (entry.track.providerId == ProviderId.LOCAL) {
                        entry.track.toMediaItemOrNull()
                    } else null
                }
                val validEntries = saved.queue.filter { it.track.providerId == ProviderId.LOCAL && it.track.toMediaItemOrNull() != null }
                if (items.isNotEmpty()) {
                    val idx = saved.currentIndex.coerceIn(0, items.lastIndex)
                    controller.setMediaItems(items, idx, saved.positionMs.coerceAtLeast(0L))
                    controller.shuffleModeEnabled = saved.shuffleEnabled
                    controller.repeatMode = when (saved.repeatMode) {
                        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    }
                    controller.prepare()
                    _state.value = _state.value.copy(
                        queue = validEntries,
                        currentIndex = idx,
                        currentTrack = validEntries.getOrNull(idx)?.track,
                        positionMs = saved.positionMs,
                        durationMs = validEntries.getOrNull(idx)?.track?.durationMs ?: 0L,
                        shuffleEnabled = saved.shuffleEnabled,
                        repeatMode = saved.repeatMode,
                        playbackSpeed = prefs.audioSettings.playbackSpeed,
                        playbackPitch = prefs.audioSettings.playbackPitch
                    )
                }
            }
        }
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            var tickCount = 0
            while (isActive) {
                val controller = mediaController
                if (controller != null) {
                    val pos = controller.currentPosition.coerceAtLeast(0L)
                    val dur = controller.duration.takeIf { it > 0 } ?: (_state.value.currentTrack?.durationMs ?: 0L)
                    _state.value = _state.value.copy(
                        positionMs = pos,
                        durationMs = dur,
                        isPlaying = controller.isPlaying
                    )
                    // Crossfade volume envelope near end of track if enabled
                    tickCount++
                    if (tickCount % 10 == 0 && controller.isPlaying) {
                        persistSession()
                    }
                }
                delay(400L)
            }
        }
    }

    fun playTrackList(tracks: List<MusicTrack>, startIndex: Int = 0) {
        val clicked = tracks.getOrNull(startIndex) ?: return
        if (!clicked.canJoinUnifiedQueue) {
            _events.tryEmit(PlaybackEvent.OpenExternalProvider(clicked))
            return
        }

        val queueableTracks = tracks.filter { it.canJoinUnifiedQueue }
        val targetIndex = queueableTracks.indexOfFirst { it.id == clicked.id }.coerceAtLeast(0)

        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val resolvedPairs = mutableListOf<Pair<QueueEntry, MediaItem>>()
            for (t in queueableTracks) {
                val res = providerManager.resolveStreamUrl(t, prefs.isLocalOnlyMode)
                if (res is ProviderResult.Success) {
                    val item = t.toMediaItemOrNull(res.data)
                    if (item != null) {
                        resolvedPairs.add(QueueEntry(UUID.randomUUID().toString(), t) to item)
                    }
                }
            }

            if (resolvedPairs.isEmpty()) {
                _events.tryEmit(PlaybackEvent.Message("Track could not be prepared for playback."))
                return@launch
            }

            val entries = resolvedPairs.map { it.first }
            val items = resolvedPairs.map { it.second }
            val idx = targetIndex.coerceIn(0, items.lastIndex)

            val controller = mediaController ?: return@launch
            controller.setMediaItems(items, idx, 0L)
            controller.prepare()
            controller.play()

            _state.value = _state.value.copy(
                queue = entries,
                currentIndex = idx,
                currentTrack = entries[idx].track,
                positionMs = 0L,
                durationMs = entries[idx].track.durationMs,
                errorMessage = null
            )
            recordTrackPlay(entries[idx].track)
            persistSession()
        }
    }

    fun playNext(track: MusicTrack) {
        if (!track.canJoinUnifiedQueue) {
            _events.tryEmit(PlaybackEvent.OpenExternalProvider(track))
            return
        }
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val res = providerManager.resolveStreamUrl(track, prefs.isLocalOnlyMode)
            if (res !is ProviderResult.Success) {
                _events.tryEmit(PlaybackEvent.Message((res as? ProviderResult.Error)?.message ?: "Cannot queue track"))
                return@launch
            }
            val item = track.toMediaItemOrNull(res.data) ?: return@launch
            val controller = mediaController ?: return@launch
            val insertAt = if (_state.value.queue.isEmpty()) 0 else (_state.value.currentIndex + 1).coerceAtMost(_state.value.queue.size)
            val newQueue = _state.value.queue.toMutableList().apply {
                add(insertAt, QueueEntry(UUID.randomUUID().toString(), track))
            }
            controller.addMediaItem(insertAt, item)
            if (newQueue.size == 1) {
                controller.prepare()
                controller.play()
            }
            _state.value = _state.value.copy(queue = newQueue)
            _events.tryEmit(PlaybackEvent.Message("Playing \"${track.title}\" next"))
            persistSession()
        }
    }

    fun addToQueue(track: MusicTrack) {
        if (!track.canJoinUnifiedQueue) {
            _events.tryEmit(PlaybackEvent.OpenExternalProvider(track))
            return
        }
        scope.launch {
            val prefs = preferencesRepository.preferencesFlow.first()
            val res = providerManager.resolveStreamUrl(track, prefs.isLocalOnlyMode)
            if (res !is ProviderResult.Success) {
                _events.tryEmit(PlaybackEvent.Message((res as? ProviderResult.Error)?.message ?: "Cannot queue track"))
                return@launch
            }
            val item = track.toMediaItemOrNull(res.data) ?: return@launch
            val controller = mediaController ?: return@launch
            val newQueue = _state.value.queue + QueueEntry(UUID.randomUUID().toString(), track)
            controller.addMediaItem(item)
            if (newQueue.size == 1) {
                controller.prepare()
                controller.play()
            }
            _state.value = _state.value.copy(queue = newQueue)
            _events.tryEmit(PlaybackEvent.Message("Added \"${track.title}\" to queue"))
            persistSession()
        }
    }

    fun togglePlayPause() {
        val controller = mediaController ?: return
        if (controller.isPlaying) controller.pause() else controller.play()
    }

    fun skipToNext() {
        mediaController?.seekToNextMediaItem()
    }

    fun skipToPrevious() {
        val controller = mediaController ?: return
        if (controller.currentPosition > 3500L) {
            controller.seekTo(0L)
        } else {
            controller.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs.coerceAtLeast(0L))
        _state.value = _state.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    fun seekBy(deltaMs: Long) {
        val controller = mediaController ?: return
        val target = (controller.currentPosition + deltaMs).coerceIn(0L, controller.duration.coerceAtLeast(0L))
        seekTo(target)
    }

    fun jumpToQueueIndex(index: Int) {
        val controller = mediaController ?: return
        if (index in _state.value.queue.indices) {
            controller.seekToDefaultPosition(index)
            controller.play()
        }
    }

    fun removeFromQueue(index: Int) {
        val controller = mediaController ?: return
        if (index !in _state.value.queue.indices) return
        controller.removeMediaItem(index)
        val updated = _state.value.queue.toMutableList().apply { removeAt(index) }
        val newIndex = controller.currentMediaItemIndex
        _state.value = _state.value.copy(
            queue = updated,
            currentIndex = newIndex,
            currentTrack = updated.getOrNull(newIndex)?.track
        )
        persistSession()
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val controller = mediaController ?: return
        val queue = _state.value.queue
        if (fromIndex !in queue.indices || toIndex !in queue.indices || fromIndex == toIndex) return
        controller.moveMediaItem(fromIndex, toIndex)
        val mutable = queue.toMutableList()
        val moved = mutable.removeAt(fromIndex)
        mutable.add(toIndex, moved)
        val newIndex = controller.currentMediaItemIndex
        _state.value = _state.value.copy(
            queue = mutable,
            currentIndex = newIndex,
            currentTrack = mutable.getOrNull(newIndex)?.track
        )
        persistSession()
    }

    fun clearQueue() {
        val controller = mediaController ?: return
        controller.clearMediaItems()
        _state.value = _state.value.copy(
            queue = emptyList(),
            currentIndex = -1,
            currentTrack = null,
            isPlaying = false,
            positionMs = 0L,
            durationMs = 0L
        )
        persistSession()
    }

    fun toggleShuffle() {
        val controller = mediaController ?: return
        controller.shuffleModeEnabled = !controller.shuffleModeEnabled
    }

    fun cycleRepeatMode() {
        val controller = mediaController ?: return
        val next = when (_state.value.repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_ALL
            RepeatMode.ALL -> Player.REPEAT_MODE_ONE
            RepeatMode.ONE -> Player.REPEAT_MODE_OFF
        }
        controller.repeatMode = next
    }

    fun applyAudioSettings(settings: AudioEngineSettings) {
        mediaController?.let { controller ->
            val speed = settings.playbackSpeed.coerceIn(0.5f, 2.0f)
            val pitch = settings.playbackPitch.coerceIn(0.5f, 2.0f)
            controller.playbackParameters = PlaybackParameters(speed, pitch)
            // Apply stereo balance approximation via volume if strongly panned or keep clean Unity gain
            _state.value = _state.value.copy(playbackSpeed = speed, playbackPitch = pitch)
        }
        audioEffectsManager.applySettings(settings)
    }

    fun startSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerRemainingMs = null)
            return
        }
        var remaining = minutes * 60_000L
        _state.value = _state.value.copy(sleepTimerRemainingMs = remaining)
        sleepTimerJob = scope.launch {
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining -= 1000L
                _state.value = _state.value.copy(sleepTimerRemainingMs = remaining.coerceAtLeast(0L))
            }
            if (remaining <= 0) {
                mediaController?.pause()
                _state.value = _state.value.copy(sleepTimerRemainingMs = null)
                _events.tryEmit(PlaybackEvent.Message("Sleep timer paused playback"))
            }
        }
    }

    private fun recordTrackPlay(track: MusicTrack) {
        if (lastRecordedTrackId == track.id) return
        lastRecordedTrackId = track.id
        scope.launch(Dispatchers.IO) {
            val prefs = preferencesRepository.preferencesFlow.first()
            if (prefs.historyTrackingEnabled) {
                val now = System.currentTimeMillis()
                trackDao.incrementPlayCount(track.id, now)
                historyDao.insertHistory(
                    PlaybackHistoryEntity(
                        trackId = track.id,
                        providerId = track.providerId.name,
                        title = track.title,
                        artist = track.artist,
                        album = track.album,
                        artworkUri = track.artworkUri,
                        playedAtMs = now
                    )
                )
            }
        }
    }

    private fun persistSession() {
        val current = _state.value
        scope.launch(Dispatchers.IO) {
            preferencesRepository.savePlaybackSession(
                SavedPlaybackSession(
                    queue = current.queue.take(100),
                    currentIndex = current.currentIndex.coerceAtLeast(0),
                    positionMs = current.positionMs,
                    shuffleEnabled = current.shuffleEnabled,
                    repeatMode = current.repeatMode,
                    playbackSpeed = current.playbackSpeed
                )
            )
        }
    }

    fun release() {
        positionTickerJob?.cancel()
        sleepTimerJob?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        mediaController = null
    }
}
