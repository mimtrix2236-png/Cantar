package com.cantar.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class PitchAnalyzerTest {
    @Test
    fun detects440HzTone() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 440.0 * index / sampleRate) * 12_000.0).toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(440f, pitch!!.frequencyHz, 3.0f)
        assertEquals(69, pitch.midiNote)
    }

    @Test
    fun rejectsSilence() {
        assertNull(PitchAnalyzer.analyze(ShortArray(4_096), 44_100))
    }
}

class MelodyReferenceTest {
    @Test
    fun parsesTimedNotesAndScoresAccuratePitch() {
        val notes = MelodyReferenceParser.parseCsv(
            "start_ms,duration_ms,midi_note\n0,300,69\n350,250,71"
        )
        val scorer = MelodyScorer(notes)
        val la = PitchAnalyzer.Pitch(440f, 69, "La 4", 0, 0.99f)
        val si = PitchAnalyzer.Pitch(493.88f, 71, "Si 4", 0, 0.99f)

        scorer.observe(100, la)
        scorer.observe(400, si)
        val result = scorer.finish()

        assertEquals(1_000, result.totalPoints)
        assertEquals(2, result.correctNotes)
        assertEquals(2, result.evaluatedNotes)
        assertEquals(2, result.detectedNotes)
    }

    @Test
    fun appliesFiftyCentToleranceToEachNote() {
        val notes = MelodyReferenceParser.parseCsv(
            "start_ms,duration_ms,midi_note\n0,250,69\n300,250,69"
        )
        val scorer = MelodyScorer(notes)
        scorer.observe(100, PitchAnalyzer.Pitch(440f, 69, "La 4", 50, 0.99f))
        scorer.observe(400, PitchAnalyzer.Pitch(466.16f, 70, "La♯ 4", 0, 0.99f))

        val result = scorer.finish()

        assertEquals(250, result.totalPoints)
        assertEquals(1, result.correctNotes)
        assertEquals(2, result.evaluatedNotes)
    }

    @Test
    fun transpositionMovesTargetBySemitones() {
        val notes = MelodyReferenceParser.parseCsv("start_ms,duration_ms,midi_note\n0,300,69")
        val scorer = MelodyScorer(notes, transposeSemitones = 2)

        assertEquals(71, scorer.targetAt(100)?.midiNote)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverlappingMonophonicNotes() {
        MelodyReferenceParser.parseCsv(
            "start_ms,duration_ms,midi_note\n0,500,60\n400,300,62"
        )
    }
}
