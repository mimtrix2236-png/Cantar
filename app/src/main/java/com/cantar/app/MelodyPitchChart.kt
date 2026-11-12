package com.cantar.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MelodyPitchChart(
    referenceNotes: List<ReferenceNote>,
    trace: List<PitchTracePoint>,
    currentPositionMs: Long,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(178.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF080F29)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val windowStart = (currentPositionMs - 2_000L).coerceAtLeast(0L)
            val windowEnd = windowStart + 8_000L
            val visibleNotes = referenceNotes.filter {
                it.endMs >= windowStart && it.startMs <= windowEnd
            }
            val visibleTrace = trace.filter {
                it.timeMs in windowStart..windowEnd
            }.sortedBy { it.timeMs }

            // Include detected pitches in the vertical range so a badly missed note
            // remains visible instead of being silently clipped at the chart edge.
            val minReference = (visibleNotes.minOfOrNull { it.midiNote } ?: 48) - 4
            val minVoice = (visibleTrace.minOfOrNull { it.midiValue }?.toInt() ?: 48) - 2
            val minMidi = minOf(minReference, minVoice).coerceAtLeast(0)
            val maxReference = (visibleNotes.maxOfOrNull { it.midiNote } ?: 84) + 4
            val maxVoice = (visibleTrace.maxOfOrNull { it.midiValue }?.toInt() ?: 84) + 2
            val maxMidi = maxOf(maxReference, maxVoice).coerceAtMost(127).coerceAtLeast(minMidi + 8)

            fun xFor(timeMs: Long): Float =
                ((timeMs - windowStart).toFloat() / (windowEnd - windowStart).toFloat())
                    .coerceIn(0f, 1f) * size.width

            fun yFor(midi: Float): Float =
                size.height - ((midi - minMidi) / (maxMidi - minMidi).toFloat())
                    .coerceIn(0f, 1f) * size.height

            for (step in 0..8) {
                val y = size.height * step / 8f
                drawLine(
                    color = Color(0xFF263454),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            visibleNotes.forEach { note ->
                val left = xFor(note.startMs)
                val right = xFor(note.endMs)
                val top = yFor(note.midiNote + 0.42f)
                val bottom = yFor(note.midiNote - 0.42f)
                val isCurrent = currentPositionMs in note.startMs until note.endMs
                drawRoundRect(
                    color = if (isCurrent) Color(0xFF19E66B) else Color(0xFF00DFFF),
                    topLeft = Offset(left, top),
                    size = Size((right - left).coerceAtLeast(2f), (bottom - top).coerceAtLeast(4f)),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx()),
                    alpha = if (isCurrent) 0.95f else 0.52f
                )
            }

            visibleTrace.zipWithNext().forEach { (previous, next) ->
                val gapMs = next.timeMs - previous.timeMs
                // Do not draw a misleading straight line across silence or unvoiced frames.
                if (gapMs in 1L..220L) {
                    drawLine(
                        color = if (next.inTune) Color(0xFF24F276) else Color(0xFFFFA52E),
                        start = Offset(xFor(previous.timeMs), yFor(previous.midiValue)),
                        end = Offset(xFor(next.timeMs), yFor(next.midiValue)),
                        strokeWidth = 3.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            }
            visibleTrace.lastOrNull()?.let { point ->
                drawCircle(
                    color = if (point.inTune) Color(0xFF24F276) else Color(0xFFFFA52E),
                    radius = 5.dp.toPx(),
                    center = Offset(xFor(point.timeMs), yFor(point.midiValue))
                )
            }

            val cursorX = xFor(currentPositionMs)
            drawLine(
                color = Color.White.copy(alpha = 0.78f),
                start = Offset(cursorX, 0f),
                end = Offset(cursorX, size.height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = null
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.12f),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height),
                cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        if (referenceNotes.isEmpty()) {
            Text(
                "Importá una melodía de referencia para ver las notas objetivo y puntuar.",
                color = Color(0xFF9BA8C8)
            )
        }
    }
}
