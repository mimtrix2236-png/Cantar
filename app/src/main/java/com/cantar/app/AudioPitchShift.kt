package com.cantar.app

import androidx.media3.common.PlaybackParameters
import kotlin.math.pow

/**
 * Semitone transposition that preserves the media playback rate.
 *
 * Media3 receives speed = 1.0 for every value; only the pitch factor changes.
 */
object AudioPitchShift {
    const val MIN_SEMITONES = -12
    const val MAX_SEMITONES = 12

    fun clampSemitones(semitones: Int): Int =
        semitones.coerceIn(MIN_SEMITONES, MAX_SEMITONES)

    fun playbackParameters(semitones: Int): PlaybackParameters {
        val bounded = clampSemitones(semitones)
        val pitchFactor = 2.0.pow(bounded / 12.0).toFloat()
        return PlaybackParameters(1f, pitchFactor)
    }
}
