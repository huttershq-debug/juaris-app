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
    private val cooldownMillis = 1100L
    
    private var smoothedCentroidY: Float = -1f
    private var anchorY: Float = -1f
    private var isTracking = false
    private var gestureStartTime = 0L

    private var currentBytesBuffer: ByteArray? = null
    private var previousBytesBuffer: ByteArray? = null

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

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val currentTime = System.currentTimeMillis()
                        
                        // Korrekter Zugriff auf das primäre Helligkeits-Plane (Y)
                        val planes = imageProxy.planes
                        val buffer = planes[0].buffer
                        val rowStride = planes[0].rowStride
                        
                        val width = imageProxy.width
                        val height = imageProxy.height
                        val remaining = buffer.remaining()

                        if (currentBytesBuffer == null || currentBytesBuffer!!.size != remaining) {
                            currentBytesBuffer = ByteArray(remaining)
                            previousBytesBuffer = ByteArray(remaining)
                            buffer.get(currentBytesBuffer)
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        buffer.get(currentBytesBuffer)

                        val currBytes = currentBytesBuffer!!
                        val prevBytes = previousBytesBuffer!!

                        if (currentTime - lastTriggerTime < cooldownMillis) {
                            System.arraycopy(currBytes, 0, prevBytes, 0, remaining)
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        var massY = 0L
                        var totalMass = 0L
                        val step = 12

                        for (y in 12 until height - 12 step step) {
                            for (x in 12 until width - 12 step step) {
                                val index = y * rowStride + x
                                if (index < remaining) {
                                    val curr = currBytes[index].toInt() and 0xFF
                                    val prev = prevBytes[index].toInt() and 0xFF
                                    val delta = abs(curr - prev)
                                    
                                    if (delta > 15) { 
                                        massY += (y * delta).toLong()
                                        totalMass += delta.toLong()
                                    }
                                }
                            }
                        }

                        val massEnter = 3000L 
                        val massExit = 1200L

                        if (totalMass > (if (isTracking) massExit else massEnter)) {
                            val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                            smoothedCentroidY = if (smoothedCentroidY == -1f) {
                                rawCentroidY
                            } else {
                                0.35f * rawCentroidY + 0.65f * smoothedCentroidY
                            }

                            if (!isTracking) {
                                anchorY = smoothedCentroidY
                                isTracking = true
                                gestureStartTime = currentTime
                            } else {
                                val totalDisplacement = smoothedCentroidY - anchorY
                                val swipeThreshold = height.toFloat() * 0.18f 
                                val duration = currentTime - gestureStartTime

                                if (duration > 1000L) {
                                    anchorY = smoothedCentroidY
                                    gestureStartTime = currentTime
                                }

                                if (abs(totalDisplacement) > swipeThreshold) {
                                    if (duration in 80L..600L) {
                                        lastTriggerTime = currentTime

                                        val action = if (totalDisplacement < 0f) {
                                            _gestureState.value = "Swipe: Rauf (Rechts)"
                                            _lastAction.value = "SWIPE_RIGHT"
                                            GestureAction.SWIPE_RIGHT
                                        } else {
                                            _gestureState.value = "Swipe: Runter (Links)"
                                            _lastAction.value = "SWIPE_LEFT"
                                            GestureAction.SWIPE_LEFT
                                        }

                                        ContextCompat.getMainExecutor(context).execute {
                                            onGestureDetected(action)
                                        }

                                        isTracking = false
                                        smoothedCentroidY = -1f
                                        anchorY = -1f
                                    } else {
                                        anchorY = smoothedCentroidY
                                        gestureStartTime = currentTime
                                    }
                                }
                            }
                        } else {
                            if (totalMass < massExit) {
                                isTracking = false
                                smoothedCentroidY = -1f
                                anchorY = -1f
                            }
                        }

                        System.arraycopy(currBytes, 0, prevBytes, 0, remaining)

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
            currentBytesBuffer = null
            previousBytesBuffer = null
        } catch (e: Exception) {
            // Ignorieren
        }
    }
}


