package com.example.providers

import android.content.Context
import com.example.core.model.MusicAlbum
import com.example.core.model.MusicArtist
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderId
import com.example.persistence.SecureTokenVault

data class ProviderUiStatus(
    val descriptor: ProviderDescriptor,
    val isEnabled: Boolean,
    val status: ProviderConnectionStatus
)

data class OnlineSearchAggregate(
    val providerId: ProviderId,
    val tracks: List<MusicTrack> = emptyList(),
    val artists: List<MusicArtist> = emptyList(),
    val albums: List<MusicAlbum> = emptyList(),
    val playlists: List<MusicPlaylist> = emptyList(),
    val errorMessage: String? = null
)

class ProviderManager(
    context: Context,
    tokenVault: SecureTokenVault,
    jamendoClientIdProvider: () -> String
) {
    val audius = AudiusProvider()
    val jamendo = JamendoProvider(jamendoClientIdProvider)
    val ytMusicExperimental = YouTubeMusicExperimentalProvider(context)
    val spotify = SpotifyExternalProvider(context, tokenVault)

    private val remoteProviders: Map<ProviderId, MusicProvider> = mapOf(
        ProviderId.AUDIUS to audius,
        ProviderId.JAMENDO to jamendo,
        ProviderId.YOUTUBE_MUSIC_EXPERIMENTAL to ytMusicExperimental,
        ProviderId.SPOTIFY to spotify
    )

    suspend fun getProviderStatuses(
        isLocalOnlyMode: Boolean,
        enabledProviders: Set<ProviderId>
    ): List<ProviderUiStatus> {
        return remoteProviders.values.map { provider ->
            val enabled = !isLocalOnlyMode && provider.descriptor.id in enabledProviders
            val status = if (!enabled) ProviderConnectionStatus.DISABLED else provider.getStatus()
            ProviderUiStatus(
                descriptor = provider.descriptor,
                isEnabled = enabled,
                status = status
            )
        }
    }

    suspend fun fetchDiscoveryTracks(
        isLocalOnlyMode: Boolean,
        enabledProviders: Set<ProviderId>
    ): List<Pair<ProviderId, ProviderResult<List<MusicTrack>>>> {
        if (isLocalOnlyMode) return emptyList()
        val results = mutableListOf<Pair<ProviderId, ProviderResult<List<MusicTrack>>>>()
        if (ProviderId.AUDIUS in enabledProviders) {
            results.add(ProviderId.AUDIUS to audius.getTrendingTracks(limit = 12))
        }
        if (ProviderId.JAMENDO in enabledProviders) {
            results.add(ProviderId.JAMENDO to jamendo.getTrendingTracks(limit = 12))
        }
        return results
    }

    suspend fun searchOnlineProviders(
        query: String,
        isLocalOnlyMode: Boolean,
        enabledProviders: Set<ProviderId>,
        filterProvider: ProviderId? = null
    ): List<OnlineSearchAggregate> {
        if (isLocalOnlyMode || query.isBlank()) return emptyList()
        val targetIds = enabledProviders.filter { it != ProviderId.LOCAL }
            .filter { filterProvider == null || it == filterProvider }

        return targetIds.mapNotNull { id ->
            val provider = remoteProviders[id] ?: return@mapNotNull null
            val trackRes = provider.searchTracks(query, limit = 15)
            val artistRes = provider.searchArtists(query, limit = 6)
            val albumRes = provider.searchAlbums(query, limit = 6)
            val playlistRes = provider.searchPlaylists(query, limit = 6)

            val tracks = (trackRes as? ProviderResult.Success)?.data ?: emptyList()
            val artists = (artistRes as? ProviderResult.Success)?.data ?: emptyList()
            val albums = (albumRes as? ProviderResult.Success)?.data ?: emptyList()
            val playlists = (playlistRes as? ProviderResult.Success)?.data ?: emptyList()

            val err = when (trackRes) {
                is ProviderResult.Error -> trackRes.message
                is ProviderResult.Unsupported -> trackRes.reason
                is ProviderResult.Success -> null
            }

            OnlineSearchAggregate(
                providerId = id,
                tracks = tracks,
                artists = artists,
                albums = albums,
                playlists = playlists,
                errorMessage = err
            )
        }
    }

    suspend fun resolveStreamUrl(track: MusicTrack, isLocalOnlyMode: Boolean): ProviderResult<String> {
        if (track.providerId == ProviderId.LOCAL) {
            val uri = track.localContentUri ?: track.localFilePath
            return if (uri != null) ProviderResult.Success(uri)
            else ProviderResult.Error("Missing local file URI")
        }
        if (isLocalOnlyMode) {
            return ProviderResult.Error("Local-Only Mode is enabled. Disable it in Settings to stream online tracks.")
        }
        val provider = remoteProviders[track.providerId]
            ?: return ProviderResult.Unsupported("Unknown provider ${track.providerId}")
        return provider.resolveStreamUrl(track)
    }
}
