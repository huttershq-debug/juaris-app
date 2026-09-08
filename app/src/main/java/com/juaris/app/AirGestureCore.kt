package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class AirGestureCore(private val context: Context) {

    enum class GestureAction {
        NONE, SWIPE_LEFT, SWIPE_RIGHT
    }

    private val _gestureState = MutableStateFlow("Juaris Autonomous Core Aktiv")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("KEINE")
    val lastAction: StateFlow<String> = _lastAction

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastTriggerTime = 0L
    private val cooldownMillis = 350L // Rasantes, flüssiges Durchswipen
    
    private var previousCentroidX: Float = -1f
    private var velocityBuffer = 0f

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                var previousBytes: ByteArray? = null

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val currentTime = System.currentTimeMillis()
                        val plane = imageProxy.planes[0]
                        val buffer = plane.buffer
                        val rowStride = plane.rowStride
                        val width = imageProxy.width
                        val height = imageProxy.height

                        val currentBytes = ByteArray(buffer.remaining())
                        buffer.get(currentBytes)

                        if (previousBytes != null && previousBytes!!.size == currentBytes.size) {
                            var massX = 0L
                            var totalMass = 0L
                            var totalBrightness = 0L
                            val step = 12
                            val totalPixels = (width / step) * (height / step)

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        totalBrightness += curr
                                        
                                        val delta = abs(curr - prev)
                                        if (delta > 18) { // Ultra-sensibel für jede Handbewegung
                                            massX += (x * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            val avgBrightness = if (totalPixels > 0) totalBrightness / totalPixels else 128L
                            val dynamicMassThreshold = if (avgBrightness < 40) 2500L else 6000L

                            if (totalMass > dynamicMassThreshold) {
                                val currentCentroidX = massX.toFloat() / totalMass.toFloat()

                                if (previousCentroidX != -1f) {
                                    val deltaX = currentCentroidX - previousCentroidX
                                    velocityBuffer = (velocityBuffer * 0.3f) + (deltaX * 0.7f)

                                    val activationThreshold = width * 0.018f // Maximale Reichweite & Empfindlichkeit

                                    if (abs(velocityBuffer) > activationThreshold) {
                                        if (currentTime - lastTriggerTime > cooldownMillis) {
                                            lastTriggerTime = currentTime

                                            val action = if (velocityBuffer > 0) {
                                                _gestureState.value = "Swipe Rechts erkannt"
                                                _lastAction.value = "SWIPE_RIGHT"
                                                GestureAction.SWIPE_RIGHT
                                            } else {
                                                _gestureState.value = "Swipe Links erkannt"
                                                _lastAction.value = "SWIPE_LEFT"
                                                GestureAction.SWIPE_LEFT
                                            }

                                            // Zwingend auf den UI-Thread dispatchen für sofortiges Umschalten
                                            ContextCompat.getMainExecutor(context).execute {
                                                onGestureDetected(action)
                                            }

                                            // Sofortiger Reset für den nächsten flüssigen Endlos-Swipe
                                            previousCentroidX = -1f
                                            velocityBuffer = 0f
                                        }
                                    }
                                }
                                previousCentroidX = currentCentroidX
                            } else {
                                previousCentroidX = -1f
                                velocityBuffer = 0f
                            }
                        }
                        previousBytes = currentBytes

                    } catch (e: Exception) {
                        // Frame-Sicherung
                    } finally {
                        imageProxy.close()
                    }
                }

                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )

            } catch (e: Exception) {
                _gestureState.value = "Initialisierungsfehler: ${e.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopGestureDetection() {
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}

