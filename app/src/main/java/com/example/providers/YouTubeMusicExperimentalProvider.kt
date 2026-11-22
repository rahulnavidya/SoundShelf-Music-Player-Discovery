package com.example.providers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderCapability
import com.example.core.model.ProviderId

/**
 * Isolated experimental compatibility module for YouTube Music.
 * Strict policy guardrails:
 * - Does NOT collect Google passwords or browser session cookies.
 * - Does NOT bypass DRM, Premium entitlements, or advertisements.
 * - Does NOT extract unauthorized audio streams or offer unauthorized offline downloads.
 * - Hands off playback/search cleanly to the official YouTube Music app or web portal.
 */
class YouTubeMusicExperimentalProvider(
    private val context: Context
) : MusicProvider {

    companion object {
        const val YT_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
    }

    override val descriptor = ProviderDescriptor(
        id = ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL,
        displayName = "YouTube Music (Experimental)",
        description = "Isolated compatibility module evaluating YouTube Music integration patterns without bypassing DRM, ads, or authentication policies.",
        capabilities = setOf(
            ProviderCapability.EXTERNAL_HANDOFF
        ),
        authType = ProviderAuthType.EXTERNAL_APP_HANDOFF,
        privacyNotice = "SoundShelf never asks for your Google password or collects session cookies. Requests open the official YouTube Music app.",
        restrictionNotice = "YouTube Music does not provide a public third-party audio streaming API. Direct in-app stream extraction and offline downloads are disabled by policy."
    )

    fun isOfficialAppInstalled(): Boolean {
        return runCatching {
            context.packageManager.getPackageInfo(YT_MUSIC_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            true
        }.recoverCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(YT_MUSIC_PACKAGE, 0)
            true
        }.getOrDefault(false)
    }

    override suspend fun getStatus(): ProviderConnectionStatus {
        return if (isOfficialAppInstalled()) {
            ProviderConnectionStatus.EXTERNAL_APP_INSTALLED
        } else {
            ProviderConnectionStatus.EXPERIMENTAL
        }
    }

    override suspend fun searchTracks(query: String, limit: Int): ProviderResult<List<MusicTrack>> {
        return ProviderResult.Unsupported(
            "Direct unauthenticated stream indexing is disabled to comply with YouTube Music terms. Use 'Open in YouTube Music' to search \"$query\" in the official app."
        )
    }

    fun openInOfficialYouTubeMusic(queryOrUrl: String): Boolean {
        val targetUrl = if (queryOrUrl.startsWith("http")) {
            queryOrUrl
        } else {
            "https://music.youtube.com/search?q=${Uri.encode(queryOrUrl)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isOfficialAppInstalled()) {
                setPackage(YT_MUSIC_PACKAGE)
            }
        }
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
