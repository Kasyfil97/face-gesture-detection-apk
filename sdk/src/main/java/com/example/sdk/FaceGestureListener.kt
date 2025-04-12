package com.example.sdk

import com.example.sdk.models.GestureType

/**
 * Interface for receiving face gesture detection events.
 */
interface FaceGestureListener {
    /**
     * Called when a face is detected in the frame.
     * @param detected True if a face is detected, false otherwise.
     */
    fun onFaceDetected(detected: Boolean)

    /**
     * Called when the face position is determined to be in the correct position for detection.
     * @param inPosition True if the face is in position, false otherwise.
     */
    fun onFaceInPosition(inPosition: Boolean)

    /**
     * Called when a specific gesture is detected.
     * @param gestureType The type of gesture detected.
     */
    fun onGestureDetected(gestureType: GestureType)

    /**
     * Called when a gesture changes state (started or ended).
     * @param gestureType The type of gesture.
     * @param isActive True if the gesture is active, false if it has ended.
     */
    fun onGestureStateChanged(gestureType: GestureType, isActive: Boolean)

    /**
     * Called when an error occurs during detection.
     * @param errorMessage The error message.
     */
    fun onError(errorMessage: String, errorCode: Int = 0)
}