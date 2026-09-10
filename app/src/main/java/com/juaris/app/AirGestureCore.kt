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

    private var lastAvgTop = -1.0
    private var lastAvgBottom = -1.0

    // Sequenz-Tracking für echte Bewegungsrichtung
    private var activeGestureDirection = 0 // 1 = von unten nach oben, -1 = von oben nach unten
    private var gestureStageTime = 0L

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

            val yStep = (effectiveHeight / 10).coerceAtLeast(1)
            val xStep = (effectiveWidth / 10).coerceAtLeast(1)

            for (y in 0 until height step yStep) {
                for (x in 0 until width step xStep) {
                    val index = y * rowStride + x * pixelStride
                    if (index < buffer.capacity()) {
                        val pixel = buffer.get(index).toInt() and 0xFF
                        val vCoord = if (isPortrait) x else y
                        if (vCoord < effectiveHeight / 2) {
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

            if (lastAvgTop < 0.0) {
                lastAvgTop = avgTop
                lastAvgBottom = avgBottom
                return
            }

            // Differenz zum vorherigen Frame (Frame-to-Frame Änderung)
            val diffTop = avgTop - lastAvgTop
            val diffBottom = avgBottom - lastAvgBottom

            lastAvgTop = avgTop
            lastAvgBottom = avgBottom

            val currentTime = System.currentTimeMillis()
            val threshold = 3.5 // Empfindlichkeits-Schwelle für Schattenwurf der Hand

            // Cooldown zwischen Aktionen (600ms)
            if (currentTime - lastActionTime > 600) {

                // 1. Schritt: Hand betritt das Bild von unten (Unten wird abgedunkelt)
                if (diffBottom < -threshold && activeGestureDirection == 0) {
                    activeGestureDirection = 1 // Vermutet Wisch nach Oben
                    gestureStageTime = currentTime
                }
                // 1. Schritt Alternative: Hand betritt das Bild von oben (Oben wird abgedunkelt)
                else if (diffTop < -threshold && activeGestureDirection == 0) {
                    activeGestureDirection = -1 // Vermutet Wisch nach Unten
                    gestureStageTime = currentTime
                }

                // 2. Schritt: Die Sequenz vervollständigen (innerhalb von 400ms muss die Hand im anderen Bereich ankommen)
                if (activeGestureDirection == 1) {
                    if (diffTop < -threshold && currentTime - gestureStageTime < 400) {
                        // Erfolgreich: Unten gestartet, jetzt oben -> Wisch nach Oben!
                        lastActionTime = currentTime
                        activeGestureDirection = 0
                        currentActionState = GestureAction.SWIPE_UP
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_UP)
                        }
                    } else if (currentTime - gestureStageTime > 400) {
                        activeGestureDirection = 0 // Timeout zurücksetzen
                    }
                } else if (activeGestureDirection == -1) {
                    if (diffBottom < -threshold && currentTime - gestureStageTime < 400) {
                        // Erfolgreich: Oben gestartet, jetzt unten -> Wisch nach Unten!
                        lastActionTime = currentTime
                        activeGestureDirection = 0
                        currentActionState = GestureAction.SWIPE_DOWN
                        CoroutineScope(Dispatchers.Main).launch {
                            onGestureDetected(GestureAction.SWIPE_DOWN)
                        }
                    } else if (currentTime - gestureStageTime > 400) {
                        activeGestureDirection = 0 // Timeout zurücksetzen
                    }
                }
            }

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

