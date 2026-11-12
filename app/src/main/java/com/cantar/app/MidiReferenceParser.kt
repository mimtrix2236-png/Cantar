package com.cantar.app

import kotlin.math.roundToLong

data class MidiTrackOption(
    val index: Int,
    val name: String,
    val rawNoteCount: Int,
    val referenceNotes: List<ReferenceNote>
)

data class MidiImportResult(
    val format: Int,
    val ticksPerQuarter: Int,
    val tracks: List<MidiTrackOption>
)

/**
 * Standard MIDI File (SMF) reader for format 0/1, PPQ timing and note events.
 * Polyphonic tracks are reduced to a monophonic guide by following the highest
 * currently active note. The user chooses the source track; no song is inferred.
 */
object MidiReferenceParser {
    private data class RawNote(val startTick: Long, val endTick: Long, val midi: Int)
    private data class TempoChange(val tick: Long, val microsPerQuarter: Int, val order: Int)
    private data class TempoSegment(val tick: Long, val millis: Double, val microsPerQuarter: Int)
    private data class Boundary(val tick: Long, val midi: Int, val starts: Boolean)

    private class Cursor(
        private val data: ByteArray,
        var position: Int,
        private val limit: Int
    ) {
        fun readU8(): Int {
            require(position < limit) { "El archivo MIDI está truncado." }
            return data[position++].toInt() and 0xFF
        }

        fun readU16(): Int = (readU8() shl 8) or readU8()

        fun readU32(): Long =
            (readU8().toLong() shl 24) or
                (readU8().toLong() shl 16) or
                (readU8().toLong() shl 8) or
                readU8().toLong()

        fun readAscii(length: Int): String {
            require(length >= 0 && position + length <= limit) { "El archivo MIDI está truncado." }
            val value = String(data, position, length, Charsets.US_ASCII)
            position += length
            return value
        }

        fun readBytes(length: Int): ByteArray {
            require(length >= 0 && position + length <= limit) { "El archivo MIDI está truncado." }
            val value = data.copyOfRange(position, position + length)
            position += length
            return value
        }

        fun skip(length: Int) {
            require(length >= 0 && position + length <= limit) { "El archivo MIDI está truncado." }
            position += length
        }

        fun hasRemaining(): Boolean = position < limit

        fun readVariableLength(): Long {
            var value = 0L
            repeat(4) {
                val byte = readU8()
                value = (value shl 7) or (byte and 0x7F).toLong()
                if (byte and 0x80 == 0) return value
            }
            throw IllegalArgumentException("Valor MIDI de longitud variable inválido.")
        }
    }

    private data class ParsedTrack(
        val name: String,
        val notes: List<RawNote>,
        val tempos: List<TempoChange>
    )

