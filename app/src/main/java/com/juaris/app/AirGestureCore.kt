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
import kotlin.math.abs

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_UP, SWIPE_DOWN
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var lastActionTime = 0L
    
    // Dynamische Baseline für den Hintergrund (lernt sich selbst an)
    private var baselineTop = -1.0
    private var baselineBottom = -1.0

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
            val effectiveHeight = if (isPortrait) width else height
            val effectiveWidth = if (isPortrait) height else width

            val yStep = (effectiveHeight / 12).coerceAtLeast(1)
            val xStep = (effectiveWidth / 12).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        
                        // Korrekte Ausrichtung auf die vertikale Achse im Hochformat
                        val vCoord = if (isPortrait) x else y
                        val midPoint = effectiveHeight / 2

                        if (vCoord < midPoint) {
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

            // Baseline beim Start initialisieren
            if (baselineTop < 0.0) {
                baselineTop = avgTop
                baselineBottom = avgBottom
                return
            }

            // Differenz zum normalen Hintergrund berechnen
            val deltaTop = avgTop - baselineTop
            val deltaBottom = avgBottom - baselineBottom

            val currentTime = System.currentTimeMillis()
            
            // 500ms Cooldown für sauberes, flüssiges Blättern durch die Tabs
            if (currentTime - lastActionTime > 500) {
                val threshold = 2.5 // Sensitivitäts-Schwelle

                // Hand bewegt sich von unten nach oben (Unten wird abgedeckt -> Delta negativ)
                if (deltaBottom < -threshold && deltaTop > -threshold * 0.7) {
                    lastActionTime = currentTime
                    currentActionState = GestureAction.SWIPE_UP
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_UP)
                    }
                } 
                // Hand bewegt sich von oben nach unten (Oben wird abgedeckt -> Delta negativ)
                else if (deltaTop < -threshold && deltaBottom > -threshold * 0.7) {
                    lastActionTime = currentTime
                    currentActionState = GestureAction.SWIPE_DOWN
                    CoroutineScope(Dispatchers.Main).launch {
                        onGestureDetected(GestureAction.SWIPE_DOWN)
                    }
                }
            }

            // Baseline passt sich langsam an Lichtveränderungen im Raum an
            baselineTop = baselineTop * 0.92 + avgTop * 0.08
            baselineBottom = baselineBottom * 0.92 + avgBottom * 0.08

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

