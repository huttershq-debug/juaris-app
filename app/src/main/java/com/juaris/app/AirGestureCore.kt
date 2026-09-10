package com.juaris.app

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastActionTime = 0L
    private var lastBalance = 0.0
    private var frameCounter = 0
    
    // Zähler für die Multi-Frame-Bestätigung (stoppt jegliches Herumspringen)
    private var consecutiveUpFrames = 0
    private var consecutiveDownFrames = 0

    var currentActionState: GestureAction = GestureAction.NONE
        private set

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    // Performance-Schonung: Jeden 3. Frame analysieren
                    frameCounter++
                    if (frameCounter % 3 != 0) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    processImage(imageProxy, onGestureDetected)
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImage(imageProxy: ImageProxy, onGestureDetected: (GestureAction) -> Unit) {
        try {
            val plane = imageProxy.planes[0]
            val buffer: ByteBuffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride
            val width = imageProxy.width
            val height = imageProxy.height
            val rotation = imageProxy.imageInfo.rotationDegrees

            var topSum = 0.0
            var bottomSum = 0.0
            var countTop = 0
            var countBottom = 0

            val isPortrait = rotation == 90 || rotation == 270

            // Stabiles Raster (12) gegen unnötiges Rauschen und Herumspringen
            val yStep = (height / 12).coerceAtLeast(1)
            val xStep = (width / 12).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        
                        val verticalCoord = if (isPortrait) x else y
                        val verticalLimit = if (isPortrait) width else height

                        if (verticalCoord < verticalLimit / 2) {
                            topSum += pixel
                            countTop++
                        } else {
                            bottomSum += pixel
                            countBottom++
                        }
                    }
                }
            }

            val avgTop = if (countTop > 0) topSum / countTop else 0.0
            val avgBottom = if (countBottom > 0) bottomSum / countBottom else 0.0

            val totalLight = avgTop + avgBottom
            val currentBalance = if (totalLight > 1.0) {
                (avgBottom - avgTop) / totalLight
            } else {
                0.0
            }

            val currentTime = System.currentTimeMillis()

            // 4.0 Sekunden absolute Sperre nach jeder Aktion (verhindert Überspringen komplett)
            if (currentTime - lastActionTime > 4000) {
                if (lastBalance != 0.0) {
                    val balanceChange = currentBalance - lastBalance

                    // Korrekte Richtungszuordnung & stabiler Schwellenwert (0.22) gegen Fehltrünke
                    if (balanceChange > 0.22) {
                        // Hand bewegt sich nach oben -> SWIPE_UP (Nächster Tab / Rechts)
                        consecutiveUpFrames++
                        consecutiveDownFrames = 0
                    } else if (balanceChange < -0.22) {
                        // Hand bewegt sich nach unten -> SWIPE_DOWN (Vorheriger Tab / Links)
                        consecutiveDownFrames++
                        consecutiveUpFrames = 0
                    } else {
                        // Werte langsam abbauen, um Jitter zu vermeiden
                        consecutiveUpFrames = maxOf(0, consecutiveUpFrames - 1)
                        consecutiveDownFrames = maxOf(0, consecutiveDownFrames - 1)
                    }

                    // Erst ab 3 stabilen Frames in Folge wird die Geste fehlerfrei ausgelöst
                    if (consecutiveUpFrames >= 3) {
                        lastActionTime = currentTime
                        consecutiveUpFrames = 0
                        currentActionState = GestureAction.SWIPE_UP
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_UP)
                        }
                    } else if (consecutiveDownFrames >= 3) {
                        lastActionTime = currentTime
                        consecutiveDownFrames = 0
                        currentActionState = GestureAction.SWIPE_DOWN
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_DOWN)
                        }
                    }
                }
            } else {
                currentActionState = GestureAction.NONE
                consecutiveUpFrames = 0
                consecutiveDownFrames = 0
            }

            lastBalance = currentBalance
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            imageProxy.close()
        }
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

