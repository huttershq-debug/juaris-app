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

    private val _gestureState = MutableStateFlow("Juaris Kortex Aktiv")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("KEINE")
    val lastAction: StateFlow<String> = _lastAction

    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    private var lastTriggerTime = 0L
    private val cooldownMillis = 750L // 750 ms Ruhepause nach jedem Swipe
    
    private var previousCentroidX: Float = -1f
    private var accumulatedDeltaX = 0f

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        if (cameraProvider != null) return

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
                            val step = 10 // Feines Raster beibehalten

                            // Komplettes Bild scannen
                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        val delta = abs(curr - prev)
                                        
                                        // Sweet Spot: Ignoriert leichtes Rauschen (delta > 12)
                                        if (delta > 12) {
                                            massX += (x * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            // Sweet Spot: Etwas höhere Masse (6000L) gegen Fehltrigger
                            if (totalMass > 6000L) {
                                val currentCentroidX = massX.toFloat() / totalMass.toFloat()

                                if (previousCentroidX != -1f) {
                                    val deltaX = currentCentroidX - previousCentroidX

                                    if (abs(deltaX) > 0.3f) {
                                        if ((accumulatedDeltaX > 0f && deltaX < 0f) || (accumulatedDeltaX < 0f && deltaX > 0f)) {
                                            accumulatedDeltaX = deltaX
                                        } else {
                                            accumulatedDeltaX += deltaX
                                        }

                                        val swipeThreshold = width * 0.15f // Präziser Schwellenwert

                                        if (abs(accumulatedDeltaX) > swipeThreshold) {
                                            if (currentTime - lastTriggerTime > cooldownMillis) {
                                                lastTriggerTime = currentTime

                                                val action = if (accumulatedDeltaX > 0f) {
                                                    _gestureState.value = "Swipe: Rechts"
                                                    _lastAction.value = "SWIPE_RIGHT"
                                                    GestureAction.SWIPE_RIGHT
                                                } else {
                                                    _gestureState.value = "Swipe: Links"
                                                    _lastAction.value = "SWIPE_LEFT"
                                                    GestureAction.SWIPE_LEFT
                                                }

                                                ContextCompat.getMainExecutor(context).execute {
                                                    onGestureDetected(action)
                                                }
                                            }
                                            accumulatedDeltaX = 0f
                                        }
                                    }
                                }
                                previousCentroidX = currentCentroidX
                            } else {
                                previousCentroidX = -1f
                                accumulatedDeltaX = 0f
                            }
                        }
                        previousBytes = currentBytes

                    } catch (e: Exception) {
                        // Frame abfangen
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
            cameraProvider = null
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}

