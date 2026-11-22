package com.example.providers

import com.example.core.model.AudioQualityInfo
import com.example.core.model.MusicArtist
import com.example.core.model.MusicPlaylist
import com.example.core.model.MusicTrack
import com.example.core.model.ProviderCapability
import com.example.core.model.ProviderId
import com.example.core.model.QualitySourceKind
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class AudiusTrackListResponse(val data: List<AudiusTrackDto> = emptyList())

@JsonClass(generateAdapter = true)
data class AudiusUserListResponse(val data: List<AudiusUserDto> = emptyList())

@JsonClass(generateAdapter = true)
data class AudiusPlaylistListResponse(val data: List<AudiusPlaylistDto> = emptyList())

@JsonClass(generateAdapter = true)
data class AudiusArtworkDto(
    @Json(name = "150x150") val small: String? = null,
    @Json(name = "480x480") val medium: String? = null,
    @Json(name = "1000x1000") val large: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusUserDto(
    val id: String,
    val name: String,
    val handle: String? = null,
    val bio: String? = null,
    @Json(name = "profile_picture") val profilePicture: AudiusArtworkDto? = null,
    @Json(name = "track_count") val trackCount: Int = 0,
    @Json(name = "album_count") val albumCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class AudiusTrackDto(
    val id: String,
    val title: String,
    val genre: String? = null,
    val duration: Int = 0,
    val artwork: AudiusArtworkDto? = null,
    val user: AudiusUserDto? = null,
    val permalink: String? = null,
    @Json(name = "is_downloadable") val isDownloadable: Boolean = false,
    @Json(name = "release_date") val releaseDate: String? = null,
    val isrc: String? = null
)

@JsonClass(generateAdapter = true)
data class AudiusPlaylistDto(
    val id: String,
    @Json(name = "playlist_name") val playlistName: String,
    val description: String? = null,
    val artwork: AudiusArtworkDto? = null,
    val permalink: String? = null
)

interface AudiusApiService {
    @GET("v1/tracks/trending")
    suspend fun getTrendingTracks(
        @Query("app_name") appName: String = "SoundShelf",
        @Query("limit") limit: Int = 20
    ): AudiusTrackListResponse

    @GET("v1/tracks/search")
    suspend fun searchTracks(
        @Query("query") query: String,
        @Query("app_name") appName: String = "SoundShelf",
        @Query("limit") limit: Int = 20
    ): AudiusTrackListResponse

    @GET("v1/users/search")
    suspend fun searchUsers(
        @Query("query") query: String,
        @Query("app_name") appName: String = "SoundShelf",
        @Query("limit") limit: Int = 10
    ): AudiusUserListResponse

    @GET("v1/playlists/search")
    suspend fun searchPlaylists(
        @Query("query") query: String,
        @Query("app_name") appName: String = "SoundShelf",
        @Query("limit") limit: Int = 10
    ): AudiusPlaylistListResponse
}

class AudiusProvider : MusicProvider {
    private val baseUrl = "https://discoveryprovider.audius.co/"

    private val api: AudiusApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(AudiusApiService::class.java)
    }

    override val descriptor = ProviderDescriptor(
        id = ProviderId.AUDIUS,
        displayName = "Audius",
        description = "Open artist-first music streaming & discovery protocol. Supports free public discovery, artist profiles, playlists, and creator-permitted downloads.",
        capabilities = setOf(
            ProviderCapability.SEARCH,
            ProviderCapability.TRACK_DETAILS,
            ProviderCapability.ARTIST_DETAILS,
            ProviderCapability.PLAYLIST_DETAILS,
            ProviderCapability.STREAM_PLAYBACK,
            ProviderCapability.RECOMMENDATIONS,
            ProviderCapability.DOWNLOAD_LINKS,
            ProviderCapability.DIRECT_DOWNLOAD
        ),
        authType = ProviderAuthType.NONE,
        privacyNotice = "Only sends your explicit search query or trending request to Audius Discovery Nodes with app_name=SoundShelf. Never sends local library data.",
        restrictionNotice = "Stream and download permissions are set per track by the creator. Only creator-marked downloadable tracks allow file downloads."
    )

    override suspend fun getStatus(): ProviderConnectionStatus = ProviderConnectionStatus.UNAUTHENTICATED_BROWSING

    override suspend fun getTrendingTracks(limit: Int): ProviderResult<List<MusicTrack>> = safeApiCall {
        api.getTrendingTracks(limit = limit).data.map { it.toDomain() }
    }

    override suspend fun searchTracks(query: String, limit: Int): ProviderResult<List<MusicTrack>> = safeApiCall {
        api.searchTracks(query = query, limit = limit).data.map { it.toDomain() }
    }

    override suspend fun searchArtists(query: String, limit: Int): ProviderResult<List<MusicArtist>> = safeApiCall {
        api.searchUsers(query = query, limit = limit).data.map { user ->
            MusicArtist(
                id = "audius_artist_${user.id}",
                providerId = ProviderId.AUDIUS,
                name = user.name,
                artworkUri = user.profilePicture?.medium ?: user.profilePicture?.small,
                trackCount = user.trackCount,
                albumCount = user.albumCount,
                bio = user.bio,
                externalUrl = user.handle?.let { "https://audius.co/$it" }
            )
        }
    }

    override suspend fun searchPlaylists(query: String, limit: Int): ProviderResult<List<MusicPlaylist>> = safeApiCall {
        api.searchPlaylists(query = query, limit = limit).data.map { pl ->
            MusicPlaylist(
                id = "audius_pl_${pl.id}",
                name = pl.playlistName,
                description = pl.description ?: "",
                providerId = ProviderId.AUDIUS,
                artworkUri = pl.artwork?.medium ?: pl.artwork?.small,
                externalUrl = pl.permalink?.let { "https://audius.co$it" }
            )
        }
    }

    override suspend fun resolveStreamUrl(track: MusicTrack): ProviderResult<String> {
        return ProviderResult.Success("${baseUrl}v1/tracks/${track.providerTrackId}/stream?app_name=SoundShelf")
    }

    private fun AudiusTrackDto.toDomain(): MusicTrack {
        val yearVal = releaseDate?.take(4)?.toIntOrNull() ?: 0
        val downloadEndpoint = if (isDownloadable) {
            "${baseUrl}v1/tracks/$id/download?app_name=SoundShelf"
        } else null
        return MusicTrack(
            id = "audius_$id",
            providerId = ProviderId.AUDIUS,
            providerTrackId = id,
            title = title,
            artist = user?.name ?: "Audius Artist",
            artistId = user?.id?.let { "audius_artist_$it" } ?: "",
            album = "Audius Release",
            genre = genre ?: "Independent",
            year = yearVal,
            durationMs = duration * 1000L,
            artworkUri = artwork?.medium ?: artwork?.large ?: artwork?.small,
            qualityInfo = AudioQualityInfo(
                format = "MP3 / Stream",
                codec = "Audius HLS/MP3",
                bitrateKbps = 320,
                sourceKind = QualitySourceKind.STREAMING
            ),
            licenseName = "Audius Creator Stream",
            isDownloadPermitted = isDownloadable,
            downloadUrl = downloadEndpoint,
            externalPageUrl = permalink?.let { "https://audius.co$it" },
            isrc = isrc,
            capabilities = buildSet {
                add(ProviderCapability.SEARCH)
                add(ProviderCapability.TRACK_DETAILS)
                add(ProviderCapability.STREAM_PLAYBACK)
                if (isDownloadable) {
                    add(ProviderCapability.DOWNLOAD_LINKS)
                    add(ProviderCapability.DIRECT_DOWNLOAD)
                }
            },
            canJoinUnifiedQueue = true
        )
    }

    private inline fun <T> safeApiCall(block: () -> T): ProviderResult<T> {
        return try {
            ProviderResult.Success(block())
        } catch (e: retrofit2.HttpException) {
            ProviderResult.Error(
                message = "Audius HTTP ${e.code()}: ${e.message()}",
                isRateLimited = e.code() == 429
            )
        } catch (e: Exception) {
            ProviderResult.Error(
                message = e.localizedMessage ?: "Network unavailable for Audius",
                isNetworkError = true
            )
        }
    }
}
