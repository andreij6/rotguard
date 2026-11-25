package com.rotguard.app.storage

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface HapticConductor {
    fun playSelectionTick()
    fun playWarningPulse(intensity: Float)
    fun playHoldProgressTick(progressFraction: Float)
    fun playRotDecayShudder(stageIndex: Int)
    fun playVictoryChord()
    fun playReleaseSoftTick()

    /** A crisp, clearly felt click for primary button presses (selections keep the lighter tick). */
    fun playButtonPress() = playSelectionTick()
}

private const val TICK_DURATION_MS = 16L
private const val TICK_AMPLITUDE = 110
private const val BUTTON_PRESS_SCALE = 0.8f
private const val BUTTON_PRESS_DURATION_MS = 24L
private const val BUTTON_PRESS_AMPLITUDE = 170
private const val RELEASE_DURATION_MS = 22L
private const val RELEASE_AMPLITUDE = 85
private const val WARNING_DURATION_MS = 85L
private const val MIN_AMPLITUDE = 40
private const val MAX_AMPLITUDE = 255
private const val MIN_INTENSITY = 0.1f
private const val SCALE_SOFT = 0.35f
private const val SCALE_MEDIUM = 0.65f
private const val SCALE_HIGH = 0.90f
private const val GAP_SHORT_MS = 45
private const val GAP_MEDIUM_MS = 75
private const val GAP_LONG_MS = 110
private const val STAGE_ALERT = 1
private const val STAGE_STRAINED = 2
private const val STAGE_CRACKING = 3
private const val HOLD_PULSE_BASE_SCALE = 0.25f
private const val HOLD_PULSE_RANGE_SCALE = 0.75f
private const val HOLD_PULSE_MIN_DURATION_MS = 18L
private const val HOLD_PULSE_MAX_DELTA_MS = 28L
private const val VICTORY_STEP_GAP_MS = 55

private val VICTORY_TIMINGS = longArrayOf(0L, 28L, 45L, 45L)
private val VICTORY_AMPLITUDES = intArrayOf(0, 140, 0, 230)
private const val STAGE_ZERO_DURATION_MS = 18L
private const val STAGE_ZERO_AMPLITUDE = 90
private val STAGE_ALERT_TIMINGS = longArrayOf(0L, 25L, 45L, 25L)
private val STAGE_ALERT_AMPLITUDES = intArrayOf(0, 120, 0, 140)
private val STAGE_STRAINED_TIMINGS = longArrayOf(0L, 45L, 60L, 65L)
private val STAGE_STRAINED_AMPLITUDES = intArrayOf(0, 160, 0, 190)
private val STAGE_CRACKING_TIMINGS = longArrayOf(0L, 60L, 45L, 60L, 45L, 75L)
private val STAGE_CRACKING_AMPLITUDES = intArrayOf(0, 210, 0, 220, 0, 240)
private val STAGE_ROTTED_TIMINGS = longArrayOf(0L, 100L, 80L, 120L)
private val STAGE_ROTTED_AMPLITUDES = intArrayOf(0, 255, 0, 220)

