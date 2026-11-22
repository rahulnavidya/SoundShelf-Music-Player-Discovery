package com.example.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.designsystem.NowPlayingLayoutMode
import com.example.core.designsystem.SoundShelfThemeId
import com.example.core.designsystem.ThemeCustomizations
import com.example.core.model.HomeSectionId
import com.example.core.model.ProviderId
import com.example.core.model.QueueEntry
import com.example.core.model.RepeatMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "soundshelf_prefs")

@Serializable
data class HomeConfig(
    val sectionOrder: List<HomeSectionId> = HomeSectionId.entries,
    val hiddenSections: Set<HomeSectionId> = emptySet(),
    val separateLocalAndOnline: Boolean = true,
    val showOnlineDiscovery: Boolean = true,
    val showRecentlyPlayed: Boolean = true
)

@Serializable
data class AudioEngineSettings(
    val gaplessPlayback: Boolean = true,
    val crossfadeSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val playbackPitch: Float = 1.0f,
    val replayGainEnabled: Boolean = false,
    val volumeNormalization: Boolean = false,
    val equalizerEnabled: Boolean = false,
    val equalizerBandsMb: List<Int> = listOf(0, 0, 0, 0, 0),
    val bassBoostStrength: Int = 0,
    val loudnessGainMb: Int = 0,
    val balance: Float = 0f
)

@Serializable
data class SavedPlaybackSession(
    val queue: List<QueueEntry> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val playbackSpeed: Float = 1.0f
)

data class UserPreferencesState(
    val onboardingCompleted: Boolean = false,
    val themeId: SoundShelfThemeId = SoundShelfThemeId.MIDNIGHT_STUDIO,
    val themeCustomizations: ThemeCustomizations = ThemeCustomizations(),
    val nowPlayingLayout: NowPlayingLayoutMode = NowPlayingLayoutMode.CLASSIC,
    val homeConfig: HomeConfig = HomeConfig(),
    val audioSettings: AudioEngineSettings = AudioEngineSettings(),
    val isLocalOnlyMode: Boolean = false,
    val enabledProviders: Set<ProviderId> = setOf(ProviderId.LOCAL, ProviderId.AUDIUS),
    val hapticsEnabled: Boolean = true,
    val historyTrackingEnabled: Boolean = true,
    val onlineLyricsEnabled: Boolean = true,
    val jamendoClientId: String = "",
    val recentSearches: List<String> = emptyList(),
    val savedSession: SavedPlaybackSession = SavedPlaybackSession()
)

class UserPreferencesRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_ID = stringPreferencesKey("theme_id")
        val THEME_CUSTOMIZATIONS = stringPreferencesKey("theme_customizations")
        val NOW_PLAYING_LAYOUT = stringPreferencesKey("now_playing_layout")
        val HOME_CONFIG = stringPreferencesKey("home_config")
        val AUDIO_SETTINGS = stringPreferencesKey("audio_settings")
        val LOCAL_ONLY_MODE = booleanPreferencesKey("local_only_mode")
        val ENABLED_PROVIDERS = stringPreferencesKey("enabled_providers")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HISTORY_TRACKING = booleanPreferencesKey("history_tracking")
        val ONLINE_LYRICS = booleanPreferencesKey("online_lyrics")
        val JAMENDO_CLIENT_ID = stringPreferencesKey("jamendo_client_id")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val SAVED_SESSION = stringPreferencesKey("saved_playback_session")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.dataStore.data.map { prefs ->
        val themeId = prefs[Keys.THEME_ID]?.let {
            runCatching { SoundShelfThemeId.valueOf(it) }.getOrNull()
        } ?: SoundShelfThemeId.MIDNIGHT_STUDIO

        val customizations = prefs[Keys.THEME_CUSTOMIZATIONS]?.let {
            runCatching { json.decodeFromString<ThemeCustomizations>(it) }.getOrNull()
        } ?: ThemeCustomizations()

        val nowPlayingLayout = prefs[Keys.NOW_PLAYING_LAYOUT]?.let {
            runCatching { NowPlayingLayoutMode.valueOf(it) }.getOrNull()
        } ?: NowPlayingLayoutMode.CLASSIC

        val homeConfig = prefs[Keys.HOME_CONFIG]?.let {
            runCatching { json.decodeFromString<HomeConfig>(it) }.getOrNull()
        } ?: HomeConfig()

        val audioSettings = prefs[Keys.AUDIO_SETTINGS]?.let {
            runCatching { json.decodeFromString<AudioEngineSettings>(it) }.getOrNull()
        } ?: AudioEngineSettings()

        val providers = prefs[Keys.ENABLED_PROVIDERS]?.let {
            runCatching { json.decodeFromString<Set<ProviderId>>(it) }.getOrNull()
        } ?: setOf(ProviderId.LOCAL, ProviderId.AUDIUS)

        val recentSearches = prefs[Keys.RECENT_SEARCHES]?.let {
            runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
        } ?: emptyList()

        val savedSession = prefs[Keys.SAVED_SESSION]?.let {
            runCatching { json.decodeFromString<SavedPlaybackSession>(it) }.getOrNull()
        } ?: SavedPlaybackSession()

        UserPreferencesState(
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            themeId = themeId,
            themeCustomizations = customizations,
            nowPlayingLayout = nowPlayingLayout,
            homeConfig = homeConfig,
            audioSettings = audioSettings,
            isLocalOnlyMode = prefs[Keys.LOCAL_ONLY_MODE] ?: false,
            enabledProviders = providers + ProviderId.LOCAL,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            historyTrackingEnabled = prefs[Keys.HISTORY_TRACKING] ?: true,
            onlineLyricsEnabled = prefs[Keys.ONLINE_LYRICS] ?: true,
            jamendoClientId = prefs[Keys.JAMENDO_CLIENT_ID] ?: "",
            recentSearches = recentSearches,
            savedSession = savedSession
        )
    }

    suspend fun completeOnboarding() {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = true }
    }

    suspend fun setTheme(themeId: SoundShelfThemeId) {
        context.dataStore.edit { it[Keys.THEME_ID] = themeId.name }
    }

    suspend fun updateThemeCustomizations(customizations: ThemeCustomizations) {
        context.dataStore.edit { it[Keys.THEME_CUSTOMIZATIONS] = json.encodeToString(customizations) }
    }

    suspend fun setNowPlayingLayout(mode: NowPlayingLayoutMode) {
        context.dataStore.edit { it[Keys.NOW_PLAYING_LAYOUT] = mode.name }
    }

    suspend fun updateHomeConfig(config: HomeConfig) {
        context.dataStore.edit { it[Keys.HOME_CONFIG] = json.encodeToString(config) }
    }

    suspend fun updateAudioSettings(settings: AudioEngineSettings) {
        context.dataStore.edit { it[Keys.AUDIO_SETTINGS] = json.encodeToString(settings) }
    }

    suspend fun setLocalOnlyMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCAL_ONLY_MODE] = enabled }
    }

    suspend fun setProviderEnabled(providerId: ProviderId, enabled: Boolean) {
        if (providerId == ProviderId.LOCAL) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.ENABLED_PROVIDERS]?.let {
                runCatching { json.decodeFromString<Set<ProviderId>>(it) }.getOrNull()
            } ?: setOf(ProviderId.LOCAL, ProviderId.AUDIUS)
            val updated = if (enabled) current + providerId else current - providerId
            prefs[Keys.ENABLED_PROVIDERS] = json.encodeToString(updated + ProviderId.LOCAL)
        }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS_ENABLED] = enabled }
    }

    suspend fun setHistoryTrackingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HISTORY_TRACKING] = enabled }
    }

    suspend fun setOnlineLyricsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.ONLINE_LYRICS] = enabled }
    }

    suspend fun setJamendoClientId(clientId: String) {
        context.dataStore.edit { it[Keys.JAMENDO_CLIENT_ID] = clientId.trim() }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isEmpty()) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES]?.let {
                runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
            } ?: emptyList()
            val updated = (listOf(clean) + current.filterNot { it.equals(clean, ignoreCase = true) }).take(15)
            prefs[Keys.RECENT_SEARCHES] = json.encodeToString(updated)
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    suspend fun savePlaybackSession(session: SavedPlaybackSession) {
        context.dataStore.edit { it[Keys.SAVED_SESSION] = json.encodeToString(session) }
    }
}