    fun parse(bytes: ByteArray): MidiImportResult {
        require(bytes.size in 14..5_242_880) {
            "El MIDI debe ocupar entre 14 bytes y 5 MB."
        }
        val header = Cursor(bytes, 0, bytes.size)
        require(header.readAscii(4) == "MThd") { "El archivo no tiene una cabecera MIDI válida." }
        val headerLength = header.readU32()
        require(headerLength in 6..1024 && 8L + headerLength <= bytes.size) {
            "La cabecera MIDI está incompleta o no es válida."
        }
        val headerData = Cursor(bytes, 8, 8 + headerLength.toInt())
        val format = headerData.readU16()
        val trackCount = headerData.readU16()
        val division = headerData.readU16()
        require(format == 0 || format == 1) {
            "Solo se admiten MIDI tipo 0 o 1; el tipo 2 contiene secuencias independientes."
        }
        require(trackCount in 1..64 && (format != 0 || trackCount == 1)) {
            "La cantidad de pistas MIDI no es válida."
        }
        require(division in 1..0x7FFF) {
            "El MIDI usa tiempo SMPTE. Exportalo con resolución PPQ para sincronizarlo."
        }

        val fileCursor = Cursor(bytes, 8 + headerLength.toInt(), bytes.size)
        val parsedTracks = mutableListOf<ParsedTrack>()
        var tempoOrder = 0
        repeat(trackCount) { trackIndex ->
            require(fileCursor.readAscii(4) == "MTrk") {
                "No se encontró la pista MIDI ${trackIndex + 1}."
            }
            val trackLength = fileCursor.readU32()
            require(trackLength <= Int.MAX_VALUE && fileCursor.position.toLong() + trackLength <= bytes.size) {
                "La pista MIDI ${trackIndex + 1} está truncada."
            }
            val trackEnd = fileCursor.position + trackLength.toInt()
            val trackCursor = Cursor(bytes, fileCursor.position, trackEnd)
            val parsed = parseTrack(trackCursor, trackIndex, tempoOrder)
            tempoOrder += parsed.tempos.size
            parsedTracks += parsed
            fileCursor.position = trackEnd
        }

        val tempos = parsedTracks.flatMap { it.tempos }
            .sortedWith(compareBy<TempoChange> { it.tick }.thenBy { it.order })
        val tempoSegments = buildTempoSegments(tempos, division)
        val options = parsedTracks.mapIndexedNotNull { index, track ->
            val references = toMonophonicReference(track.notes, tempoSegments, division)
            if (references.isEmpty()) null else MidiTrackOption(
                index = index,
                name = track.name,
                rawNoteCount = track.notes.size,
                referenceNotes = references
            )
        }
        require(options.isNotEmpty()) {
            "El MIDI no contiene notas utilizables. Revisá que tenga eventos de nota y duración."
        }
        return MidiImportResult(format, division, options)
    }

    private fun parseTrack(cursor: Cursor, index: Int, orderStart: Int): ParsedTrack {
        var name = "Pista ${index + 1}"
        var tick = 0L
        var runningStatus = 0
        var order = orderStart
        val notes = mutableListOf<RawNote>()
        val tempos = mutableListOf<TempoChange>()
        val active = mutableMapOf<Pair<Int, Int>, java.util.ArrayDeque<Long>>()

        while (cursor.hasRemaining()) {
            tick += cursor.readVariableLength()
            val first = cursor.readU8()
            val status: Int
            val firstData: Int?
            if (first >= 0x80) {
                status = first
                firstData = null
            } else {
                require(runningStatus in 0x80..0xEF) {
                    "Evento MIDI sin estado previo en la pista ${index + 1}."
                }
                status = runningStatus
                firstData = first
            }

            when (status) {
                0xFF -> {
                    val type = cursor.readU8()
                    val length = cursor.readVariableLength()
                    require(length <= Int.MAX_VALUE) { "Evento MIDI demasiado grande." }
                    val payload = cursor.readBytes(length.toInt())
                    when (type) {
                        0x03 -> {
                            val decoded = String(payload, Charsets.UTF_8).trim()
                            if (decoded.isNotBlank()) name = decoded.take(80)
                        }
                        0x51 -> if (payload.size == 3) {
                            val tempo = ((payload[0].toInt() and 0xFF) shl 16) or
                                ((payload[1].toInt() and 0xFF) shl 8) or
                                (payload[2].toInt() and 0xFF)
                            if (tempo > 0) tempos += TempoChange(tick, tempo, order++)
                        }
                        0x2F -> break
                    }
                }
                0xF0, 0xF7 -> {
                    val length = cursor.readVariableLength()
                    require(length <= Int.MAX_VALUE) { "Evento SysEx demasiado grande." }
                    cursor.skip(length.toInt())
                    runningStatus = 0
                }
                else -> {
                    require(status in 0x80..0xEF) { "Evento de sistema MIDI no compatible." }
                    runningStatus = status
                    val command = status and 0xF0
                    val channel = status and 0x0F
                    val data1 = firstData ?: cursor.readU8()
                    val data2 = if (command == 0xC0 || command == 0xD0) 0 else cursor.readU8()
                    if (command == 0x80 || (command == 0x90 && data2 == 0)) {
                        val key = channel to data1
                        val starts = active[key]
                        if (starts != null && starts.isNotEmpty()) {
                            val startTick = starts.removeFirst()
                            if (tick > startTick) notes += RawNote(startTick, tick, data1)
                            if (starts.isEmpty()) active.remove(key)
                        }
                    } else if (command == 0x90 && data2 > 0) {
                        val key = channel to data1
                        active.getOrPut(key) { java.util.ArrayDeque() }.addLast(tick)
                    }
                    require(notes.size <= 100_000) { "La pista MIDI supera las 100.000 notas." }
                }
            }
        }
        return ParsedTrack(name, notes.sortedBy { it.startTick }, tempos)
    }