@Singleton
class AndroidHapticConductor @Inject constructor(
    @ApplicationContext private val context: Context,
) : HapticConductor {

    override fun playSelectionTick() {
        val vibrator = resolveVibrator() ?: return
        val fallback = VibrationEffect.createOneShot(TICK_DURATION_MS, TICK_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_TICK)
        ) {
            vibrateSafely(
                vibrator,
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, SCALE_MEDIUM)
                    .compose(),
            )
        } else {
            vibrateSafely(vibrator, fallback)
        }
    }

    override fun playWarningPulse(intensity: Float) {
        val vibrator = resolveVibrator() ?: return
        val clamped = intensity.coerceIn(MIN_INTENSITY, 1.0f)
        val fallbackAmp = (clamped * MAX_AMPLITUDE).toInt().coerceIn(MIN_AMPLITUDE, MAX_AMPLITUDE)
        val fallback = VibrationEffect.createOneShot(WARNING_DURATION_MS, fallbackAmp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_THUD,
            )
        ) {
            vibrateSafely(
                vibrator,
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, clamped)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, clamped, GAP_SHORT_MS)
                    .compose(),
            )
        } else {
            vibrateSafely(vibrator, fallback)
        }
    }

    override fun playHoldProgressTick(progressFraction: Float) {
        val vibrator = resolveVibrator() ?: return
        val clamped = progressFraction.coerceIn(0f, 1f)
        val scale = (HOLD_PULSE_BASE_SCALE + clamped * HOLD_PULSE_RANGE_SCALE).coerceIn(MIN_INTENSITY, 1.0f)
        val durationMs = HOLD_PULSE_MIN_DURATION_MS + (clamped * HOLD_PULSE_MAX_DELTA_MS).toLong()
        val amp = (scale * MAX_AMPLITUDE).toInt().coerceIn(MIN_AMPLITUDE, MAX_AMPLITUDE)
        val fallback = VibrationEffect.createOneShot(durationMs, amp)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val primitive = if (clamped >= SCALE_HIGH) {
                VibrationEffect.Composition.PRIMITIVE_CLICK
            } else {
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK
            }
            if (vibrator.areAllPrimitivesSupported(primitive)) {
                vibrateSafely(
                    vibrator,
                    VibrationEffect.startComposition().addPrimitive(primitive, scale).compose(),
                )
                return
            }
        }
        vibrateSafely(vibrator, fallback)
    }

    override fun playRotDecayShudder(stageIndex: Int) {
        val vibrator = resolveVibrator() ?: return
        val fallback = buildStageWaveformFallback(stageIndex)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_TICK,
                VibrationEffect.Composition.PRIMITIVE_LOW_TICK,
                VibrationEffect.Composition.PRIMITIVE_THUD,
            )
        ) {
            vibrateSafely(vibrator, buildStageComposition(stageIndex))
        } else {
            vibrateSafely(vibrator, fallback)
        }
    }

    override fun playVictoryChord() {
        val vibrator = resolveVibrator() ?: return
        val fallback = VibrationEffect.createWaveform(VICTORY_TIMINGS, VICTORY_AMPLITUDES, -1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(
                VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                VibrationEffect.Composition.PRIMITIVE_CLICK,
            )
        ) {
            vibrateSafely(
                vibrator,
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, SCALE_MEDIUM)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, SCALE_HIGH, VICTORY_STEP_GAP_MS)
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, VICTORY_STEP_GAP_MS)
                    .compose(),
            )
        } else {
            vibrateSafely(vibrator, fallback)
        }
    }

    override fun playButtonPress() {
        val vibrator = resolveVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            vibrator.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK)
        ) {
            vibrateSafely(
                vibrator,
                VibrationEffect.startComposition()
                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, BUTTON_PRESS_SCALE)
                    .compose(),
            )
        } else {
            vibrateSafely(vibrator, VibrationEffect.createOneShot(BUTTON_PRESS_DURATION_MS, BUTTON_PRESS_AMPLITUDE))
        }
    }

    override fun playReleaseSoftTick() {
        val vibrator = resolveVibrator() ?: return
        vibrateSafely(vibrator, VibrationEffect.createOneShot(RELEASE_DURATION_MS, RELEASE_AMPLITUDE))
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun buildStageComposition(stageIndex: Int): VibrationEffect {
        val builder = VibrationEffect.startComposition()
        when (stageIndex) {
            0 -> builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, SCALE_SOFT)
            STAGE_ALERT -> {
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, SCALE_MEDIUM)
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, SCALE_MEDIUM, GAP_SHORT_MS)
            }
            STAGE_STRAINED -> {
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, SCALE_HIGH)
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, SCALE_MEDIUM, GAP_MEDIUM_MS)
            }
            STAGE_CRACKING -> {
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, SCALE_HIGH)
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, SCALE_HIGH, GAP_SHORT_MS)
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, SCALE_HIGH, GAP_SHORT_MS)
            }
            else -> {
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, 1.0f)
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, SCALE_HIGH, GAP_LONG_MS)
                builder.addPrimitive(VibrationEffect.Composition.PRIMITIVE_THUD, SCALE_MEDIUM, GAP_LONG_MS)
            }
        }
        return builder.compose()
    }

    private fun buildStageWaveformFallback(stageIndex: Int): VibrationEffect = when (stageIndex) {
        0 -> VibrationEffect.createOneShot(STAGE_ZERO_DURATION_MS, STAGE_ZERO_AMPLITUDE)
        STAGE_ALERT -> VibrationEffect.createWaveform(STAGE_ALERT_TIMINGS, STAGE_ALERT_AMPLITUDES, -1)
        STAGE_STRAINED -> VibrationEffect.createWaveform(STAGE_STRAINED_TIMINGS, STAGE_STRAINED_AMPLITUDES, -1)
        STAGE_CRACKING -> VibrationEffect.createWaveform(STAGE_CRACKING_TIMINGS, STAGE_CRACKING_AMPLITUDES, -1)
        else -> VibrationEffect.createWaveform(STAGE_ROTTED_TIMINGS, STAGE_ROTTED_AMPLITUDES, -1)
    }

    private fun vibrateSafely(vibrator: Vibrator, effect: VibrationEffect) {
        if (!vibrator.hasVibrator()) return
        try {
            vibrator.vibrate(effect)
        } catch (_: SecurityException) {
            // Vibrator access denied on restricted profile.
        } catch (_: IllegalStateException) {
            // Vibrator service temporarily unavailable.
        }
    }

    private fun resolveVibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
}
