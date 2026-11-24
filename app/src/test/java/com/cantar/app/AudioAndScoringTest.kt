package com.cantar.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random
import java.io.ByteArrayInputStream


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
    fun preservesPlaybackSpeedAndExactPitchRatioAcrossEverySupportedSemitone() {
        for (semitones in AudioPitchShift.MIN_SEMITONES..AudioPitchShift.MAX_SEMITONES) {
            val parameters = AudioPitchShift.playbackParameters(semitones)
            val expectedPitch = Math.pow(2.0, semitones / 12.0).toFloat()

            assertEquals("Playback speed at $semitones semitones", 1f, parameters.speed, 0f)
            assertEquals("Pitch factor at $semitones semitones", expectedPitch, parameters.pitch, 0.0001f)
        }
    }

    @Test
    fun clampsPitchShiftToOneOctave() {
        assertEquals(0.5f, AudioPitchShift.playbackParameters(-99).pitch, 0.0001f)
        assertEquals(2f, AudioPitchShift.playbackParameters(99).pitch, 0.0001f)
    }
}

class PitchAnalyzerTest {
    @Test
    fun rejectsConstantDcOffsetWithoutVoice() {
        assertNull(PitchAnalyzer.analyze(ShortArray(4_096) { 6_000 }, 44_100))
    }

