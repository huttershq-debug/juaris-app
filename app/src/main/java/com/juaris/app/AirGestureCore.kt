package com.juaris.app

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
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

    private val _gestureState = MutableStateFlow("Initialisiere Kamera...")
    val gestureState: StateFlow<String> = _gestureState

    private val _lastAction = MutableStateFlow<GestureAction>(GestureAction.NONE)
    val lastAction: StateFlow<GestureAction> = _lastAction

    private lateinit var cameraExecutor: ExecutorService
    private var previousByteArray: ByteArray? = null
    private var lastCenterX: Double = 0.0
    private var frameCounter: Long = 0

    fun startGestureDetection(lifecycleOwner: LifecycleOwner, onActionDetected: (GestureAction) -> Unit) {
        cameraExecutor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
           
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processImageFrame(imageProxy, onActionDetected)
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    imageAnalysis
                )
                _gestureState.value = "Kamera aktiv – Hand bewegen"
            } catch (exc: Exception) {
                _gestureState.value = "Fehler: ${exc.localizedMessage}"
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun processImageFrame(imageProxy: ImageProxy, onActionDetected: (GestureAction) -> Unit) {
        try {
            frameCounter++
            val plane = imageProxy.planes[0]
            val buffer = plane.buffer
            
            // WICHTIG: Buffer-Position auf 0 zurücksetzen, sonst liest er keine Daten!
            buffer.rewind()
            
            val rowStride = plane.rowStride
            val width = imageProxy.width
            val height = imageProxy.height

            val currentBytes = ByteArray(buffer.remaining())
            buffer.get(currentBytes)

            val prev = previousByteArray
            if (prev != null && prev.size == currentBytes.size) {
                var totalX = 0L
                var motionPixels = 0L
                val step = 16 // Feinerer Raster-Scan für sichere Erkennung

                for (y in 0 until height step step) {
                    for (x in 0 until width step step) {
                        val index = y * rowStride + x
                        if (index < currentBytes.size && index < prev.size) {
                            val currVal = currentBytes[index].toInt() and 0xFF
                            val prevVal = prev[index].toInt() and 0xFF
                            val diffVal = abs(currVal - prevVal)

                            if (diffVal > 15) { // Sensibler Schwellenwert für Bewegung
                                totalX += x
                                motionPixels++
                            }
                        }
                    }
                }

                if (motionPixels > 15) {
                    val currentCenterX = totalX.toDouble() / motionPixels
                    if (lastCenterX > 0.0) {
                        val deltaX = currentCenterX - lastCenterX
                        _gestureState.value = "Motion: $motionPixels | Delta: ${String.format("%.1f", deltaX)}"

                        val swipeThreshold = 4.0
                        if (deltaX > swipeThreshold) {
                            _lastAction.value = GestureAction.SWIPE_RIGHT
                            onActionDetected(GestureAction.SWIPE_RIGHT)
                        } else if (deltaX < -swipeThreshold) {
                            _lastAction.value = GestureAction.SWIPE_LEFT
                            onActionDetected(GestureAction.SWIPE_LEFT)
                        }
                    }
                    lastCenterX = currentCenterX
                } else {
                    _gestureState.value = "Bereit (#$frameCounter) – warte auf Swipe"
                }
            } else {
                _gestureState.value = "Kalibriere Sensor (#$frameCounter)..."
            }

            previousByteArray = currentBytes
        } catch (e: Exception) {
            _gestureState.value = "Fehler: ${e.localizedMessage}"
        } finally {
            imageProxy.close()
        }
    }

    fun stopGestureDetection() {
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}

