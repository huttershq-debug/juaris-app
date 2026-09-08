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
    private val cooldownMillis = 600L // Gesunde Pause, damit kein Tab doppelt springt
    
    private var previousCentroidY: Float = -1f
    
    // Zähler für konsistente Bewegungen über mehrere Frames (eliminiert Rauschen & Springen)
    private var consecutiveUpFrames = 0
    private var consecutiveDownFrames = 0

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
                            var massY = 0L
                            var totalMass = 0L
                            val step = 8

                            for (y in 0 until height step step) {
                                for (x in 0 until width step step) {
                                    val index = y * rowStride + x
                                    if (index < currentBytes.size && index < previousBytes!!.size) {
                                        val curr = currentBytes[index].toInt() and 0xFF
                                        val prev = previousBytes!![index].toInt() and 0xFF
                                        val delta = abs(curr - prev)
                                        
                                        if (delta > 12) { // Etwas strenger gegen Hintergrundrauschen
                                            massY += (y * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            // Moderate Masse-Schwelle
                            if (totalMass > 4000L) {
                                val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                                val currentCentroidY = if (previousCentroidY == -1f) {
                                    rawCentroidY
                                } else {
                                    0.5f * rawCentroidY + 0.5f * previousCentroidY
                                }

                                if (previousCentroidY != -1f) {
                                    val deltaY = currentCentroidY - previousCentroidY

                                    // Mindest-Weg pro Frame, damit es eine echte Bewegung ist
                                    if (abs(deltaY) > 0.4f) {
                                        if (deltaY < 0f) {
                                            // Bewegung nach oben
                                            consecutiveUpFrames++
                                            consecutiveDownFrames = 0
                                        } else {
                                            // Bewegung nach unten
                                            consecutiveDownFrames++
                                            consecutiveUpFrames = 0
                                        }

                                        // Wenn die Bewegung über 3 Frames hinweg konstant in dieselbe Richtung läuft -> Auslösen!
                                        val requiredFrames = 3

                                        if (consecutiveUpFrames >= requiredFrames) {
                                            if (currentTime - lastTriggerTime > cooldownMillis) {
                                                lastTriggerTime = currentTime
                                                
                                                _gestureState.value = "Swipe: Rauf (Rechts)"
                                                _lastAction.value = "SWIPE_RIGHT"
                                                
                                                ContextCompat.getMainExecutor(context).execute {
                                                    onGestureDetected(GestureAction.SWIPE_RIGHT)
                                                }
                                            }
                                            consecutiveUpFrames = 0
                                        } else if (consecutiveDownFrames >= requiredFrames) {
                                            if (currentTime - lastTriggerTime > cooldownMillis) {
                                                lastTriggerTime = currentTime
                                                
                                                _gestureState.value = "Swipe: Runter (Links)"
                                                _lastAction.value = "SWIPE_LEFT"
                                                
                                                ContextCompat.getMainExecutor(context).execute {
                                                    onGestureDetected(GestureAction.SWIPE_LEFT)
                                                }
                                            }
                                            consecutiveDownFrames = 0
                                        }
                                    }
                                }
                                previousCentroidY = currentCentroidY
                            } else {
                                // Wenn keine Hand im Bild / Bewegung zu gering: Zähler sanft zurücksetzen
                                consecutiveUpFrames = 0
                                consecutiveDownFrames = 0
                                previousCentroidY = -1f
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

