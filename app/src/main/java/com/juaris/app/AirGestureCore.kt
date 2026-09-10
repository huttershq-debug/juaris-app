package com.juaris.app

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

class AirGestureCore(private val context: Context) : ImageAnalysis.Analyzer {

    enum class GestureAction {
        SWIPE_RIGHT, SWIPE_LEFT, NONE
    }

    private val _gestureState = MutableStateFlow("Bereit")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow("NONE")
    val lastAction: StateFlow<String> = _lastAction

    private var previousBytesBuffer: ByteArray? = null
    private var isTracking = false
    private var anchorY = -1f
    private var smoothedCentroidY = -1f
    private var gestureStartTime = 0L
    private var lastTriggerTime = 0L

    private var currentCallback: ((GestureAction) -> Unit)? = null

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onGestureDetected: (GestureAction) -> Unit) {
        currentCallback = onGestureDetected
        _gestureState.value = "Kamera aktiv - Bereit für Gesten"
    }

    fun stopGestureDetection() {
        currentCallback = null
        previousBytesBuffer = null
        isTracking = false
        _gestureState.value = "Gestoppt"
    }

    override fun analyze(image: ImageProxy) {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val remaining = buffer.remaining()
            val currBytes = ByteArray(remaining)
            buffer.get(currBytes)

            val width = image.width
            val height = image.height
            val rowStride = planes[0].rowStride

            if (previousBytesBuffer == null || previousBytesBuffer!!.size != remaining) {
                previousBytesBuffer = currBytes
                image.close()
                return
            }

            val currentTime = System.currentTimeMillis()
            // Auf 1200ms erhöht für eine angenehme Pause zwischen den Gesten
            if (currentTime - lastTriggerTime > 1200L) {
                var massY = 0L
                var totalMass = 0L
                val step = 4

                for (y in 0 until height step step) {
                    val rowOffset = y * rowStride
                    for (x in 0 until width step step) {
                        val index = rowOffset + x
                        if (index < remaining) {
                            val curr = currBytes[index].toInt() and 0xFF
                            val prev = previousBytesBuffer!![index].toInt() and 0xFF
                            val delta = abs(curr - prev)

                            if (delta > 12) {
                                massY += (y * delta).toLong()
                                totalMass += delta.toLong()
                            }
                        }
                    }
                }

                val massEnter = 400L
                val massExit = 150L

                if (totalMass > (if (isTracking) massExit else massEnter)) {
                    val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                    smoothedCentroidY = if (smoothedCentroidY == -1f) {
                        rawCentroidY
                    } else {
                        0.3f * rawCentroidY + 0.7f * smoothedCentroidY
                    }

                    if (!isTracking) {
                        anchorY = smoothedCentroidY
                        isTracking = true
                        gestureStartTime = currentTime
                        _gestureState.value = "Hand erkannt..."
                    } else {
                        val totalDisplacement = smoothedCentroidY - anchorY
                        // Auf 12% der Höhe erhöht, verhindert unbeabsichtigtes "von alleine weitergehen"
                        val swipeThreshold = height.toFloat() * 0.12f

                        if (currentTime - gestureStartTime > 2000L) {
                            anchorY = smoothedCentroidY
                            gestureStartTime = currentTime
                        }

                        if (abs(totalDisplacement) > swipeThreshold) {
                            lastTriggerTime = currentTime

                            // Vertikale Steuerung: Rauf wischen = SWIPE_RIGHT, Runter wischen = SWIPE_LEFT
                            val action = if (totalDisplacement < 0f) {
                                _gestureState.value = "Aktion: Rauf (Nächster Tab)"
                                _lastAction.value = "SWIPE_RIGHT"
                                GestureAction.SWIPE_RIGHT
                            } else {
                                _gestureState.value = "Aktion: Runter (Vorheriger Tab)"
                                _lastAction.value = "SWIPE_LEFT"
                                GestureAction.SWIPE_LEFT
                            }

                            currentCallback?.let { callback ->
                                ContextCompat.getMainExecutor(context).execute {
                                    callback(action)
                                }
                            }

                            isTracking = false
                            smoothedCentroidY = -1f
                            anchorY = -1f
                        }
                    }
                } else {
                    if (totalMass < massExit) {
                        isTracking = false
                        smoothedCentroidY = -1f
                        anchorY = -1f
                        _gestureState.value = "Warte auf Geste..."
                    }
                }
            }

            System.arraycopy(currBytes, 0, previousBytesBuffer!!, 0, remaining)
        } catch (e: Exception) {
            // Frame-Fehler abfangen
        } finally {
            image.close()
        }
    }
}

