package com.cantar.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random


class MidiReferenceParserTest {
    @Test
    fun parsesTrackNameAndTempoToMilliseconds() {
        val midi = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
            0x00, 0x01, 0x00, 0x01, 0x01, 0xE0.toByte(),
            0x4D, 0x54, 0x72, 0x6B, 0x00, 0x00, 0x00, 0x1C,
            0x00, 0xFF.toByte(), 0x03, 0x04, 0x4C, 0x65, 0x61, 0x64,
            0x00, 0xFF.toByte(), 0x51, 0x03, 0x07, 0xA1.toByte(), 0x20,
            0x00, 0x90.toByte(), 0x3C, 0x5A,
            0x83.toByte(), 0x60, 0x80.toByte(), 0x3C, 0x00,
            0x00, 0xFF.toByte(), 0x2F, 0x00
        )

        val result = MidiReferenceParser.parse(midi)
        val track = result.tracks.single()
        val note = track.referenceNotes.single()

        assertEquals("Lead", track.name)
        assertEquals(480, result.ticksPerQuarter)
        assertEquals(1, track.rawNoteCount)
        assertEquals(0L, note.startMs)
        assertEquals(500L, note.durationMs)
        assertEquals(60, note.midiNote)
    }

    @Test
    fun appliesTempoChangesAcrossMidiTimeline() {
        val midi = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
            0x00, 0x01, 0x00, 0x01, 0x01, 0xE0.toByte(),
            0x4D, 0x54, 0x72, 0x6B, 0x00, 0x00, 0x00, 0x1C,
            0x00, 0xFF.toByte(), 0x51, 0x03, 0x07, 0xA1.toByte(), 0x20,
            0x00, 0x90.toByte(), 0x3C, 0x5A,
            0x83.toByte(), 0x60, 0xFF.toByte(), 0x51, 0x03, 0x0F, 0x42, 0x40,
            0x83.toByte(), 0x60, 0x80.toByte(), 0x3C, 0x00,
            0x00, 0xFF.toByte(), 0x2F, 0x00
        )

        val note = MidiReferenceParser.parse(midi).tracks.single().referenceNotes.single()

        assertEquals(1_500L, note.durationMs)
    }

    @Test
    fun reducesPolyphonicTrackToHighestActiveNote() {
        val midi = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
            0x00, 0x01, 0x00, 0x01, 0x01, 0xE0.toByte(),
            0x4D, 0x54, 0x72, 0x6B, 0x00, 0x00, 0x00, 0x17,
            0x00, 0x90.toByte(), 0x3C, 0x5A,
            0x81.toByte(), 0x70, 0x90.toByte(), 0x43, 0x5A,
            0x81.toByte(), 0x70, 0x80.toByte(), 0x43, 0x00,
            0x81.toByte(), 0x70, 0x80.toByte(), 0x3C, 0x00,
            0x00, 0xFF.toByte(), 0x2F, 0x00
        )

        val notes = MidiReferenceParser.parse(midi).tracks.single().referenceNotes

        assertEquals(3, notes.size)
        assertEquals(60, notes[0].midiNote)
        assertEquals(67, notes[1].midiNote)
        assertEquals(60, notes[2].midiNote)
        assertEquals(250L, notes[0].durationMs)
        assertEquals(250L, notes[1].durationMs)
        assertEquals(250L, notes[2].durationMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsSmpteTimingInsteadOfGuessingTempo() {
        val midi = byteArrayOf(
            0x4D, 0x54, 0x68, 0x64, 0x00, 0x00, 0x00, 0x06,
            0x00, 0x00, 0x00, 0x01, 0xE7.toByte(), 0x28
        )
        MidiReferenceParser.parse(midi)
    }
}

class AudioPitchShiftTest {
    @Test
    fun semitoneShiftChangesPitchWithoutChangingPlaybackSpeed() {
        val lower = AudioPitchShift.playbackParameters(-12)
        val unchanged = AudioPitchShift.playbackParameters(0)
        val higher = AudioPitchShift.playbackParameters(12)

        assertEquals(1f, lower.speed, 0f)
        assertEquals(0.5f, lower.pitch, 0.0001f)
        assertEquals(1f, unchanged.speed, 0f)
        assertEquals(1f, unchanged.pitch, 0.0001f)
        assertEquals(1f, higher.speed, 0f)
        assertEquals(2f, higher.pitch, 0.0001f)
    }

