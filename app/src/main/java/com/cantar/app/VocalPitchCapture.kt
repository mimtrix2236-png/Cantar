package com.cantar.app

import android.Manifest
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun VocalPitchCapture(
    onPitch: (PitchAnalyzer.Pitch?) -> Unit,
    onPoints: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<PitchAnalyzer.Pitch?>(null) }
    var earned by remember { mutableIntStateOf(0) }
    var lastAwardAt by remember { mutableLongStateOf(0L) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
        recording = granted
    }
    LaunchedEffect(recording) {
        if (!recording) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sampleRate = 44_100
            val minBuffer = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuffer <= 0) return@withContext
            val recorder = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minBuffer, 8192))
            if (recorder.state != AudioRecord.STATE_INITIALIZED) { recorder.release(); return@withContext }
            val buffer = ShortArray(4096)
            try {
                recorder.startRecording()
                while (isActive && recording) {
                    val count = recorder.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        val pitch = PitchAnalyzer.analyze(buffer.copyOf(count), sampleRate)
                        withContext(Dispatchers.Main) {
                            current = pitch
                            onPitch(pitch)
                            val now = System.currentTimeMillis()
                            if (pitch != null && abs(pitch.cents) <= 25 && now - lastAwardAt >= 250) {
                                earned += 10
                                lastAwardAt = now
                                onPoints(earned)
                            }
                        }
                    }
                }
            } finally {
                try { recorder.stop() } catch (_: IllegalStateException) {}
                recorder.release()
            }
        }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(current?.let { "${it.noteName} · ${it.frequencyHz.toInt()} Hz · ${it.cents} cents" } ?: "Cantá una nota sostenida para medir tu afinación",
            color = current?.let { if (abs(it.cents) <= 25) Color(0xFF22E66B) else Color(0xFFFFD43B) } ?: Color(0xFF9BA8C8))
        Text("Puntos por afinación estable: $earned", color = Color(0xFF22E66B))
        if (permissionDenied) Text("Necesitás habilitar el micrófono en Android para usar esta función.", color = Color(0xFFFFD43B))
        Button(onClick = {
            if (recording) recording = false
            else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) recording = true
            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = if (recording) Color(0xFFB83250) else Color(0xFF19A85A))) {
            Text(if (recording) "Detener micrófono" else "Activar micrófono y cantar")
        }
    }
}
