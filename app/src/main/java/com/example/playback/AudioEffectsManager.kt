package com.example.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import com.example.persistence.AudioEngineSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqBandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val currentLevelMb: Int
)

data class AudioEffectCapabilities(
    val audioSessionId: Int = 0,
    val equalizerSupported: Boolean = false,
    val bandCount: Int = 0,
    val minBandLevelMb: Int = -1500,
    val maxBandLevelMb: Int = 1500,
    val bands: List<EqBandInfo> = emptyList(),
    val presets: List<String> = emptyList(),
    val bassBoostSupported: Boolean = false,
    val loudnessEnhancerSupported: Boolean = false
)

class AudioEffectsManager {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var currentSessionId: Int = 0

    private val _capabilities = MutableStateFlow(AudioEffectCapabilities())
    val capabilities: StateFlow<AudioEffectCapabilities> = _capabilities.asStateFlow()

    fun attachToSession(audioSessionId: Int, settings: AudioEngineSettings) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) {
            applySettings(settings)
            return
        }
        release()
        currentSessionId = audioSessionId

        var eqSupported = false
        var bands = emptyList<EqBandInfo>()
        var minMb = -1500
        var maxMb = 1500
        var presets = emptyList<String>()

        runCatching {
            val eq = Equalizer(0, audioSessionId)
            equalizer = eq
            eqSupported = true
            val range = eq.bandLevelRange
            minMb = range[0].toInt()
            maxMb = range[1].toInt()
            val numBands = eq.numberOfBands.toInt()
            bands = (0 until numBands).map { idx ->
                EqBandInfo(
                    index = idx,
                    centerFreqHz = eq.getCenterFreq(idx.toShort()) / 1000,
                    currentLevelMb = eq.getBandLevel(idx.toShort()).toInt()
                )
            }
            presets = (0 until eq.numberOfPresets.toInt()).map { p ->
                eq.getPresetName(p.toShort())
            }
        }

        val bassSupported = runCatching {
            val bb = BassBoost(0, audioSessionId)
            bassBoost = bb
            bb.strengthSupported
        }.getOrDefault(false)

        val loudnessSupported = runCatching {
            loudnessEnhancer = LoudnessEnhancer(audioSessionId)
            true
        }.getOrDefault(false)

        _capabilities.value = AudioEffectCapabilities(
            audioSessionId = audioSessionId,
            equalizerSupported = eqSupported,
            bandCount = bands.size,
            minBandLevelMb = minMb,
            maxBandLevelMb = maxMb,
            bands = bands,
            presets = presets,
            bassBoostSupported = bassSupported,
            loudnessEnhancerSupported = loudnessSupported
        )
        applySettings(settings)
    }

    fun applySettings(settings: AudioEngineSettings) {
        equalizer?.let { eq ->
            runCatching {
                eq.enabled = settings.equalizerEnabled
                val count = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val min = range[0].toInt()
                val max = range[1].toInt()
                for (i in 0 until count) {
                    val target = settings.equalizerBandsMb.getOrElse(i) { 0 }.coerceIn(min, max)
                    eq.setBandLevel(i.toShort(), target.toShort())
                }
                val updatedBands = (0 until count).map { idx ->
                    EqBandInfo(
                        index = idx,
                        centerFreqHz = eq.getCenterFreq(idx.toShort()) / 1000,
                        currentLevelMb = eq.getBandLevel(idx.toShort()).toInt()
                    )
                }
                _capabilities.value = _capabilities.value.copy(bands = updatedBands)
            }
        }

        bassBoost?.let { bb ->
            runCatching {
                val strength = settings.bassBoostStrength.coerceIn(0, 1000)
                bb.enabled = strength > 0
                if (bb.strengthSupported) {
                    bb.setStrength(strength.toShort())
                }
            }
        }

        loudnessEnhancer?.let { le ->
            runCatching {
                val gain = settings.loudnessGainMb.coerceIn(0, 1500)
                val active = gain > 0 || settings.volumeNormalization
                le.enabled = active
                val effectiveGain = if (settings.volumeNormalization && gain == 0) 350 else gain
                le.setTargetGain(effectiveGain)
            }
        }
    }

    fun getPresetBandLevels(presetIndex: Int): List<Int>? {
        val eq = equalizer ?: return null
        return runCatching {
            eq.usePreset(presetIndex.toShort())
            (0 until eq.numberOfBands.toInt()).map { idx ->
                eq.getBandLevel(idx.toShort()).toInt()
            }
        }.getOrNull()
    }

    fun release() {
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.release() }
        runCatching { loudnessEnhancer?.release() }
        equalizer = null
        bassBoost = null
        loudnessEnhancer = null
        currentSessionId = 0
    }
}
