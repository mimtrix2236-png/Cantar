package com.cantar.app

import java.io.ByteArrayOutputStream
import java.io.InputStream

internal object ImportedTextLimits {
    const val MAX_REFERENCE_BYTES = 512 * 1024
    const val MAX_LYRICS_BYTES = 1024 * 1024
}

/**
 * Reads a UTF-8 document without allowing an unbounded allocation from a
 * user-selected content URI. The caller owns and closes the input stream.
 */
internal fun readBoundedUtf8(input: InputStream, maxBytes: Int): String {
    require(maxBytes > 0) { "El límite de lectura debe ser mayor que cero." }

    val output = ByteArrayOutputStream(minOf(maxBytes, 8_192))
    val buffer = ByteArray(8_192)
    var total = 0

    while (true) {
        val count = input.read(buffer)
        if (count < 0) break

        if (count == 0) {
            val single = input.read()
            if (single < 0) break
            if (total >= maxBytes) {
                throw IllegalArgumentException(limitMessage(maxBytes))
            }
            output.write(single)
            total++
            continue
        }

        if (count > maxBytes - total) {
            throw IllegalArgumentException(limitMessage(maxBytes))
        }
        output.write(buffer, 0, count)
        total += count
    }

    return String(output.toByteArray(), Charsets.UTF_8)
}

private fun limitMessage(maxBytes: Int): String {
    val limit = if (maxBytes >= 1_024) "${maxBytes / 1_024} KB" else "$maxBytes bytes"
    return "El archivo supera el límite de $limit."
}
