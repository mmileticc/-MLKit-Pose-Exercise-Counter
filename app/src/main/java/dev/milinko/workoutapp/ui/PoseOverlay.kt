package dev.milinko.workoutapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.google.mlkit.vision.pose.PoseLandmark

@Composable
fun PoseOverlay(
    landmarks: Map<Int, PoseLandmark>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        // Crtamo krugove za sve detektovane tačke
        landmarks.values.forEach { landmark ->
            drawCircle(
                color = Color.White.copy(alpha = 0.5f),
                radius = 6f,
                center = Offset(landmark.position.x, landmark.position.y)
            )
        }

        // Definišemo parove tačaka koje treba povezati (kostur)
        val connections = listOf(
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_ELBOW,
            PoseLandmark.LEFT_ELBOW to PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_ELBOW,
            PoseLandmark.RIGHT_ELBOW to PoseLandmark.RIGHT_WRIST,
            PoseLandmark.LEFT_SHOULDER to PoseLandmark.LEFT_HIP,
            PoseLandmark.RIGHT_SHOULDER to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.RIGHT_HIP,
            PoseLandmark.LEFT_HIP to PoseLandmark.LEFT_KNEE,
            PoseLandmark.RIGHT_HIP to PoseLandmark.RIGHT_KNEE,
            PoseLandmark.LEFT_KNEE to PoseLandmark.LEFT_ANKLE,
            PoseLandmark.RIGHT_KNEE to PoseLandmark.RIGHT_ANKLE
        )

        connections.forEach { (startType, endType) ->
            val start = landmarks[startType]
            val end = landmarks[endType]
            if (start != null && end != null) {
                drawLine(
                    color = Color.Cyan,
                    start = Offset(start.position.x, start.position.y),
                    end = Offset(end.position.x, end.position.y),
                    strokeWidth = 6f
                )
            }
        }
        
        // Istaknimo zglobove koji se analiziraju za sklekove
        val activeLandmarks = listOf(
            PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST,
            PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST
        )
        
        activeLandmarks.forEach { type ->
            landmarks[type]?.let { landmark ->
                drawCircle(
                    color = Color.Yellow,
                    radius = 10f,
                    center = Offset(landmark.position.x, landmark.position.y)
                )
            }
        }
    }
}
