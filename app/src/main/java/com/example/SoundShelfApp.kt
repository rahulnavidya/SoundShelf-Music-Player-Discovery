package com.example

import android.app.Application
import com.example.downloadfinder.DownloadFinderRepository
import com.example.library.LocalMetadataEditor
import com.example.library.MediaStoreScanner
import com.example.library.PlaylistEngine
import com.example.lyrics.LyricsRepository
import com.example.persistence.SecureTokenVault
import com.example.persistence.SoundShelfDatabase
import com.example.persistence.UserPreferencesRepository
import com.example.playback.AudioEffectsManager
import com.example.playback.PlaybackController
import com.example.providers.ProviderManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class SoundShelfApp : Application() {

    val database: SoundShelfDatabase by lazy { SoundShelfDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val secureTokenVault: SecureTokenVault by lazy { SecureTokenVault(this) }
    val audioEffectsManager: AudioEffectsManager by lazy { AudioEffectsManager() }

    val providerManager: ProviderManager by lazy {
        ProviderManager(
            context = this,
            tokenVault = secureTokenVault,
            jamendoClientIdProvider = {
                runBlocking { preferencesRepository.preferencesFlow.first().jamendoClientId }
            }
        )
    }

    val mediaStoreScanner: MediaStoreScanner by lazy {
        MediaStoreScanner(
            context = this,
            trackDao = database.trackDao(),
            excludedFolderDao = database.excludedFolderDao()
        )
    }

    val metadataEditor: LocalMetadataEditor by lazy {
        LocalMetadataEditor(
            context = this,
            trackDao = database.trackDao(),
            backupDao = database.metadataBackupDao()
        )
    }

    val playlistEngine: PlaylistEngine by lazy {
        PlaylistEngine(playlistDao = database.playlistDao())
    }

    val lyricsRepository: LyricsRepository by lazy {
        LyricsRepository(context = this, lyricsDao = database.lyricsDao())
    }

    val downloadFinderRepository: DownloadFinderRepository by lazy {
        DownloadFinderRepository(
            context = this,
            providerManager = providerManager,
            trackDao = database.trackDao(),
            playlistDao = database.playlistDao(),
            historyDao = database.historyDao()
        )
    }

    val playbackController: PlaybackController by lazy {
        PlaybackController(
            context = this,
            preferencesRepository = preferencesRepository,
            providerManager = providerManager,
            trackDao = database.trackDao(),
            historyDao = database.historyDao(),
            audioEffectsManager = audioEffectsManager
        )
    }

    override fun onCreate() {
        super.onCreate()
        playbackController.connect()
    }
}
