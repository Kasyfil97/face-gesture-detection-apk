package com.example.facegesturedetection

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.sdk.FaceGestureDetector
import com.example.sdk.FaceGestureListener
import com.example.sdk.models.GestureType
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onFaceDetected: (Boolean) -> Unit,
    onFaceInPosition: (Boolean) -> Unit,
    onGestureDetected: (GestureType) -> Unit,
    onGestureStateChanged: (GestureType, Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember { mutableStateOf(false) }
    val previewView = remember { PreviewView(context) }

    // Create an executor for the image analysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Remember the face gesture detector with customizable parameters
    val faceGestureDetector = remember {
        FaceGestureDetector.Builder(context)
            .setListener(object : FaceGestureListener {
                override fun onFaceDetected(detected: Boolean) {
                    onFaceDetected(detected)
                }

                override fun onFaceInPosition(inPosition: Boolean) {
                    onFaceInPosition(inPosition)
                }

                override fun onGestureDetected(gestureType: GestureType) {
                    onGestureDetected(gestureType)
                }

                override fun onGestureStateChanged(gestureType: GestureType, isActive: Boolean) {
                    onGestureStateChanged(gestureType, isActive)
                }

                override fun onError(errorMessage: String, errorCode: Int) {
                    Log.e(TAG, "Face gesture error: $errorMessage, code: $errorCode")
                }
            })
            // Contoh pengaturan threshold kustom (bisa disesuaikan oleh developer)
            .setBlinkThreshold(0.7f)
            .setJawOpenThreshold(0.4f)
            .setSmileThreshold(0.7f)
            .setGestureCooldown(500L)
            .build()
    }

    // Request camera permission
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasCameraPermission = isGranted
            if (!isGranted) {
                Log.e(TAG, "Camera permission denied")
            }
        }
    )

    LaunchedEffect(Unit) {
        val permissionCheckResult = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        )
        if (permissionCheckResult == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Start camera if we have permission
    if (hasCameraPermission) {
        DisposableEffect(previewView) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val cameraProvider = cameraProviderFuture.get()

            // Set up the preview use case
            val preview = Preview.Builder().build()
            preview.setSurfaceProvider(previewView.surfaceProvider)

            // Set up the image analysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                faceGestureDetector.processImageProxy(imageProxy, true)
            }

            // Select front camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            try {
                // Unbind all use cases before binding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
            }

            onDispose {
                cameraExecutor.shutdown()
                faceGestureDetector.shutdown()
                cameraProvider.unbindAll()
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
    }
}