    @Test
    fun clampsPitchShiftToOneOctave() {
        assertEquals(0.5f, AudioPitchShift.playbackParameters(-99).pitch, 0.0001f)
        assertEquals(2f, AudioPitchShift.playbackParameters(99).pitch, 0.0001f)
    }
}

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
    fun findsFundamentalWhenSecondHarmonicIsStronger() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            val time = index.toDouble() / sampleRate
            val value = 3_000.0 * sin(2.0 * PI * 220.0 * time) +
                9_000.0 * sin(2.0 * PI * 440.0 * time) +
                4_000.0 * sin(2.0 * PI * 660.0 * time)
            value.toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(220f, pitch!!.frequencyHz, 4.0f)
    }


    @Test
    fun detectsLowVoiceAt48kWhenSecondHarmonicDominates() {
        val sampleRate = 48_000
        val noise = Random(7)
        val samples = ShortArray(4_096) { index ->
            val time = index.toDouble() / sampleRate
            val value = 1_500.0 * sin(2.0 * PI * 110.0 * time) +
                6_000.0 * sin(2.0 * PI * 220.0 * time) +
                800.0 * sin(2.0 * PI * 330.0 * time) +
                noise.nextInt(-150, 151)
            value.toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(110f, pitch!!.frequencyHz, 2.0f)
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
    fun acceptsLongSustainedReferenceNotesImportedFromMidi() {
        val notes = MelodyReferenceParser.parseCsv("start_ms,duration_ms,midi_note\n0,20000,60")

        assertEquals(20_000L, notes.single().durationMs)
    }

    @Test
    fun transpositionMovesTargetBySemitones() {
        val notes = MelodyReferenceParser.parseCsv("start_ms,duration_ms,midi_note\n0,300,69")
        val scorer = MelodyScorer(notes, transposeSemitones = 2)

        assertEquals(71, scorer.targetAt(100)?.midiNote)
    }


    @Test
    fun usesMedianPitchErrorToLimitSingleFrameOutliers() {
        val notes = MelodyReferenceParser.parseCsv("start_ms,duration_ms,midi_note\n0,500,69")
        val scorer = MelodyScorer(notes)
        scorer.observe(100, PitchAnalyzer.Pitch(440f, 69, "La 4", 0, 0.99f))
        scorer.observe(200, PitchAnalyzer.Pitch(440f, 69, "La 4", 10, 0.99f))
        scorer.observe(300, PitchAnalyzer.Pitch(466.16f, 70, "La♯ 4", 0, 0.99f))

        assertEquals(900, scorer.finish().totalPoints)
    }

    @Test
    fun doesNotAwardPointsWhenNoVoiceWasDetected() {
        val notes = MelodyReferenceParser.parseCsv("start_ms,duration_ms,midi_note\n0,300,69")
        val scorer = MelodyScorer(notes)

        val result = scorer.finish()

        assertEquals(0, result.totalPoints)
        assertEquals(0, result.detectedNotes)
        assertEquals(1, result.evaluatedNotes)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOverlappingMonophonicNotes() {
        MelodyReferenceParser.parseCsv(
            "start_ms,duration_ms,midi_note\n0,500,60\n400,300,62"
        )
    }
}


class PitchTraceContinuityTest {
    @Test
    fun connectsAdjacentVoicedFrames() {
        val previous = PitchTracePoint(100L, 69.0f, true)
        val next = PitchTracePoint(165L, 69.1f, true)

        org.junit.Assert.assertTrue(canConnectPitchTrace(previous, next))
    }

    @Test
    fun breaksLineAtUnvoicedFrame() {
        val previous = PitchTracePoint(100L, 69.0f, true)
        val silence = PitchTracePoint(145L, Float.NaN, false, breakBefore = true)
        val resumed = PitchTracePoint(190L, 69.1f, true)

        org.junit.Assert.assertFalse(canConnectPitchTrace(previous, silence))
        org.junit.Assert.assertFalse(canConnectPitchTrace(silence, resumed))
    }

    @Test
    fun doesNotConnectAcrossLongUnvoicedGap() {
        val previous = PitchTracePoint(100L, 69.0f, true)
        val next = PitchTracePoint(400L, 69.1f, true)

        org.junit.Assert.assertFalse(canConnectPitchTrace(previous, next))
    }

    @Test
    fun rejectsNonIncreasingTraceTimes() {
        val previous = PitchTracePoint(200L, 69.0f, true)
        val next = PitchTracePoint(200L, 69.1f, true)

        org.junit.Assert.assertFalse(canConnectPitchTrace(previous, next))
    }
}
