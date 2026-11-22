package com.example.providers

import com.example.core.model.AudioQualityInfo
import com.example.core.model.MusicAlbum
import com.example.core.model.MusicArtist
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
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class JamendoTracksResponse(val results: List<JamendoTrackDto> = emptyList())

@JsonClass(generateAdapter = true)
data class JamendoArtistsResponse(val results: List<JamendoArtistDto> = emptyList())

@JsonClass(generateAdapter = true)
data class JamendoAlbumsResponse(val results: List<JamendoAlbumDto> = emptyList())

@JsonClass(generateAdapter = true)
data class JamendoTrackDto(
    val id: String,
    val name: String,
    val duration: Int = 0,
    @Json(name = "artist_id") val artistId: String = "",
    @Json(name = "artist_name") val artistName: String = "Jamendo Artist",
    @Json(name = "album_id") val albumId: String = "",
    @Json(name = "album_name") val albumName: String = "",
    @Json(name = "album_image") val albumImage: String? = null,
    val audio: String? = null,
    val audiodownload: String? = null,
    @Json(name = "audiodownload_allowed") val audiodownloadAllowed: Boolean = false,
    @Json(name = "license_ccurl") val licenseCcUrl: String? = null,
    val shareurl: String? = null,
    @Json(name = "releasedate") val releaseDate: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoArtistDto(
    val id: String,
    val name: String,
    val image: String? = null,
    val shareurl: String? = null
)

@JsonClass(generateAdapter = true)
data class JamendoAlbumDto(
    val id: String,
    val name: String,
    @Json(name = "artist_name") val artistName: String = "",
    val image: String? = null,
    @Json(name = "releasedate") val releaseDate: String? = null,
    val shareurl: String? = null
)

interface JamendoApiService {
    @GET("v3.0/tracks/")
    suspend fun searchTracks(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("search") query: String? = null,
        @Query("order") order: String = "popularity_week",
        @Query("limit") limit: Int = 20
    ): JamendoTracksResponse

    @GET("v3.0/artists/")
    suspend fun searchArtists(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("namesearch") query: String,
        @Query("limit") limit: Int = 10
    ): JamendoArtistsResponse

    @GET("v3.0/albums/")
    suspend fun searchAlbums(
        @Query("client_id") clientId: String,
        @Query("format") format: String = "json",
        @Query("namesearch") query: String,
        @Query("limit") limit: Int = 10
    ): JamendoAlbumsResponse
}

class JamendoProvider(
    private val clientIdProvider: () -> String
) : MusicProvider {

    private val api: JamendoApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.jamendo.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(JamendoApiService::class.java)
    }

    override val descriptor = ProviderDescriptor(
        id = ProviderId.JAMENDO,
        displayName = "Jamendo",
        description = "Creative Commons independent music platform. Supports licensed streaming, attribution tracking, and creator-authorised CC downloads.",
        capabilities = setOf(
            ProviderCapability.SEARCH,
            ProviderCapability.TRACK_DETAILS,
            ProviderCapability.ARTIST_DETAILS,
            ProviderCapability.ALBUM_DETAILS,
            ProviderCapability.STREAM_PLAYBACK,
            ProviderCapability.RECOMMENDATIONS,
            ProviderCapability.DOWNLOAD_LINKS,
            ProviderCapability.DIRECT_DOWNLOAD
        ),
        authType = ProviderAuthType.CLIENT_KEY,
        privacyNotice = "Requires a Jamendo Developer Client ID (configured in Connected Services). Only explicit queries are sent to api.jamendo.com.",
        restrictionNotice = "Respects Creative Commons terms (e.g., NonCommercial / ShareAlike) and per-track audiodownload_allowed flags."
    )

    override suspend fun getStatus(): ProviderConnectionStatus {
        return if (clientIdProvider().isBlank()) ProviderConnectionStatus.REQUIRES_CONFIG
        else ProviderConnectionStatus.CONNECTED
    }

    override suspend fun getTrendingTracks(limit: Int): ProviderResult<List<MusicTrack>> {
        val cid = clientIdProvider().trim()
        if (cid.isEmpty()) {
            return ProviderResult.Error("Add a Jamendo Client ID in Connected Services to browse Jamendo CC charts.")
        }
        return safeCall {
            api.searchTracks(clientId = cid, query = null, limit = limit).results.map { it.toDomain() }
        }
    }

    override suspend fun searchTracks(query: String, limit: Int): ProviderResult<List<MusicTrack>> {
        val cid = clientIdProvider().trim()
        if (cid.isEmpty()) {
            return ProviderResult.Error("Jamendo Client ID not configured in Connected Services.")
        }
        return safeCall {
            api.searchTracks(clientId = cid, query = query, limit = limit).results.map { it.toDomain() }
        }
    }

    override suspend fun searchArtists(query: String, limit: Int): ProviderResult<List<MusicArtist>> {
        val cid = clientIdProvider().trim()
        if (cid.isEmpty()) return ProviderResult.Error("Jamendo Client ID not configured.")
        return safeCall {
            api.searchArtists(clientId = cid, query = query, limit = limit).results.map {
                MusicArtist(
                    id = "jamendo_artist_${it.id}",
                    providerId = ProviderId.JAMENDO,
                    name = it.name,
                    artworkUri = it.image,
                    externalUrl = it.shareurl
                )
            }
        }
    }

    override suspend fun searchAlbums(query: String, limit: Int): ProviderResult<List<MusicAlbum>> {
        val cid = clientIdProvider().trim()
        if (cid.isEmpty()) return ProviderResult.Error("Jamendo Client ID not configured.")
        return safeCall {
            api.searchAlbums(clientId = cid, query = query, limit = limit).results.map {
                MusicAlbum(
                    id = "jamendo_album_${it.id}",
                    providerId = ProviderId.JAMENDO,
                    title = it.name,
                    artist = it.artistName,
                    year = it.releaseDate?.take(4)?.toIntOrNull() ?: 0,
                    artworkUri = it.image,
                    externalUrl = it.shareurl
                )
            }
        }
    }

    override suspend fun resolveStreamUrl(track: MusicTrack): ProviderResult<String> {
        val url = track.externalPageUrl?.takeIf { it.contains("mp3") } ?: track.downloadUrl
        return if (!url.isNullOrBlank()) ProviderResult.Success(url)
        else ProviderResult.Unsupported("No active Jamendo stream URL for ${track.title}")
    }

    private fun JamendoTrackDto.toDomain(): MusicTrack {
        return MusicTrack(
            id = "jamendo_$id",
            providerId = ProviderId.JAMENDO,
            providerTrackId = id,
            title = name,
            artist = artistName,
            artistId = "jamendo_artist_$artistId",
            album = albumName.ifBlank { "Jamendo Single" },
            albumId = "jamendo_album_$albumId",
            genre = "Creative Commons",
            year = releaseDate?.take(4)?.toIntOrNull() ?: 0,
            durationMs = duration * 1000L,
            artworkUri = albumImage,
            qualityInfo = AudioQualityInfo(
                format = "MP3",
                codec = "MPEG-1 Layer III",
                bitrateKbps = 96,
                sourceKind = QualitySourceKind.STREAMING
            ),
            licenseName = "Creative Commons",
            licenseUrl = licenseCcUrl,
            isDownloadPermitted = audiodownloadAllowed && !audiodownload.isNullOrBlank(),
            downloadUrl = if (audiodownloadAllowed) audiodownload else null,
            externalPageUrl = audio ?: shareurl,
            canJoinUnifiedQueue = true
        )
    }

    private inline fun <T> safeCall(block: () -> T): ProviderResult<T> {
        return try {
            ProviderResult.Success(block())
        } catch (e: retrofit2.HttpException) {
            ProviderResult.Error("Jamendo HTTP ${e.code()}", isRateLimited = e.code() == 429)
        } catch (e: Exception) {
            ProviderResult.Error(e.localizedMessage ?: "Jamendo network error", isNetworkError = true)
        }
    }
}
