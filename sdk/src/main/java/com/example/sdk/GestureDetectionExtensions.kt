package com.example.sdk

import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult

/**
 * Extension functions for detecting specific gestures from FaceLandmarkerResult.
 */

/**
 * Checks if both eyes are blinking in the face landmark result.
 * @param threshold The confidence threshold for considering an eye as blinking (default: 0.7f)
 * @return True if both eyes are blinking, false otherwise.
 */
fun FaceLandmarkerResult.isEyesBlinked(threshold: Float = 0.7f): Boolean {
    if (faceBlendshapes().isEmpty) return false
    val blendshapes = faceBlendshapes().get()[0]

    val leftEye = blendshapes.firstOrNull { it.categoryName() == "eyeBlinkLeft" }
    val rightEye = blendshapes.firstOrNull { it.categoryName() == "eyeBlinkRight" }

    return (leftEye?.score() ?: 0f) > threshold && (rightEye?.score() ?: 0f) > threshold
}

/**
 * Checks if the jaw is open in the face landmark result.
 * @param threshold The confidence threshold for considering the jaw as open (default: 0.4f)
 * @return True if the jaw is open, false otherwise.
 */
fun FaceLandmarkerResult.isJawOpen(threshold: Float = 0.4f): Boolean {
    if (faceBlendshapes().isEmpty) return false
    val blendshapes = faceBlendshapes().get()[0]

    val jawOpen = blendshapes.firstOrNull { it.categoryName() == "jawOpen" }

    return (jawOpen?.score() ?: 0f) > threshold
}

/**
 * Checks if the mouth is smiling in the face landmark result.
 * @param threshold The confidence threshold for considering the mouth as smiling (default: 0.7f)
 * @return True if the mouth is smiling, false otherwise.
 */
fun FaceLandmarkerResult.isMouthSmile(threshold: Float = 0.7f): Boolean {
    if (faceBlendshapes().isEmpty) return false
    val blendshapes = faceBlendshapes().get()[0]

    val mouthSmileLeft = blendshapes.firstOrNull { it.categoryName() == "mouthSmileLeft" }
    val mouthSmileRight = blendshapes.firstOrNull { it.categoryName() == "mouthSmileRight" }

    return (mouthSmileLeft?.score() ?: 0f) > threshold && (mouthSmileRight?.score() ?: 0f) > threshold
}