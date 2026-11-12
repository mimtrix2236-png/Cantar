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
        if (autoStart) recording = true
    }

    LaunchedEffect(recording) {
        if (!recording) {
            effectsMessage = "Los filtros de audio se activan solo durante la captura."
            onPitch(null)
            return@LaunchedEffect
        }

        val completed = withContext(Dispatchers.IO) {
            val sampleRates = intArrayOf(44_100, 48_000, 22_050)
            var recorder: AudioRecord? = null
            var activeRate = 0
            var echoCanceler: AcousticEchoCanceler? = null
            var noiseSuppressor: NoiseSuppressor? = null

            try {
                for (rate in sampleRates) {
                    val minimum = AudioRecord.getMinBufferSize(
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    if (minimum <= 0) continue
                    val audioManager = context.getSystemService(AudioManager::class.java)
                    val inputSource = if (audioManager?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true") {
                        MediaRecorder.AudioSource.UNPROCESSED
                    } else {
                        MediaRecorder.AudioSource.VOICE_RECOGNITION
                    }
                    val candidate = AudioRecord(
                        inputSource,
                        rate,
                        AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        maxOf(minimum * 2, 16_384)
                    )
                    if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                        recorder = candidate
                        activeRate = rate
                        break
                    }
                    candidate.release()
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
                withContext(Dispatchers.Main) {
                    effectsMessage = when {
                        echoEnabled && noiseEnabled -> "Cancelación de eco y ruido activadas por el dispositivo."
                        echoEnabled -> "Cancelación de eco activa; reducción de ruido no disponible."
                        noiseEnabled -> "Reducción de ruido activa; cancelación de eco no disponible."
                        else -> "El teléfono no ofrece filtros compatibles. Para evitar que la música se cuele en el micrófono, usá auriculares."
                    }
                }

                val readBuffer = ShortArray(2_048)
                val frame = ShortArray(4_096)
                var frameCount = 0
                var readFailed = false

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