    @Test
    fun detectsQuietToneDespiteLargeDcBias() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            (6_000.0 + 900.0 * sin(2.0 * PI * 440.0 * index / sampleRate)).toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(440f, pitch!!.frequencyHz, 3.0f)
    }

    @Test
    fun doesNotMutateTheReusableCaptureFrame() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 440.0 * index / sampleRate) * 8_000.0).toInt().toShort()
        }
        val original = samples.copyOf()

        PitchAnalyzer.analyze(samples, sampleRate)

        assertArrayEquals(original, samples)
    }

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
    fun detectsLowBassAt55HzAt48k() {
        val sampleRate = 48_000
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 55.0 * index / sampleRate) * 12_000.0).toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(55f, pitch!!.frequencyHz, 1.0f)
    }

    @Test
    fun detectsMidVoiceAt22050SampleRateFallback() {
        val sampleRate = 22_050
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 220.0 * index / sampleRate) * 12_000.0).toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(220f, pitch!!.frequencyHz, 3.0f)
    }

    @Test
    fun detects1200HzUpperBoundaryWithoutOctaveDown() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 1_200.0 * index / sampleRate) * 12_000.0).toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(1_200f, pitch!!.frequencyHz, 2.0f)
    }

    @Test
    fun rejectsToneAboveUpperFrequencyLimit() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 1_225.0 * index / sampleRate) * 12_000.0).toInt().toShort()
        }

        assertNull(PitchAnalyzer.analyze(samples, sampleRate))
    }

    @Test
    fun detectsHighSopranoAt1175Hz() {
        val sampleRate = 44_100
        val samples = ShortArray(4_096) { index ->
            (sin(2.0 * PI * 1_174.66 * index / sampleRate) * 12_000.0).toInt().toShort()
        }

        val pitch = PitchAnalyzer.analyze(samples, sampleRate)

        assertNotNull(pitch)
        assertEquals(1_174.66f, pitch!!.frequencyHz, 3.0f)
    }

    @Test
    fun rejectsBroadbandNoiseWithoutStableFundamental() {
        val random = Random(42)
        val samples = ShortArray(4_096) { random.nextInt(-10_000, 10_001).toShort() }

        assertNull(PitchAnalyzer.analyze(samples, 44_100))
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
    fun parsesEndMsColumnAndRequiresIncreasingTimeline() {
        val note = MelodyReferenceParser.parseCsv("start_ms,end_ms,midi_note\n100,350,60").single()
        assertEquals(100L, note.startMs)
        assertEquals(250L, note.durationMs)
        assertEquals(350L, note.endMs)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEndTimeNotAfterStart() {
        MelodyReferenceParser.parseCsv("start_ms,end_ms,midi_note\n100,99,60")
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsReferenceEndTimeOverflow() {
        MelodyReferenceParser.parseCsv("start_ms,duration_ms,midi_note\n9223372036854775800,40,60")
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
    fun scoresFirstSampleOfShortAdjacentNoteIndependently() {
        val notes = MelodyReferenceParser.parseCsv(
            "start_ms,duration_ms,midi_note\n0,40,69\n40,40,71"
        )
        val scorer = MelodyScorer(notes)
        scorer.observe(15, PitchAnalyzer.Pitch(440f, 69, "La 4", 0, 0.99f))
        scorer.observe(55, PitchAnalyzer.Pitch(493.88f, 71, "Si 4", 0, 0.99f))

        val result = scorer.finish()

        assertEquals(1_000, result.totalPoints)
        assertEquals(2, result.detectedNotes)
        assertEquals(2, result.correctNotes)
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
    fun breakBeforeFlagOnlyBreaksTheIncomingSegment() {
        val before = PitchTracePoint(50L, 68.9f, true)
        val firstAfterGap = PitchTracePoint(100L, 69.0f, true, breakBefore = true)
        val next = PitchTracePoint(165L, 69.1f, true)

        org.junit.Assert.assertFalse(canConnectPitchTrace(before, firstAfterGap))
        org.junit.Assert.assertTrue(canConnectPitchTrace(firstAfterGap, next))
    }

    @Test
    fun rejectsNonPositiveMaximumGap() {
        val previous = PitchTracePoint(100L, 69.0f, true)
        val next = PitchTracePoint(165L, 69.1f, true)

        org.junit.Assert.assertFalse(canConnectPitchTrace(previous, next, maxGapMs = 0L))
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
    fun showsLivePitchMarkerOnVoicedFrameAfterGap() {
        val resumed = PitchTracePoint(190L, 69.1f, true, breakBefore = true)

        org.junit.Assert.assertEquals(resumed, latestVoicedTracePoint(listOf(resumed)))
    }

    @Test
    fun hidesLivePitchMarkerAfterUnvoicedFrame() {
        val voiced = PitchTracePoint(100L, 69.0f, true)
        val silence = PitchTracePoint(145L, Float.NaN, false, breakBefore = true)

        org.junit.Assert.assertEquals(voiced, latestVoicedTracePoint(listOf(voiced)))
        org.junit.Assert.assertNull(latestVoicedTracePoint(listOf(voiced, silence)))
    }

    @Test
    fun rejectsNonIncreasingTraceTimes() {
        val previous = PitchTracePoint(200L, 69.0f, true)
        val next = PitchTracePoint(200L, 69.1f, true)

        org.junit.Assert.assertFalse(canConnectPitchTrace(previous, next))
    }

    @Test
    fun compensatesPitchFrameCenterWithoutNegativeTimeline() {
        assertEquals(954L, compensatePitchFrameTime(1_000L, 46L))
        assertEquals(0L, compensatePitchFrameTime(20L, 46L))
        assertEquals(1_000L, compensatePitchFrameTime(1_000L, -10L))
    }

    @Test
    fun alignsPitchFrameUsingMonotonicCaptureTimestamp() {
        assertEquals(
            954L,
            alignPlaybackPositionToCaptureTime(
                playbackPositionAtCallbackMs = 1_000L,
                callbackTimeNanos = 1_000_000_000L,
                captureCenterTimeNanos = 954_000_000L
            )
        )
        assertEquals(
            0L,
            alignPlaybackPositionToCaptureTime(
                playbackPositionAtCallbackMs = 20L,
                callbackTimeNanos = 1_000_000_000L,
                captureCenterTimeNanos = 954_000_000L
            )
        )
    }

    @Test
    fun doesNotSubtractTimeWhenCaptureTimestampIsInFuture() {
        assertEquals(
            1_000L,
            alignPlaybackPositionToCaptureTime(
                playbackPositionAtCallbackMs = 1_000L,
                callbackTimeNanos = 1_000_000_000L,
                captureCenterTimeNanos = 1_100_000_000L
            )
        )
    }
}

class PlaybackEndPolicyTest {
    @Test
    fun autoFinalizesOnlyAnEndedActiveKaraokeRound() {
        org.junit.Assert.assertTrue(
            shouldAutoFinalizePlayback(
                isSinging = true,
                playbackEnded = true,
                roundFinalizing = false
            )
        )
    }

    @Test
    fun doesNotFinalizeSetupOrAlreadyFinalizedRounds() {
        org.junit.Assert.assertFalse(shouldAutoFinalizePlayback(false, true, false))
        org.junit.Assert.assertFalse(shouldAutoFinalizePlayback(true, false, false))
        org.junit.Assert.assertFalse(shouldAutoFinalizePlayback(true, true, true))
    }
}


class BoundedTextImportTest {
    @Test
    fun readsUtf8TextAtTheConfiguredByteLimit() {
        val content = "start_ms,duration_ms,midi_note\\n0,250,69"
        val bytes = content.toByteArray(Charsets.UTF_8)

        assertEquals(content, readBoundedUtf8(ByteArrayInputStream(bytes), bytes.size))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsTextThatExceedsTheConfiguredByteLimit() {
        readBoundedUtf8(ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)), 3)
    }
}
