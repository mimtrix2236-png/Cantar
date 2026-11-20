package com.cantar.app

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class OverlappedPcmFrameAssemblerTest {
    @Test
    fun preservesEverySampleAcrossFragmentedReadsAndOverlappingWindows() {
        val source = ShortArray(24) { it.toShort() }
        val assembler = OverlappedPcmFrameAssembler(frameSize = 8, overlap = 4)
        val frames = mutableListOf<Pair<Long, ShortArray>>()
        val chunkSizes = listOf(3, 7, 1, 6, 2, 5)
        var offset = 0

        chunkSizes.forEach { chunkSize ->
            assembler.append(source, offset, chunkSize) { samples, center ->
                frames += center to samples.copyOf()
            }
            offset += chunkSize
        }

        assertEquals(source.size, offset)
        assertEquals(listOf(4L, 8L, 12L, 16L, 20L), frames.map { it.first })
        assertArrayEquals(ShortArray(8) { it.toShort() }, frames[0].second)
        assertArrayEquals(ShortArray(8) { (it + 4).toShort() }, frames[1].second)
        assertArrayEquals(ShortArray(8) { (it + 8).toShort() }, frames[2].second)
        assertArrayEquals(ShortArray(8) { (it + 12).toShort() }, frames[3].second)
        assertArrayEquals(ShortArray(8) { (it + 16).toShort() }, frames[4].second)
    }

    @Test
    fun doesNotEmitIncompleteFrameAndRetainsSamplesForNextRead() {
        val assembler = OverlappedPcmFrameAssembler(frameSize = 8, overlap = 4)
        var emitted = 0

        assembler.append(ShortArray(3)) { _, _ -> emitted++ }
        assembler.append(ShortArray(4) { (it + 3).toShort() }) { _, _ -> emitted++ }
        assertEquals(0, emitted)

        assembler.append(shortArrayOf(7)) { samples, center ->
            emitted++
            assertEquals(4L, center)
            assertArrayEquals(ShortArray(8) { it.toShort() }, samples)
        }
        assertEquals(1, emitted)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsInputRangeOutsideArray() {
        val assembler = OverlappedPcmFrameAssembler(frameSize = 8, overlap = 4)
        assembler.append(ShortArray(4), offset = 4, length = 1) { _, _ -> }
    }
}
