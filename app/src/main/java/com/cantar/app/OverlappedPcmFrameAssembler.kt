package com.cantar.app

/**
 * Builds contiguous PCM windows with a fixed overlap from arbitrarily sized input reads.
 * The callback receives the reusable internal frame and its center sample index; it must
 * process the frame synchronously and must not retain the sample array.
 */
class OverlappedPcmFrameAssembler(
    val frameSize: Int = 4_096,
    val overlap: Int = frameSize / 2
) {
    private val frame = ShortArray(frameSize)
    private var filled = 0
    private var samplesConsumed = 0L

    init {
        require(frameSize >= 2) { "El tamaño de ventana debe ser de al menos 2 muestras." }
        require(overlap in 0 until frameSize) { "El solapamiento debe ser menor que la ventana." }
    }

    fun append(
        input: ShortArray,
        offset: Int = 0,
        length: Int = input.size - offset,
        onFrame: (samples: ShortArray, centerSamplePosition: Long) -> Unit
    ) {
        require(offset in 0..input.size && length >= 0 && length <= input.size - offset) {
            "El rango de lectura PCM no es válido."
        }

        var sourcePosition = offset
        val sourceEnd = offset + length
        while (sourcePosition < sourceEnd) {
            val copied = minOf(frameSize - filled, sourceEnd - sourcePosition)
            input.copyInto(
                destination = frame,
                destinationOffset = filled,
                startIndex = sourcePosition,
                endIndex = sourcePosition + copied
            )
            filled += copied
            sourcePosition += copied
            samplesConsumed += copied.toLong()

            if (filled == frameSize) {
                onFrame(frame, samplesConsumed - frameSize / 2L)
                frame.copyInto(
                    destination = frame,
                    destinationOffset = 0,
                    startIndex = frameSize - overlap,
                    endIndex = frameSize
                )
                filled = overlap
            }
        }
    }
}
