package com.cantar.app

import kotlin.math.abs
import kotlin.math.roundToInt

data class ReferenceNote(
    val startMs: Long,
    val durationMs: Long,
    val midiNote: Int
) {
    val endMs: Long get() = startMs + durationMs
}

data class PitchTracePoint(
    val timeMs: Long,
    val midiValue: Float,
    val inTune: Boolean
)

data class NoteScore(
    val noteIndex: Int,
    val points: Int,
    val medianErrorCents: Float?,
    val detectedSamples: Int,
    val inTune: Boolean
)

data class ScoringSnapshot(
    val totalPoints: Int = 0,
    val correctNotes: Int = 0,
    val evaluatedNotes: Int = 0,
    val totalNotes: Int = 0,
    val detectedNotes: Int = 0
) {
    val accuracyPercent: Int
        get() = if (evaluatedNotes == 0) 0 else (correctNotes * 100f / evaluatedNotes).roundToInt()
}

/**
 * CSV contract: start_ms,duration_ms,midi_note
 * Times are relative to the beginning of the selected audio file.
 */
object MelodyReferenceParser {
    fun parseCsv(raw: String): List<ReferenceNote> {
        require(raw.isNotBlank()) { "El archivo de melodía está vacío." }
        require(raw.length <= 512_000) { "El archivo supera el límite de 512 KB." }

        val lines = raw.removePrefix("\uFEFF").lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()
        require(lines.size >= 2) { "Se necesita un encabezado y al menos una nota." }

        val delimiter = if (lines.first().contains(';')) ';' else ','
        val header = lines.first().split(delimiter).map { it.trim().lowercase() }
        val startColumn = header.indexOf("start_ms")
        val durationColumn = header.indexOf("duration_ms")
        val endColumn = header.indexOf("end_ms")
        val midiColumn = header.indexOf("midi_note")

        require(startColumn >= 0 && midiColumn >= 0 && (durationColumn >= 0 || endColumn >= 0)) {
            "Encabezado requerido: start_ms,duration_ms,midi_note (o start_ms,end_ms,midi_note)."
        }

        val notes = lines.drop(1).mapIndexedNotNull { index, line ->
            val columns = line.split(delimiter).map { it.trim() }
            if (columns.size < header.size) {
                throw IllegalArgumentException("Fila ${index + 2}: faltan columnas.")
            }
            val start = columns[startColumn].toLongOrNull()
                ?: throw IllegalArgumentException("Fila ${index + 2}: start_ms no es válido.")
            val midi = columns[midiColumn].toIntOrNull()
                ?: throw IllegalArgumentException("Fila ${index + 2}: midi_note debe ser un entero.")
            val duration = if (durationColumn >= 0) {
                columns[durationColumn].toLongOrNull()
                    ?: throw IllegalArgumentException("Fila ${index + 2}: duration_ms no es válido.")
            } else {
                val end = columns[endColumn].toLongOrNull()
                    ?: throw IllegalArgumentException("Fila ${index + 2}: end_ms no es válido.")
                end - start
            }

            require(start >= 0L) { "Fila ${index + 2}: el tiempo no puede ser negativo." }
            require(duration in 40L..15_000L) { "Fila ${index + 2}: duración fuera de rango (40–15000 ms)." }
            require(midi in 0..127) { "Fila ${index + 2}: MIDI debe estar entre 0 y 127." }
            ReferenceNote(start, duration, midi)
        }.sortedBy { it.startMs }

        require(notes.isNotEmpty()) { "No se encontraron notas válidas." }
        require(notes.size <= 10_000) { "La melodía supera las 10.000 notas." }
        for (index in 1 until notes.size) {
            require(notes[index].startMs >= notes[index - 1].endMs) {
                "Las notas se superponen en las filas de datos; la referencia debe ser monofónica."
            }
        }
        return notes
    }
}

class MelodyScorer(
    referenceNotes: List<ReferenceNote>,
    transposeSemitones: Int = 0,
    private val toleranceCents: Float = 50f
) {
    private val notes = referenceNotes.map {
        it.copy(midiNote = (it.midiNote + transposeSemitones).coerceIn(0, 127))
    }
    private val samples = Array(notes.size) { mutableListOf<Float>() }
    private val scores = mutableListOf<NoteScore>()
    private val finalized = BooleanArray(notes.size)
    private var nextToFinalize = 0
    private var lastSampleAtMs = Long.MIN_VALUE

    fun targetAt(positionMs: Long): ReferenceNote? {
        var low = 0
        var high = notes.lastIndex
        while (low <= high) {
            val middle = (low + high) ushr 1
            if (notes[middle].startMs <= positionMs) low = middle + 1 else high = middle - 1
        }
        val candidate = notes.getOrNull(high) ?: return null
        return candidate.takeIf { positionMs < it.endMs }
    }

    fun observe(positionMs: Long, pitch: PitchAnalyzer.Pitch?): ScoringSnapshot {
        finalizeExpired(positionMs)
        val target = targetAt(positionMs)
        if (target != null && pitch != null && pitch.confidence >= 0.68f &&
            (lastSampleAtMs == Long.MIN_VALUE || positionMs - lastSampleAtMs >= 65L)
        ) {
            val index = notes.binarySearchBy(target.startMs) { it.startMs }.coerceAtLeast(0)
            val error = (pitch.midiNote - target.midiNote) * 100f + pitch.cents
            samples[index].add(error)
            lastSampleAtMs = positionMs
        }
        return snapshot()
    }

    fun finish(): ScoringSnapshot {
        while (nextToFinalize < notes.size) {
            finalize(nextToFinalize)
            nextToFinalize++
        }
        return snapshot()
    }

    fun snapshot(): ScoringSnapshot = ScoringSnapshot(
        totalPoints = if (notes.isEmpty()) 0 else (scores.sumOf { it.points } * 10f / notes.size).roundToInt(),
        correctNotes = scores.count { it.inTune },
        evaluatedNotes = scores.size,
        totalNotes = notes.size,
        detectedNotes = scores.count { it.detectedSamples > 0 }
    )

    private fun finalizeExpired(positionMs: Long) {
        while (nextToFinalize < notes.size && notes[nextToFinalize].endMs <= positionMs) {
            finalize(nextToFinalize)
            nextToFinalize++
        }
    }

    private fun finalize(index: Int) {
        if (finalized[index]) return
        finalized[index] = true
        val values = samples[index].map { abs(it) }.sorted()
        val median = when {
            values.isEmpty() -> null
            values.size % 2 == 1 -> values[values.size / 2]
            else -> (values[values.size / 2 - 1] + values[values.size / 2]) / 2f
        }
        val points = if (median == null) 0 else {
            ((1f - median / (toleranceCents * 2f)).coerceIn(0f, 1f) * 100f).roundToInt()
        }
        scores.add(
            NoteScore(
                noteIndex = index,
                points = points,
                medianErrorCents = median,
                detectedSamples = values.size,
                inTune = median != null && median <= toleranceCents
            )
        )
    }
}

fun midiNoteName(midi: Int): String {
    val names = arrayOf("Do", "Do♯", "Re", "Re♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")
    return "${names[Math.floorMod(midi, 12)]} ${midi / 12 - 1}"
}
