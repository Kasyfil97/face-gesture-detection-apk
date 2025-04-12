package com.example.facegesturedetection

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sdk.models.GestureType

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Try to load OpenCV - this would be needed for more advanced pose estimation
        // but we're not using it in our simplified example
        try {
            System.loadLibrary("opencv_java4")
            Log.d("OpenCV", "OpenCV loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("OpenCV", "Failed to load OpenCV: ${e.message}")
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FaceGestureApp()
                }
            }
        }
    }
}

@Composable
fun FaceGestureApp() {
    // Track detection states
    var isFaceDetected by remember { mutableStateOf(false) }
    var isFaceInPosition by remember { mutableStateOf(false) }

    // Track gesture states
    var isBlinking by remember { mutableStateOf(false) }
    var isJawOpen by remember { mutableStateOf(false) }
    var isSmiling by remember { mutableStateOf(false) }

    // Count gesture detections
    var blinkCount by remember { mutableIntStateOf(0) }
    var jawOpenCount by remember { mutableIntStateOf(0) }
    var smileCount by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview takes up the full screen
        CameraPreview(
            modifier = Modifier.fillMaxSize(),
            onFaceDetected = { detected ->
                isFaceDetected = detected
            },
            onFaceInPosition = { inPosition ->
                isFaceInPosition = inPosition
            },
            onGestureDetected = { gestureType ->
                when (gestureType) {
                    GestureType.BLINK -> blinkCount++
                    GestureType.JAW_OPEN -> jawOpenCount++
                    GestureType.SMILE -> smileCount++
                }
            },
            onGestureStateChanged = { gestureType, isActive ->
                when (gestureType) {
                    GestureType.BLINK -> isBlinking = isActive
                    GestureType.JAW_OPEN -> isJawOpen = isActive
                    GestureType.SMILE -> isSmiling = isActive
                }
            }
        )

        // Overlay UI elements
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title at the top
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1A).copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Face Gesture Detection",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Try blinking, opening your jaw, or smiling",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Status indicators at the bottom
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A1A).copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Face detection status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(isActive = isFaceDetected)
                        Text(
                            text = "Face Detected",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    // Face position status
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusIndicator(isActive = isFaceInPosition)
                        Text(
                            text = "Face in Position",
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Gesture status section
                    Text(
                        text = "Detected Gestures",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Blink status
                    GestureStatusRow(
                        gestureType = "Blink",
                        isActive = isBlinking,
                        count = blinkCount
                    )

                    // Jaw Open status
                    GestureStatusRow(
                        gestureType = "Jaw Open",
                        isActive = isJawOpen,
                        count = jawOpenCount
                    )

                    // Smile status
                    GestureStatusRow(
                        gestureType = "Smile",
                        isActive = isSmiling,
                        count = smileCount
                    )
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(isActive: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(if (isActive) Color.Green else Color.Red)
    )
}

@Composable
fun GestureStatusRow(
    gestureType: String,
    isActive: Boolean,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusIndicator(isActive = isActive)
            Text(
                text = gestureType,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        Text(
            text = "Count: $count",
            color = Color.White,
            fontSize = 16.sp
        )
    }
}