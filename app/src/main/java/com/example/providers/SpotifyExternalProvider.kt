package com.example.providers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderCapability
import com.example.core.model.ProviderId
import com.example.persistence.SecureTokenVault

/**
 * Optional Spotify external integration & OAuth PKCE metadata module.
 * Strict policy guardrails:
 * - Spotify streams are NEVER mixed into SoundShelf's cross-provider audio queue.
 * - Never claims to offer Spotify Premium features to Free users.
 * - Playback opens directly in the official Spotify application.
 */
class SpotifyExternalProvider(
    private val context: Context,
    private val tokenVault: SecureTokenVault
) : MusicProvider {

    companion object {
        const val SPOTIFY_PACKAGE = "com.spotify.music"
    }

    override val descriptor = ProviderDescriptor(
        id = ProviderId.SPOTIFY,
        displayName = "Spotify (External Handoff)",
        description = "Optional Spotify companion integration for opening tracks, artists, albums, and playlists in the official Spotify app.",
        capabilities = setOf(
            ProviderCapability.EXTERNAL_HANDOFF,
            ProviderCapability.ACCOUNT_CONNECTION
        ),
        authType = ProviderAuthType.OAUTH_PKCE,
        privacyNotice = "OAuth tokens (if connected) are encrypted via Android Keystore. SoundShelf never asks for your Spotify password.",
        restrictionNotice = "Spotify streams cannot be mixed into SoundShelf's local/Audius queue. Unsupported playback launches the official Spotify app."
    )

    fun isSpotifyInstalled(): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            true
        }.recoverCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(SPOTIFY_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    override suspend fun getStatus(): ProviderConnectionStatus {
        return when {
            tokenVault.hasActiveToken(ProviderId.SPOTIFY) -> ProviderConnectionStatus.CONNECTED
            isSpotifyInstalled() -> ProviderConnectionStatus.EXTERNAL_APP_INSTALLED
            else -> ProviderConnectionStatus.UNSUPPORTED_PLAYBACK
        }
    }

    override suspend fun searchTracks(query: String, limit: Int): ProviderResult<List<MusicTrack>> {
        return ProviderResult.Unsupported(
            "Spotify audio streams remain inside Spotify. Tap 'Open in Spotify' to search \"$query\" directly in the Spotify app."
        )
    }

    fun openInSpotify(queryOrUri: String): Boolean {
        val uri = when {
            queryOrUri.startsWith("spotify:") || queryOrUri.startsWith("http") -> Uri.parse(queryOrUri)
            else -> Uri.parse("https://open.spotify.com/search/${Uri.encode(queryOrUri)}")
        }
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isSpotifyInstalled()) {
                setPackage(SPOTIFY_PACKAGE)
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }

    fun disconnectAccount() {
        tokenVault.clearToken(ProviderId.SPOTIFY)
    }
}
