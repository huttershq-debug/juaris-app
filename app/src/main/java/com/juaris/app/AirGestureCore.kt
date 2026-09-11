package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class AirGestureCore(private val context: Context) {

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    enum class GestureAction {
        SWIPE_UP, SWIPE_DOWN, NONE
    }

    fun startGestureDetection(
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
        onGestureDetected: (GestureAction) -> Unit,
        onDebugInfo: (String) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                val imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var previousPixels: IntArray? = null
                var coolDown = 0

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val buffer = imageProxy.planes[0].buffer
                        val width = imageProxy.width
                        val height = imageProxy.height
                        val rowStride = imageProxy.planes[0].rowStride

                        val step = 16
                        val sampledWidth = width / step
                        val sampledHeight = height / step
                        val currentPixels = IntArray(sampledWidth * sampledHeight)
                        var index = 0

                        buffer.rewind()
                        for (y in 0 until height step step) {
                            for (x in 0 until width step step) {
                                val byteIndex = y * rowStride + x * 4
                                if (byteIndex + 2 < buffer.capacity()) {
                                    val r = buffer.get(byteIndex).toInt() and 0xFF
                                    val g = buffer.get(byteIndex + 1).toInt() and 0xFF
                                    val b = buffer.get(byteIndex + 2).toInt() and 0xFF
                                    currentPixels[index++] = (r + g + b) / 3
                                }
                            }
                        }

                        // Lokale Kopie sichern, um den Kotlin Smart-Cast Fehler zu umgehen
                        val localPrev = previousPixels

                        if (coolDown > 0) {
                            coolDown--
                        } else if (localPrev != null && currentPixels.size == localPrev.size) {
                            var changedCount = 0
                            var upperChange = 0
                            var lowerChange = 0

                            for (i in currentPixels.indices) {
                                val diff = abs(currentPixels[i] - localPrev[i])
                                if (diff > 35) {
                                    changedCount++
                                    val yCoord = i / sampledWidth
                                    if (yCoord < sampledHeight / 2) {
                                        upperChange++
                                    } else {
                                        lowerChange++
                                    }
                                }
                            }

                            if (changedCount > currentPixels.size * 0.06) {
                                if (upperChange > lowerChange * 1.3) {
                                    onDebugInfo("Geste erkannt: RUNTER (Swipe Down)")
                                    onGestureDetected(GestureAction.SWIPE_DOWN)
                                    coolDown = 25
                                } else if (lowerChange > upperChange * 1.3) {
                                    onDebugInfo("Geste erkannt: HOCH (Swipe Up)")
                                    onGestureDetected(GestureAction.SWIPE_UP)
                                    coolDown = 25
                                }
                            } else {
                                onDebugInfo("Kamera aktiv (${changedCount} Pixel-Änderungen)")
                            }
                        }

                        previousPixels = currentPixels
                    } catch (e: Exception) {
                        onDebugInfo("Analyzer-Fehler: ${e.localizedMessage}")
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    imageAnalysis
                )
                onDebugInfo("Kamera läuft - Wische vor der Linse!")
            } catch (e: Exception) {
                onDebugInfo("Kamera-Startfehler: ${e.localizedMessage}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}

