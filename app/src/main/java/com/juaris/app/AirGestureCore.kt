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
    private val cooldownMillis = 500L // Flotter Cooldown
    
    private var previousCentroidY: Float = -1f
    private var accumulatedDeltaY = 0f

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
                        width = imageProxy.width
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
                                        
                                        // Extrem feinfühlig ab Masse 500L
                                        if (delta > 8) {
                                            massY += (y * delta)
                                            totalMass += delta
                                        }
                                    }
                                }
                            }

                            // Ultra-niedrige Masse (500L) - bricht nie wieder am Körper ab
                            if (totalMass > 500L) {
                                val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                                val currentCentroidY = if (previousCentroidY == -1f) {
                                    rawCentroidY
                                } else {
                                    0.3f * rawCentroidY + 0.7f * previousCentroidY
                                }

                                if (previousCentroidY != -1f) {
                                    val deltaY = currentCentroidY - previousCentroidY

                                    // Ganz feines Gate gegen Raum-Rauschen
                                    if (abs(deltaY) > 0.1f) {
                                        if (accumulatedDeltaY == 0f) {
                                            accumulatedDeltaY = deltaY
                                        } else if ((accumulatedDeltaY > 0f && deltaY > 0f) || (accumulatedDeltaY < 0f && deltaY < 0f)) {
                                            accumulatedDeltaY += deltaY
                                        } else {
                                            // Richtungswechsel -> sofort übernehmen
                                            accumulatedDeltaY = deltaY
                                        }

                                        // 8% der Bildhöhe als knackiger Schwellenwert
                                        val swipeThreshold = height.toFloat() * 0.08f

                                        if (abs(accumulatedDeltaY) > swipeThreshold) {
                                            // Sofort den Akkumulator leeren, damit KEIN Überlaufen/Überspringen passiert!
                                            val triggeredDelta = accumulatedDeltaY
                                            accumulatedDeltaY = 0f

                                            if (currentTime - lastTriggerTime > cooldownMillis) {
                                                lastTriggerTime = currentTime

                                                val action = if (triggeredDelta < 0f) {
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
                                            }
                                        }
                                    }
                                }
                                previousCentroidY = currentCentroidY
                            } else {
                                // Hand komplett weg -> Werte sanft zurücksetzen
                                previousCentroidY = -1f
                                accumulatedDeltaY = 0f
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

