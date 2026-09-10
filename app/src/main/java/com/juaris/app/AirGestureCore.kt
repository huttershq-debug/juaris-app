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
        _gestureState.value = "Überwache Raum..."
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
            if (currentTime - lastTriggerTime > 1000L) {
                var massY = 0L
                var totalMass = 0L
                val step = 6

                for (y in 0 until height step step) {
                    val rowOffset = y * rowStride
                    for (x in 0 until width step step) {
                        val index = rowOffset + x
                        if (index < remaining) {
                            val curr = currBytes[index].toInt() and 0xFF
                            val prev = previousBytesBuffer!![index].toInt() and 0xFF
                            val delta = abs(curr - prev)

                            if (delta > 15) {
                                massY += (y * delta).toLong()
                                totalMass += delta.toLong()
                            }
                        }
                    }
                }

                val massEnter = 1000L
                val massExit = 400L

                if (totalMass > (if (isTracking) massExit else massEnter)) {
                    val rawCentroidY = massY.toFloat() / totalMass.toFloat()

                    smoothedCentroidY = if (smoothedCentroidY == -1f) {
                        rawCentroidY
                    } else {
                        0.2f * rawCentroidY + 0.8f * smoothedCentroidY
                    }

                    if (!isTracking) {
                        anchorY = smoothedCentroidY
                        isTracking = true
                        gestureStartTime = currentTime
                        _gestureState.value = "Bewegung erkannt..."
                    } else {
                        val totalDisplacement = smoothedCentroidY - anchorY
                        val swipeThreshold = height.toFloat() * 0.08f

                        if (currentTime - gestureStartTime > 2000L) {
                            anchorY = smoothedCentroidY
                            gestureStartTime = currentTime
                        }

                        if (abs(totalDisplacement) > swipeThreshold) {
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
                        _gestureState.value = "Bereit"
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

