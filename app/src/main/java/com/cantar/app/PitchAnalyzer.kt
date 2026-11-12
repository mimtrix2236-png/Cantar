package com.cantar.app

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.math.roundToInt

/**
 * YIN fundamental-frequency estimator for monophonic vocal frames.
 * Downsamples by two to reduce work while keeping ample resolution for voice.
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

        val decimation = 2
        val effectiveRate = sampleRate / decimation
        val size = samples.size / decimation
        if (size < 1_024) return null

        val signal = FloatArray(size)
        var mean = 0.0
        var energy = 0.0
        for (i in 0 until size) {
            val centerIndex = i * decimation
            val left = samples[(centerIndex - 1).coerceAtLeast(0)].toFloat()
            val center = samples[centerIndex].toFloat()
            val right = samples[(centerIndex + 1).coerceAtMost(samples.lastIndex)].toFloat()
            // Three-tap low-pass before decimation reduces aliasing from upper harmonics.
            val value = (left * 0.25f + center * 0.5f + right * 0.25f) / 32768f
            signal[i] = value
            mean += value
            energy += value * value
        }
        val rms = sqrt(energy / size).toFloat()
        if (rms < 0.010f) return null

        val dc = (mean / size).toFloat()
        for (i in signal.indices) signal[i] -= dc

        val minFrequency = 65f
        val maxFrequency = 1_000f
        val minTau = (effectiveRate / maxFrequency).toInt().coerceAtLeast(2)
        val maxTau = (effectiveRate / minFrequency).toInt().coerceAtMost(size / 2)
        if (maxTau <= minTau) return null

        // YIN difference function and cumulative mean normalized difference.
        val difference = FloatArray(maxTau + 1)
        val normalized = FloatArray(maxTau + 1)
        for (tau in 1..maxTau) {
            var sum = 0.0
            val limit = size - tau
            var i = 0
            while (i < limit) {
                val delta = signal[i] - signal[i + tau]
                sum += delta * delta
                i++
            }
            difference[tau] = sum.toFloat()
        }

        var runningSum = 0.0
        for (tau in 1..maxTau) {
            runningSum += difference[tau]
            normalized[tau] = if (runningSum > 1e-12) {
                (difference[tau] * tau / runningSum).toFloat()
            } else {
                1f
            }
        }

        // Choose the first convincing local minimum rather than a later harmonic.
        val threshold = 0.16f
        var tau = minTau
        var candidate = -1
        while (tau <= maxTau) {
            if (normalized[tau] < threshold) {
                while (tau < maxTau && normalized[tau + 1] < normalized[tau]) tau++
                candidate = tau
                break
            }
            tau++
        }
        if (candidate < 0) {
            candidate = (minTau..maxTau).minByOrNull { normalized[it] } ?: return null
            if (normalized[candidate] > 0.32f) return null
        }

        // A strong second harmonic can make the first YIN threshold crossing
        // look like an octave-up note. Prefer a convincing double-period minimum
        // when its normalized difference is substantially lower.
        while (candidate * 2 <= maxTau) {
            val doubled = candidate * 2
            val searchStart = (doubled - 2).coerceAtLeast(minTau)
            val searchEnd = (doubled + 2).coerceAtMost(maxTau)
            if (searchStart > searchEnd) break
            val subharmonic = (searchStart..searchEnd).minByOrNull { normalized[it] } ?: break
            if (normalized[subharmonic] < normalized[candidate] * 0.65f &&
                normalized[subharmonic] < 0.20f
            ) {
                candidate = subharmonic
            } else {
                break
            }
        }

        val refinedTau = if (candidate > minTau && candidate < maxTau) {
            val left = normalized[candidate - 1]
            val center = normalized[candidate]
            val right = normalized[candidate + 1]
            val divisor = left - 2f * center + right
            if (abs(divisor) > 1e-7f) {
                candidate + (0.5f * (left - right) / divisor)
            } else candidate.toFloat()
        } else candidate.toFloat()

        if (refinedTau <= 0f) return null
        val frequency = effectiveRate / refinedTau
        if (!frequency.isFinite() || frequency !in minFrequency..maxFrequency) return null

        val midi = (69.0 + 12.0 * ln(frequency / 440.0) / ln(2.0)).roundToInt()
        val exactFrequency = 440.0 * Math.pow(2.0, (midi - 69) / 12.0)
        val cents = (1200.0 * ln(frequency / exactFrequency) / ln(2.0)).roundToInt()
        val octave = midi / 12 - 1
        val confidence = (1f - normalized[candidate]).coerceIn(0f, 1f)

        if (confidence < 0.68f) return null
        return Pitch(
            frequencyHz = frequency,
            midiNote = midi,
            noteName = "${noteNames[Math.floorMod(midi, 12)]} $octave",
            cents = cents,
            confidence = confidence
        )
    }
}
