package com.cantar.app

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun VocalPitchCapture(
    onPitch: (PitchAnalyzer.Pitch?) -> Unit,
    autoStart: Boolean = false,
    onPermissionGranted: () -> Unit = {},
    onFrameCenterOffsetMs: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var audioUnavailable by remember { mutableStateOf(false) }
    var effectsMessage by remember { mutableStateOf("La cancelación de eco depende del teléfono.") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionDenied = !granted
        recording = granted
        if (granted) onPermissionGranted()
    }

    LaunchedEffect(autoStart) {
        if (autoStart) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            permissionDenied = !granted
            if (granted) recording = true
        }
    }

    LaunchedEffect(recording) {
        if (!recording) {
            effectsMessage = "Los filtros de audio se activan solo durante la captura."
            onPitch(null)
            return@LaunchedEffect
        }

        val completed = withContext(Dispatchers.IO) {
            val sampleRates = intArrayOf(48_000, 44_100, 22_050)
            var recorder: AudioRecord? = null
            var activeRate = 0
            var activeSource = MediaRecorder.AudioSource.VOICE_RECOGNITION
            var echoCanceler: AcousticEchoCanceler? = null
            var noiseSuppressor: NoiseSuppressor? = null

            try {
                val audioManager = context.getSystemService(AudioManager::class.java)
                val unprocessedSupported =
                    audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
                val sourceCandidates = buildList {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // Android documents this source specifically for low-latency live performance,
                        // including karaoke, with reduced coupling to the playback path.
                        add(MediaRecorder.AudioSource.VOICE_PERFORMANCE)
                    }
                    if (unprocessedSupported) add(MediaRecorder.AudioSource.UNPROCESSED)
                    add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                }.distinct()

                // Try both source and sample-rate combinations. Some Android devices expose
                // VOICE_PERFORMANCE but do not support every requested input rate.
                for (source in sourceCandidates) {
                    for (rate in sampleRates) {
                        val minimum = AudioRecord.getMinBufferSize(
                            rate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT
                        )
                        if (minimum <= 0) continue
                        val candidate = try {
                            AudioRecord(
                                source,
                                rate,
                                AudioFormat.CHANNEL_IN_MONO,
                                AudioFormat.ENCODING_PCM_16BIT,
                                maxOf(minimum * 2, 16_384)
                            )
                        } catch (_: IllegalArgumentException) {
                            null
                        } ?: continue
                        if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                            recorder = candidate
                            activeRate = candidate.sampleRate.takeIf { it > 0 } ?: rate
                            activeSource = source
                            break
                        }
                        candidate.release()
                    }
                    if (recorder != null) break
                }

                val audioRecord = recorder ?: return@withContext false
                echoCanceler = runCatching {
                    if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(audioRecord.audioSessionId) else null
                }.getOrNull()
                noiseSuppressor = runCatching {
                    if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(audioRecord.audioSessionId) else null
                }.getOrNull()
                val echoEnabled = runCatching { echoCanceler?.setEnabled(true) == AudioEffect.SUCCESS }.getOrDefault(false)
                val noiseEnabled = runCatching { noiseSuppressor?.setEnabled(true) == AudioEffect.SUCCESS }.getOrDefault(false)
                val sourceLabel = when (activeSource) {
                    MediaRecorder.AudioSource.VOICE_PERFORMANCE -> "Entrada de baja latencia para karaoke"
                    MediaRecorder.AudioSource.UNPROCESSED -> "Entrada de micrófono sin procesar"
                    else -> "Entrada optimizada para reconocimiento de voz"
                }
                val effectsLabel = when {
                    echoEnabled && noiseEnabled -> "cancelación de eco y ruido activadas"
                    echoEnabled -> "cancelación de eco activa"
                    noiseEnabled -> "reducción de ruido activa"
                    else -> "sin filtros de eco/ruido compatibles; se recomiendan auriculares"
                }
                withContext(Dispatchers.Main) {
                    effectsMessage = "$sourceLabel · $effectsLabel · ${activeRate} Hz."
                }

                val readBuffer = ShortArray(2_048)
                val frame = ShortArray(4_096)
                var frameCount = 0
                var readFailed = false
                val frameCenterOffsetMs = (frame.size / 2L * 1_000L) / activeRate

                withContext(Dispatchers.Main) {
                    onFrameCenterOffsetMs(frameCenterOffsetMs)
                }
                audioRecord.startRecording()
                while (isActive) {
                    val count = audioRecord.read(
                        readBuffer,
                        0,
                        readBuffer.size,
                        AudioRecord.READ_NON_BLOCKING
                    )
                    when {
                        count > 0 -> {
                            val copied = minOf(count, frame.size - frameCount)
                            readBuffer.copyInto(frame, frameCount, 0, copied)
                            frameCount += copied

                            if (frameCount == frame.size) {
                                val pitch = PitchAnalyzer.analyze(frame.copyOf(), activeRate)
                                withContext(Dispatchers.Main) { onPitch(pitch) }
                                // 50% overlap gives a smoother pitch trace without excessive CPU use.
                                frame.copyInto(frame, 0, frame.size / 2, frame.size)
                                frameCount = frame.size / 2
                            }
                        }
                        count == 0 -> delay(6)
                        count < 0 -> { readFailed = true; break }
                        else -> delay(10)
                    }
                }
                !readFailed
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalStateException) {
                false
            } catch (_: IllegalArgumentException) {
                false
            } finally {
                try { noiseSuppressor?.release() } catch (_: RuntimeException) {}
                try { echoCanceler?.release() } catch (_: RuntimeException) {}
                recorder?.let {
                    try {
                        if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop()
                    } catch (_: IllegalStateException) {
                    }
                    it.release()
                }
            }
        }

        if (!completed && isActive) {
            audioUnavailable = true
            recording = false
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (permissionDenied) {
            Text(
                "Necesitás habilitar el micrófono en Android para medir tu voz.",
                color = Color(0xFFFFD43B)
            )
        }
        Text(effectsMessage, color = Color(0xFF9BA8C8), fontSize = 10.sp)
        if (audioUnavailable) {
            Text(
                "No se pudo iniciar el micrófono. Cerrá otras aplicaciones que usen audio y volvé a intentar.",
                color = Color(0xFFFFD43B)
            )
        }
        Button(
            onClick = {
                audioUnavailable = false
                if (recording) {
                    recording = false
                } else if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    recording = true
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recording) Color(0xFFB83250) else Color(0xFF16E34F)
            )
        ) {
            Text(if (recording) "Detener micrófono" else "Activar micrófono y cantar")
        }
    }
}