    private fun buildTempoSegments(
        changes: List<TempoChange>,
        ppq: Int
    ): List<TempoSegment> {
        val result = mutableListOf(TempoSegment(0L, 0.0, 500_000))
        var currentTick = 0L
        var currentMillis = 0.0
        var currentTempo = 500_000
        changes.forEach { change ->
            if (change.tick < currentTick) return@forEach
            currentMillis += (change.tick - currentTick) * currentTempo / (ppq * 1_000.0)
            currentTick = change.tick
            currentTempo = change.microsPerQuarter
            result += TempoSegment(currentTick, currentMillis, currentTempo)
        }
        return result
    }

    private fun toMonophonicReference(
        rawNotes: List<RawNote>,
        tempoSegments: List<TempoSegment>,
        ppq: Int
    ): List<ReferenceNote> {
        if (rawNotes.isEmpty()) return emptyList()
        fun toMillis(tick: Long): Long {
            val segment = tempoSegments.lastOrNull { it.tick <= tick } ?: tempoSegments.first()
            return (segment.millis + (tick - segment.tick) * segment.microsPerQuarter /
                (ppq * 1_000.0)).roundToLong().coerceAtLeast(0L)
        }

        val boundaries = buildList {
            rawNotes.forEach { note ->
                add(Boundary(note.startTick, note.midi, true))
                add(Boundary(note.endTick, note.midi, false))
            }
        }.sortedWith(compareBy<Boundary> { it.tick }.thenBy { if (it.starts) 1 else 0 })

        val activeCounts = mutableMapOf<Int, Int>()
        val output = mutableListOf<ReferenceNote>()
        var currentMidi: Int? = null
        var segmentStart = 0L
        var index = 0
        while (index < boundaries.size) {
            val tick = boundaries[index].tick
            var end = index
            while (end < boundaries.size && boundaries[end].tick == tick) end++
            for (eventIndex in index until end) {
                val event = boundaries[eventIndex]
                val count = activeCounts[event.midi] ?: 0
                if (event.starts) activeCounts[event.midi] = count + 1
                else if (count <= 1) activeCounts.remove(event.midi)
                else activeCounts[event.midi] = count - 1
            }
            val nextMidi = activeCounts.keys.maxOrNull()
            if (nextMidi != currentMidi) {
                if (currentMidi != null && tick > segmentStart) {
                    val startMs = toMillis(segmentStart)
                    val endMs = toMillis(tick)
                    val duration = endMs - startMs
                    if (duration >= 40L) output += ReferenceNote(startMs, duration, currentMidi)
                }
                currentMidi = nextMidi
                segmentStart = tick
            }
            index = end
        }
        val finalTick = rawNotes.maxOf { it.endTick }
        if (currentMidi != null && finalTick > segmentStart) {
            val startMs = toMillis(segmentStart)
            val endMs = toMillis(finalTick)
            val duration = endMs - startMs
            if (duration >= 40L) output += ReferenceNote(startMs, duration, currentMidi)
        }
        return output.take(10_000)
    }
}
