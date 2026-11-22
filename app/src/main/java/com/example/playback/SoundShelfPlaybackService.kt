package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.SoundShelfApp
import com.example.core.model.MusicTrack
import com.example.core.model.RepeatMode
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@OptIn(UnstableApi::class)
class SoundShelfPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val app = application as SoundShelfApp

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true) // Handles Android audio focus & interruptions
            .setHandleAudioBecomingNoisy(true) // Auto-pauses when headphones/Bluetooth disconnect
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                serviceScope.launch {
                    val settings = app.preferencesRepository.preferencesFlow.first().audioSettings
                    app.audioEffectsManager.attachToSession(audioSessionId, settings)
                }
            }
        })

        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onPlaybackResumption(
                    mediaSession: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val saved = runBlocking(Dispatchers.IO) {
                        app.preferencesRepository.preferencesFlow.first().savedSession
                    }
                    val mediaItems = saved.queue.mapNotNull { entry ->
                        entry.track.toMediaItemOrNull()
                    }
                    if (mediaItems.isEmpty()) {
                        return Futures.immediateFailedFuture(IllegalStateException("No saved queue to resume"))
                    }
                    player.shuffleModeEnabled = saved.shuffleEnabled
                    player.repeatMode = when (saved.repeatMode) {
                        RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                        RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                        RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                    }
                    player.playbackParameters = PlaybackParameters(saved.playbackSpeed, 1.0f)
                    val idx = saved.currentIndex.coerceIn(0, mediaItems.lastIndex)
                    return Futures.immediateFuture(
                        MediaSession.MediaItemsWithStartPosition(
                            mediaItems,
                            idx,
                            saved.positionMs.coerceAtLeast(0L)
                        )
                    )
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        val app = application as? SoundShelfApp
        app?.audioEffectsManager?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}

fun MusicTrack.toMediaItemOrNull(resolvedStreamUrl: String? = null): MediaItem? {
    val uriString = resolvedStreamUrl ?: localContentUri ?: localFilePath ?: return null
    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setAlbumArtist(albumArtist)
        .setGenre(genre)
        .setTrackNumber(trackNumber.takeIf { it > 0 })
        .setDiscNumber(discNumber.takeIf { it > 0 })
        .setReleaseYear(year.takeIf { it > 0 })
        .setArtworkUri(artworkUri?.let { Uri.parse(it) })
        .build()

    return MediaItem.Builder()
        .setMediaId(id)
        .setUri(Uri.parse(uriString))
        .setMediaMetadata(metadata)
        .build()
}
