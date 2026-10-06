package com.juaris.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.abs

class AcousticThreatDetector(
    private val context: Context,
    private val onEmergencyDetected: () -> Unit
) {
    companion object {
        private const val TAG = "JuarisAudioKernel"
        private const val SAMPLE_RATE = 8000
        private const val AMPLITUDE_THRESHOLD = 27000
        private const val REQUIRED_HIGH_FRAMES = 3
    }

    @Volatile
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    private var audioRecord: AudioRecord? = null

    // Eigenes SupervisorJob-Scope für sauberes Thread-Management
    private val detectorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startListening() {
        if (isMonitoring) return

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Mikrofon-Berechtigung fehlt.")
            return
        }

        try {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, audioFormat)

            if (minBufferSize <= 0) {
                Log.e(TAG, "Ungültige Mindestpuffergröße für AudioRecord.")
                return
            }

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                channelConfig,
                audioFormat,
                minBufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Mikrofon-Hardware konnte nicht initialisiert werden.")
                audioRecord?.release()
                audioRecord = null
                return
            }

            audioRecord?.startRecording()
            isMonitoring = true

            monitoringJob = detectorScope.launch {
                val buffer = ShortArray(minBufferSize / 2)
                var highAmplitudeCount = 0

                try {
                    while (isMonitoring && isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        // Synchoner, blockierender Read schützt vor Buffer Overflows
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0

                        if (read > 0) {
                            var sum = 0L
                            for (i in 0 until read) {
                                sum += abs(buffer[i].toInt())
                            }
                            val averageAmplitude = sum / read

                            // Multi-Validierung anhaltender Schalleignisse
                            if (averageAmplitude > AMPLITUDE_THRESHOLD) {
                                highAmplitudeCount++
                                if (highAmplitudeCount >= REQUIRED_HIGH_FRAMES) {
                                    withContext(Dispatchers.Main) {
                                        onEmergencyDetected()
                                    }
                                    highAmplitudeCount = 0
                                    
                                    // 10 Sekunden Cool-Down nach Notfall-Auslösung
                                    delay(10000L)
                                }
                            } else {
                                highAmplitudeCount = (highAmplitudeCount - 1).coerceAtLeast(0)
                            }
                        } else if (read < 0) {
                            Log.e(TAG, "AudioRecord Read-Fehler Code: $read")
                            break
                        }
                    }
                } catch (e: CancellationException) {
                    Log.d(TAG, "Audio-Überwachungsschleife beendet.")
                } catch (e: Exception) {
                    Log.e(TAG, "Fehler in der Audio-Verarbeitung: ${e.message}", e)
                } finally {
                    releaseAudioRecord()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Starten der Audio-Erkennung: ${e.message}", e)
            stopListening()
        }
    }

    fun stopListening() {
        isMonitoring = false
        monitoringJob?.cancel()
        detectorScope.coroutineContext.cancelChildren()
        releaseAudioRecord()
    }

    private synchronized fun releaseAudioRecord() {
        try {
            audioRecord?.let { record ->
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fehler beim Freigeben der AudioRecord-Ressource: ${e.message}")
        } finally {
            audioRecord = null
            Log.d(TAG, "🛑 Akustischer Wächter sicher heruntergefahren.")
        }
    }
}
