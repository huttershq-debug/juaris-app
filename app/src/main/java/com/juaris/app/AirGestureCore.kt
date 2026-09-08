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

    private val _gestureState = MutableStateFlow("Kamera bereit – Hand swipen")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("KEINE")
    val lastAction: StateFlow<String> = _lastAction

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastTriggerTime = 0L
    private val cooldownMillis = 600L // Schnelle Wiederholrate für flüssiges Durchswipen
    private var previousCentroidX: Float = -1f

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
                            val step = 16 // Optimierte Abtastung für flüssige Performance

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        val delta = abs(curr - prev)

                                        // Filter gegen Rauschen, registriert echte Handkonturen
                                        if (delta > 20) {
                                            massX += (x * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            // Angepasster Schwellenwert für sofortige Erkennung auf dem S23
                            if (totalMass > 5000L) {
                                val currentCentroidX = massX.toFloat() / totalMass.toFloat()

                                if (previousCentroidX != -1f) {
                                    val deltaX = currentCentroidX - previousCentroidX
                                    val movementThreshold = width * 0.025f // 2.5% Bewegung reichen für einen Swipe

                                    if (abs(deltaX) > movementThreshold) {
                                        if (currentTime - lastTriggerTime > cooldownMillis) {
                                            lastTriggerTime = currentTime

                                            if (deltaX > 0) {
                                                _gestureState.value = "Geste: Swipe Rechts"
                                                _lastAction.value = "SWIPE_RIGHT"
                                                onGestureDetected(GestureAction.SWIPE_RIGHT)
                                            } else {
                                                _gestureState.value = "Geste: Swipe Links"
                                                _lastAction.value = "SWIPE_LEFT"
                                                onGestureDetected(GestureAction.SWIPE_LEFT)
                                            }
                                        }
                                    }
                                }
                                previousCentroidX = currentCentroidX
                            } else {
                                previousCentroidX = -1f
                            }
                        }
                        previousBytes = currentBytes

                    } catch (e: Exception) {
                        // Abfangen von Frame-Ausnahmen
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
                _gestureState.value = "Fehler: ${e.localizedMessage}"
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


