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
    }

    private var isMonitoring = false
    private var monitoringJob: Job? = null
    private var audioRecord: AudioRecord? = null

    // KORREKTUR 1: Eigener, kontrollierter Scope verhindert ungebundene Thread-Leaks im Ruhezustand!
    private val detectorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun startListening() {
        if (isMonitoring) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        try {
            val sampleRate = 8000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "Mikrofon-Hardware konnte nicht initialisiert werden.")
                return
            }

            audioRecord?.startRecording()
            isMonitoring = true

            monitoringJob = detectorScope.launch {
                val shortBufferSize = bufferSize / 2
                val buffer = ShortArray(shortBufferSize)
                var highAmplitudeCount = 0

                // KORREKTUR 2: Prüft vor jedem Durchlauf bitgenau, ob die Hardware noch aktiv geschaltet ist!
                while (isMonitoring && isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    try {
                        val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        if (read > 0) {
                            var sum = 0L
                            for (i in 0 until read) {
                                sum += abs(buffer[i].toInt())
                            }
                            val averageAmplitude = sum / read
                           
                            // Unbestechliche Erkennung von anhaltendem Lärm/Schreien (3 Frames Multi-Validierung)
                            if (averageAmplitude > 27000) {
                                highAmplitudeCount++
                                if (highAmplitudeCount >= 3) {
                                    withContext(Dispatchers.Main) {
                                        onEmergencyDetected()
                                    }
                                    highAmplitudeCount = 0
                                    
                                    // 10 Sekunden Sicherheits-Pause nach Alarm-Auslöser
                                    delay(10000L) 
                                }
                            } else {
                                highAmplitudeCount = (highAmplitudeCount - 1).coerceAtLeast(0)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Fehler im Audio-Stream-Parsing: ${e.message}")
                    }
                    delay(100L)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopListening() {
        isMonitoring = false
        monitoringJob?.cancel()
        
        try {
            // KORREKTUR 3: Erst den State prüfen, dann stoppen – Verhindert die gefürchtete IllegalStateException!
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            Log.d(TAG, "🛑 Akustischer Wächter sicher heruntergefahren.")
        }
    }
}

