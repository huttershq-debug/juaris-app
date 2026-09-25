package com.juaris.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlin.math.abs

class AcousticThreatDetector(
    private val context: Context,
    private val onEmergencyDetected: () -> Unit
) {
    private var isMonitoring = false
    private var monitoringJob: Job? = null
    private var audioRecord: AudioRecord? = null

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

            audioRecord?.startRecording()
            isMonitoring = true

            monitoringJob = CoroutineScope(Dispatchers.IO).launch {
                val buffer = ByteArray(bufferSize)
                var highAmplitudeCount = 0 // Verhindert Fehlalarme durch einmaliges Klopfen/Teller fallen lassen

                while (isMonitoring && isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        var sum = 0L
                        for (i in 0 until read step 2) {
                            val sample = (buffer[i].toInt() and 0xFF) or ((buffer[i+1].toInt()) shl 8)
                            sum += abs(sample.toShort().toInt())
                        }
                        val averageAmplitude = sum / (read / 2)
                       
                        // Erkennung von extremen und anhaltenden akustischen Ausschlägen
                        if (averageAmplitude > 27000) {
                            highAmplitudeCount++
                            if (highAmplitudeCount >= 3) { // Muss in 3 aufeinanderfolgenden Messungen anhalten
                                withContext(Dispatchers.Main) {
                                    onEmergencyDetected()
                                }
                                highAmplitudeCount = 0
                                delay(10000) // Cooldown nach Alarmauslösung
                            }
                        } else {
                            highAmplitudeCount = (highAmplitudeCount - 1).coerceAtLeast(0)
                        }
                    }
                    delay(100)
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
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {}
        audioRecord = null
    }
}

