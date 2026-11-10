package com.cantar.app

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Lightweight monophonic pitch estimator for PCM microphone frames.
 * Uses normalized autocorrelation; returns null for silence or an unclear period.
 */
object PitchAnalyzer {
    data class Pitch(
        val frequencyHz: Float,
        val midiNote: Int,
        val noteName: String,
        val cents: Int,
        val confidence: Float
    )

    private val noteNames = arrayOf(
        "Do", "Do♯", "Re", "Re♯", "Mi", "Fa",
        "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si"
    )

    fun analyze(samples: ShortArray, sampleRate: Int = 44_100): Pitch? {
        if (samples.size < 2_048 || sampleRate <= 0) return null

        val frame = FloatArray(samples.size)
        var energy = 0.0
        for (i in samples.indices) {
            val value = samples[i].toFloat() / 32768f
            frame[i] = value
            energy += value * value
        }
        val rms = sqrt(energy / samples.size).toFloat()
        if (rms < 0.012f) return null

        // Remove DC offset and apply a Hann window to reduce edge artifacts.
        val mean = frame.average().toFloat()
        for (i in frame.indices) {
            val window = 0.5f - 0.5f * kotlin.math.cos(
                (2.0 * Math.PI * i / (frame.size - 1)).toFloat()
            )
            frame[i] = (frame[i] - mean) * window
        }

        val minFrequency = 65f
        val maxFrequency = 1_000f
        val minLag = (sampleRate / maxFrequency).toInt().coerceAtLeast(2)
        val maxLag = (sampleRate / minFrequency).toInt().coerceAtMost(frame.size / 2)
        var bestLag = -1
        var bestScore = 0f

        for (lag in minLag..maxLag) {
            var cross = 0.0
            var left = 0.0
            var right = 0.0
            val limit = frame.size - lag
            var i = 0
            while (i < limit) {
                val a = frame[i].toDouble()
                val b = frame[i + lag].toDouble()
                cross += a * b
                left += a * a
                right += b * b
                i++
            }
            val denominator = sqrt(left * right)
            val score = if (denominator > 1e-12) (cross / denominator).toFloat() else 0f
            if (score > bestScore) {
                bestScore = score
                bestLag = lag
            }
        }

        if (bestLag < 0 || bestScore < 0.72f) return null

        // Parabolic interpolation improves resolution between integer sample lags.
        fun correlation(lag: Int): Double {
            var sum = 0.0
            val limit = frame.size - lag
            for (i in 0 until limit) sum += frame[i].toDouble() * frame[i + lag].toDouble()
            return sum
        }
        val refinedLag = if (bestLag > minLag && bestLag < maxLag) {
            val a = correlation(bestLag - 1)
            val b = correlation(bestLag)
            val c = correlation(bestLag + 1)
            val divisor = a - 2.0 * b + c
            if (abs(divisor) > 1e-12) bestLag + 0.5 * (a - c) / divisor else bestLag.toDouble()
        } else bestLag.toDouble()

        val frequency = (sampleRate / refinedLag).toFloat()
        if (!frequency.isFinite() || frequency !in minFrequency..maxFrequency) return null

        val midi = (69.0 + 12.0 * ln(frequency / 440.0) / ln(2.0)).toInt()
        val exactFrequency = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
        val cents = (1200.0 * ln(frequency / exactFrequency) / ln(2.0)).toInt()
        val octave = midi / 12 - 1
        val name = "${noteNames[Math.floorMod(midi, 12)]} ${octave}"

        return Pitch(
            frequencyHz = frequency,
            midiNote = midi,
            noteName = name,
            cents = cents,
            confidence = bestScore.coerceIn(0f, 1f)
        )
    }
}
