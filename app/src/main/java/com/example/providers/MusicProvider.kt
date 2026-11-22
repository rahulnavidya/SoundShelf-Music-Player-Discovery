package com.example.providers

import com.example.core.model.MusicAlbum
import com.example.core.model.MusicArtist
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderCapability
import com.example.core.model.ProviderId

enum class ProviderAuthType(val label: String) {
    NONE("No Account Required"),
    CLIENT_KEY("Public App Client ID"),
    OAUTH_PKCE("OAuth 2.0 + PKCE (Optional)"),
    EXTERNAL_APP_HANDOFF("External App Handoff")
}

enum class ProviderConnectionStatus(val label: String) {
    AVAILABLE("Available"),
    CONNECTED("Connected"),
    UNAUTHENTICATED_BROWSING("Unauthenticated Browsing"),
    EXTERNAL_APP_INSTALLED("External App Installed"),
    EXPERIMENTAL("Experimental (Opt-in)"),
    REQUIRES_CONFIG("Requires Client ID"),
    DISABLED("Disabled"),
    UNSUPPORTED_PLAYBACK("External Playback Only")
}

data class ProviderDescriptor(
    val id: ProviderId,
    val displayName: String,
    val description: String,
    val capabilities: Set<ProviderCapability>,
    val authType: ProviderAuthType,
    val privacyNotice: String,
    val restrictionNotice: String
)

sealed interface ProviderResult<out T> {
    data class Success<T>(val data: T) : ProviderResult<T>
    data class Error(
        val message: String,
        val isNetworkError: Boolean = false,
        val isRateLimited: Boolean = false,
        val isAuthExpired: Boolean = false
    ) : ProviderResult<Nothing>
    data class Unsupported(val reason: String) : ProviderResult<Nothing>
}

interface MusicProvider {
    val descriptor: ProviderDescriptor
    suspend fun getStatus(): ProviderConnectionStatus
    suspend fun searchTracks(query: String, limit: Int = 20): ProviderResult<List<MusicTrack>>
    suspend fun searchArtists(query: String, limit: Int = 10): ProviderResult<List<MusicArtist>> =
        ProviderResult.Unsupported("Artist search not supported by ${descriptor.displayName}")
    suspend fun searchAlbums(query: String, limit: Int = 10): ProviderResult<List<MusicAlbum>> =
        ProviderResult.Unsupported("Album search not supported by ${descriptor.displayName}")
    suspend fun searchPlaylists(query: String, limit: Int = 10): ProviderResult<List<MusicPlaylist>> =
        ProviderResult.Unsupported("Playlist search not supported by ${descriptor.displayName}")
    suspend fun getTrendingTracks(limit: Int = 20): ProviderResult<List<MusicTrack>> =
        ProviderResult.Unsupported("Charts not supported by ${descriptor.displayName}")
    suspend fun resolveStreamUrl(track: MusicTrack): ProviderResult<String> =
        ProviderResult.Unsupported("Direct streaming not permitted for ${descriptor.displayName}")
}
