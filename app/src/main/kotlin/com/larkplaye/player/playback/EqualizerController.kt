package com.larkplaye.player.playback

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import com.larkplaye.player.R
import com.larkplaye.player.core.Prefs
import com.larkplaye.player.core.model.EqState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class EqualizerController(
    private val context: Context,
    private val prefs: Prefs,
    private val scope: CoroutineScope,
    private val state: MutableStateFlow<EqState>,
) {
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var reverb: PresetReverb? = null
    private var sessionId: Int = 0
    private var attachJob: Job? = null
    private var bandCount: Int = 5
    private var levels: ShortArray = ShortArray(5)
    private var presetId: String = "custom"
    private var enabled: Boolean = false
    private var bassStrength: Int = 0
    private var virtualizerStrength: Int = 0
    private var reverbPreset: Int = 0

    private data class Preset(val id: String, val labelRes: Int, val db: FloatArray)

    private val presets = listOf(
        Preset("flat", R.string.eq_flat, floatArrayOf(0f, 0f, 0f, 0f, 0f)),
        Preset("rock", R.string.eq_rock, floatArrayOf(5f, 3f, -1f, 2f, 4f)),
        Preset("pop", R.string.eq_pop, floatArrayOf(-1f, 2f, 4f, 2f, -1f)),
        Preset("jazz", R.string.eq_jazz, floatArrayOf(3f, 1f, -1f, 1f, 3f)),
        Preset("classical", R.string.eq_classical, floatArrayOf(4f, 2f, -2f, 2f, 4f)),
        Preset("bass", R.string.eq_bass, floatArrayOf(6f, 4f, 0f, 0f, 0f)),
        Preset("treble", R.string.eq_treble, floatArrayOf(0f, 0f, 0f, 4f, 6f)),
        Preset("vocal", R.string.eq_vocal, floatArrayOf(-2f, 0f, 4f, 3f, 1f)),
    )

    fun attach(newSessionId: Int) {
        if (newSessionId <= 0 || newSessionId == sessionId) return
        detachEffects()
        sessionId = newSessionId
        attachJob?.cancel()
        attachJob = scope.launch(Dispatchers.Main) {
            val settings = prefs.current()
            enabled = settings.eqEnabled
            presetId = settings.eqPreset
            bassStrength = settings.bassBoost
            virtualizerStrength = settings.virtualizer
            reverbPreset = settings.reverb
            val storedLevels = settings.eqBands.split(',').mapNotNull { it.trim().toShortOrNull() }
            try {
                val eq = Equalizer(0, sessionId)
                equalizer = eq
                bandCount = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                if (storedLevels.size == bandCount) {
                    levels = storedLevels.toShortArray()
                } else {
                    levels = presetLevels(presetId, range[0], range[1])
                }
                eq.enabled = enabled
                levels.forEachIndexed { index, level ->
                    runCatching { eq.setBandLevel(index.toShort(), level) }
                }
            } catch (_: Exception) {
                equalizer = null
            }
            bassBoost = runCatching { BassBoost(0, sessionId) }.getOrNull()?.apply {
                runCatching {
                    this.enabled = enabled && bassStrength > 0
                    this.setStrength(bassStrength.toShort())
                }
            }
            virtualizer = runCatching { Virtualizer(0, sessionId) }.getOrNull()?.apply {
                runCatching {
                    this.enabled = enabled && virtualizerStrength > 0
                    this.setStrength(virtualizerStrength.toShort())
                }
            }
            reverb = runCatching { PresetReverb(0, sessionId) }.getOrNull()?.apply {
                runCatching {
                    this.preset = reverbPresetFor(reverbPreset)
                    this.enabled = enabled && reverbPreset > 0
                }
            }
            publish()
        }
    }

    fun release() {
        attachJob?.cancel()
        detachEffects()
        sessionId = 0
        state.value = EqState()
    }

    private fun detachEffects() {
        runCatching { equalizer?.enabled = false }
        runCatching { equalizer?.release() }
        runCatching { bassBoost?.enabled = false }
        runCatching { bassBoost?.release() }
        runCatching { virtualizer?.enabled = false }
        runCatching { virtualizer?.release() }
        runCatching { reverb?.enabled = false }
        runCatching { reverb?.release() }
        equalizer = null
        bassBoost = null
        virtualizer = null
        reverb = null
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        val eq = equalizer
        runCatching { eq?.enabled = value }
        runCatching { bassBoost?.enabled = value && bassStrength > 0 }
        runCatching { virtualizer?.enabled = value && virtualizerStrength > 0 }
        runCatching { reverb?.enabled = value && reverbPreset > 0 }
        publish()
        scope.launch { prefs.setEqEnabled(value) }
    }

    fun setBand(index: Int, levelMb: Int) {
        val eq = equalizer ?: return
        if (index !in levels.indices) return
        val range = runCatching { eq.bandLevelRange }.getOrNull() ?: return
        val clamped = levelMb.coerceIn(range[0].toInt(), range[1].toInt()).toShort()
        levels[index] = clamped
        runCatching { eq.setBandLevel(index.toShort(), clamped) }
        presetId = "custom"
        publish()
        scope.launch { prefs.setEqBands(levels.joinToString(",")) }
        scope.launch { prefs.setEqPreset("custom") }
    }

    fun applyPreset(index: Int) {
        val preset = presets.getOrNull(index) ?: return
        val eq = equalizer
        presetId = preset.id
        if (eq != null) {
            val range = runCatching { eq.bandLevelRange }.getOrNull()
            if (range != null) {
                levels = presetLevels(preset.id, range[0], range[1])
                levels.forEachIndexed { i, level ->
                    runCatching { eq.setBandLevel(i.toShort(), level) }
                }
            }
        }
        publish()
        scope.launch {
            prefs.setEqPreset(preset.id)
            prefs.setEqBands(levels.joinToString(","))
        }
    }

    fun setBassBoost(strength: Int) {
        bassStrength = strength.coerceIn(0, 1000)
        runCatching {
            bassBoost?.setStrength(bassStrength.toShort())
            bassBoost?.enabled = enabled && bassStrength > 0
        }
        publish()
        scope.launch { prefs.setBassBoost(bassStrength) }
    }

    fun setVirtualizer(strength: Int) {
        virtualizerStrength = strength.coerceIn(0, 1000)
        runCatching {
            virtualizer?.setStrength(virtualizerStrength.toShort())
            virtualizer?.enabled = enabled && virtualizerStrength > 0
        }
        publish()
        scope.launch { prefs.setVirtualizer(virtualizerStrength) }
    }

    fun setReverb(preset: Int) {
        reverbPreset = preset.coerceIn(0, 5)
        runCatching {
            reverb?.preset = reverbPresetFor(reverbPreset)
            reverb?.enabled = enabled && reverbPreset > 0
        }
        publish()
        scope.launch { prefs.setReverb(reverbPreset) }
    }

    fun reset() {
        presetId = "flat"
        levels = ShortArray(bandCount)
        val eq = equalizer
        levels.forEachIndexed { i, level ->
            runCatching { eq?.setBandLevel(i.toShort(), level) }
        }
        bassStrength = 0
        virtualizerStrength = 0
        reverbPreset = 0
        runCatching {
            bassBoost?.setStrength(0)
            bassBoost?.enabled = false
            virtualizer?.setStrength(0)
            virtualizer?.enabled = false
            reverb?.enabled = false
        }
        publish()
        scope.launch {
            prefs.setEqPreset("flat")
            prefs.setEqBands(levels.joinToString(","))
            prefs.setBassBoost(0)
            prefs.setVirtualizer(0)
            prefs.setReverb(0)
        }
    }

    private fun presetLevels(id: String, minLevel: Short, maxLevel: Short): ShortArray {
        val preset = presets.firstOrNull { it.id == id } ?: return ShortArray(bandCount)
        val db = interpolate(preset.db, bandCount)
        return ShortArray(bandCount) { index ->
            (db[index] * 100f).roundToInt()
                .coerceIn(minLevel.toInt(), maxLevel.toInt())
                .toShort()
        }
    }

    private fun interpolate(values: FloatArray, count: Int): FloatArray {
        if (count <= 0) return FloatArray(0)
        if (count == values.size) return values
        if (values.isEmpty()) return FloatArray(count)
        if (count == 1) return floatArrayOf(values[values.size / 2])
        return FloatArray(count) { i ->
            val position = i * (values.size - 1).toFloat() / (count - 1).toFloat()
            val low = position.toInt().coerceIn(0, values.lastIndex)
            val high = (low + 1).coerceAtMost(values.lastIndex)
            val fraction = position - low
            values[low] * (1f - fraction) + values[high] * fraction
        }
    }

    private fun reverbPresetFor(value: Int): Short = when (value) {
        1 -> PresetReverb.PRESET_SMALLROOM
        2 -> PresetReverb.PRESET_MEDIUMROOM
        3 -> PresetReverb.PRESET_LARGEROOM
        4 -> PresetReverb.PRESET_MEDIUMHALL
        5 -> PresetReverb.PRESET_LARGEHALL
        else -> PresetReverb.PRESET_NONE
    }

    private fun publish() {
        val eq = equalizer
        val presetIndex = presets.indexOfFirst { it.id == presetId }
        state.value = EqState(
            attached = eq != null,
            enabled = enabled,
            bandCount = bandCount,
            minLevel = runCatching { eq?.bandLevelRange?.get(0) }.getOrNull() ?: -1500,
            maxLevel = runCatching { eq?.bandLevelRange?.get(1) }.getOrNull() ?: 1500,
            bandLevels = levels.toList(),
            presetNames = presets.map { context.getString(it.labelRes) },
            selectedPreset = presetIndex,
            presetLabel = if (presetIndex >= 0) {
                context.getString(presets[presetIndex].labelRes)
            } else {
                context.getString(R.string.eq_custom)
            },
            bassBoost = bassStrength,
            virtualizer = virtualizerStrength,
            reverb = reverbPreset,
            bassAvailable = bassBoost != null,
            virtualizerAvailable = virtualizer != null,
            reverbAvailable = reverb != null,
        )
    }
}
