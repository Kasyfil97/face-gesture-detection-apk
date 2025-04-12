package com.example.sdk

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.sdk.models.GestureType
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.RunningMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * Main class for detecting face gestures using MediaPipe Face Landmarker.
 */
class FaceGestureDetector private constructor(
    private val context: Context,
    private val listener: FaceGestureListener,
    private val blinkThreshold: Float,
    private val jawOpenThreshold: Float,
    private val smileThreshold: Float,
    private val gestureCooldownMs: Long
) {
    companion object {
        private const val TAG = "FaceGestureDetector"

        // Default threshold values
        private const val DEFAULT_BLINK_THRESHOLD = 0.7f
        private const val DEFAULT_JAW_OPEN_THRESHOLD = 0.4f
        private const val DEFAULT_SMILE_THRESHOLD = 0.7f
        private const val DEFAULT_GESTURE_COOLDOWN_MS = 500L
    }

    /**
     * Builder class for creating FaceGestureDetector instances with customizable parameters.
     */
    class Builder(private val context: Context) {
        private var listener: FaceGestureListener? = null
        private var blinkThreshold: Float = DEFAULT_BLINK_THRESHOLD
        private var jawOpenThreshold: Float = DEFAULT_JAW_OPEN_THRESHOLD
        private var smileThreshold: Float = DEFAULT_SMILE_THRESHOLD
        private var gestureCooldownMs: Long = DEFAULT_GESTURE_COOLDOWN_MS

        /**
         * Set the listener for gesture detection events.
         */
        fun setListener(listener: FaceGestureListener): Builder {
            this.listener = listener
            return this
        }

        /**
         * Set the threshold for blink detection.
         * @param threshold The confidence threshold (0.0f - 1.0f). Higher values require more pronounced blinks.
         */
        fun setBlinkThreshold(threshold: Float): Builder {
            this.blinkThreshold = threshold.coerceIn(0f, 1f)
            return this
        }

        /**
         * Set the threshold for jaw open detection.
         * @param threshold The confidence threshold (0.0f - 1.0f). Higher values require more pronounced jaw opening.
         */
        fun setJawOpenThreshold(threshold: Float): Builder {
            this.jawOpenThreshold = threshold.coerceIn(0f, 1f)
            return this
        }

        /**
         * Set the threshold for smile detection.
         * @param threshold The confidence threshold (0.0f - 1.0f). Higher values require more pronounced smiles.
         */
        fun setSmileThreshold(threshold: Float): Builder {
            this.smileThreshold = threshold.coerceIn(0f, 1f)
            return this
        }

        /**
         * Set the cooldown time between gesture detections.
         * @param cooldownMs The cooldown time in milliseconds. Lower values allow more frequent gesture triggers.
         */
        fun setGestureCooldown(cooldownMs: Long): Builder {
            this.gestureCooldownMs = cooldownMs.coerceAtLeast(0)
            return this
        }

        /**
         * Build a FaceGestureDetector instance with the configured parameters.
         * @throws IllegalStateException if listener is not set
         */
        fun build(): FaceGestureDetector {
            if (listener == null) {
                throw IllegalStateException("FaceGestureListener must be set")
            }

            return FaceGestureDetector(
                context = context,
                listener = listener!!,
                blinkThreshold = blinkThreshold,
                jawOpenThreshold = jawOpenThreshold,
                smileThreshold = smileThreshold,
                gestureCooldownMs = gestureCooldownMs
            )
        }
    }

    // Gesture state tracking
    private var isFaceDetected = false
    private var isInPosition = false
    private var isBlinked = false
    private var isJawOpened = false
    private var isSmiling = false

    // Timestamps for gesture cooldowns
    private var lastBlinkTime = 0L
    private var lastJawOpenTime = 0L
    private var lastSmileTime = 0L

    // Create a separate executor for image analysis
    private val analysisExecutor = Executors.newSingleThreadExecutor()

    // Face landmarker helper
    private val faceLandmarkerHelper: FaceLandmarkerHelper = FaceLandmarkerHelper(
        context = context,
        runningMode = RunningMode.LIVE_STREAM,
        minFaceDetectionConfidence = 0.5f,
        minFacePresenceConfidence = 0.5f,
        minFaceTrackingConfidence = 0.5f,
        maxNumFaces = 1,
        faceLandmarkerHelperListener = object : FaceLandmarkerHelper.LandmarkerListener {
            override fun onError(error: String, errorCode: Int) {
                Log.e(TAG, "Face detection error: $error")
                listener.onError(error, errorCode)
            }

            override fun onResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
                processResults(resultBundle)
            }

            override fun onEmpty() {
                handleEmptyResults()
            }
        }
    )

    /**
     * Process an image frame for face gesture detection.
     */
    fun processImageProxy(imageProxy: ImageProxy, isFrontCamera: Boolean) {
        try {
            faceLandmarkerHelper.detectLiveStream(
                imageProxy = imageProxy,
                isFrontCamera = isFrontCamera
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error processing image: ${e.message}")
            listener.onError("Error processing image: ${e.message}")
            imageProxy.close()
        }
    }

    /**
     * Process a bitmap for face gesture detection.
     */
    fun processBitmap(bitmap: Bitmap, isFrontCamera: Boolean) {
        try {
            val matrix = Matrix().apply {
                // Flip image if using front camera
                if (isFrontCamera) {
                    postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
                }
            }

            val processedBitmap = if (isFrontCamera) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }

            val mpImage = BitmapImageBuilder(processedBitmap).build()

            CoroutineScope(Dispatchers.IO).launch {
                faceLandmarkerHelper.detectAsync(mpImage, System.currentTimeMillis())

                // Clean up if we created a new bitmap
                if (processedBitmap != bitmap) {
                    processedBitmap.recycle()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing bitmap: ${e.message}")
            listener.onError("Error processing bitmap: ${e.message}")
        }
    }

    /**
     * Process the results from face landmark detection.
     */
    private fun processResults(resultBundle: FaceLandmarkerHelper.ResultBundle) {
        val result = resultBundle.result

        // Update face detection state
        val faceDetected = result.faceLandmarks().isNotEmpty()
        if (faceDetected != isFaceDetected) {
            isFaceDetected = faceDetected
            listener.onFaceDetected(faceDetected)
        }

        // Only process gestures if a face is detected
        if (faceDetected) {
            // In a real implementation, we would check if the face is in the correct position
            // For simplicity, we'll assume the face is in position if detected
            if (!isInPosition) {
                isInPosition = true
                listener.onFaceInPosition(true)
            }

            // Process blink gesture using configured threshold
            val currentlyBlinking = result.isEyesBlinked(blinkThreshold)
            if (currentlyBlinking != isBlinked) {
                isBlinked = currentlyBlinking
                listener.onGestureStateChanged(GestureType.BLINK, currentlyBlinking)

                if (currentlyBlinking) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBlinkTime >= gestureCooldownMs) {
                        lastBlinkTime = currentTime
                        listener.onGestureDetected(GestureType.BLINK)
                    }
                }
            }

            // Process jaw open gesture using configured threshold
            val currentlyJawOpen = result.isJawOpen(jawOpenThreshold)
            if (currentlyJawOpen != isJawOpened) {
                isJawOpened = currentlyJawOpen
                listener.onGestureStateChanged(GestureType.JAW_OPEN, currentlyJawOpen)

                if (currentlyJawOpen) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastJawOpenTime >= gestureCooldownMs) {
                        lastJawOpenTime = currentTime
                        listener.onGestureDetected(GestureType.JAW_OPEN)
                    }
                }
            }

            // Process smile gesture using configured threshold
            val currentlySmiling = result.isMouthSmile(smileThreshold)
            if (currentlySmiling != isSmiling) {
                isSmiling = currentlySmiling
                listener.onGestureStateChanged(GestureType.SMILE, currentlySmiling)

                if (currentlySmiling) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSmileTime >= gestureCooldownMs) {
                        lastSmileTime = currentTime
                        listener.onGestureDetected(GestureType.SMILE)
                    }
                }
            }
        }
    }

    /**
     * Handle the case when no face is detected in the frame.
     */
    private fun handleEmptyResults() {
        if (isFaceDetected) {
            isFaceDetected = false
            listener.onFaceDetected(false)
        }

        if (isInPosition) {
            isInPosition = false
            listener.onFaceInPosition(false)
        }

        // Reset gesture states
        if (isBlinked) {
            isBlinked = false
            listener.onGestureStateChanged(GestureType.BLINK, false)
        }

        if (isJawOpened) {
            isJawOpened = false
            listener.onGestureStateChanged(GestureType.JAW_OPEN, false)
        }

        if (isSmiling) {
            isSmiling = false
            listener.onGestureStateChanged(GestureType.SMILE, false)
        }
    }

    /**
     * Release resources used by the face gesture detector.
     */
    fun shutdown() {
        faceLandmarkerHelper.clearFaceLandmarker()
        analysisExecutor.shutdown()
    }
}