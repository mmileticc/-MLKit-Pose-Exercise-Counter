package dev.milinko.workoutapp.exercise

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class PushUpAnalyzer : ExerciseAnalyzer {
    private var lastDown = false
    private var count = 0

    override fun analyze(poseLandmarks: Map<Int, PoseLandmark>): ExerciseResult {
        val leftShoulder = poseLandmarks[PoseLandmark.LEFT_SHOULDER]
        val leftElbow = poseLandmarks[PoseLandmark.LEFT_ELBOW]
        val leftWrist = poseLandmarks[PoseLandmark.LEFT_WRIST]

        val rightShoulder = poseLandmarks[PoseLandmark.RIGHT_SHOULDER]
        val rightElbow = poseLandmarks[PoseLandmark.RIGHT_ELBOW]
        val rightWrist = poseLandmarks[PoseLandmark.RIGHT_WRIST]

        // Biramo stranu koja je bolje detektovana (veći in-frame confidence)
        val leftConfidence = (leftShoulder?.inFrameLikelihood ?: 0f) + 
                         (leftElbow?.inFrameLikelihood ?: 0f) + 
                         (leftWrist?.inFrameLikelihood ?: 0f)
        
        val rightConfidence = (rightShoulder?.inFrameLikelihood ?: 0f) + 
                          (rightElbow?.inFrameLikelihood ?: 0f) + 
                          (rightWrist?.inFrameLikelihood ?: 0f)

        val (shoulder, elbow, wrist) = if (leftConfidence >= rightConfidence) {
            Triple(leftShoulder, leftElbow, leftWrist)
        } else {
            Triple(rightShoulder, rightElbow, rightWrist)
        }

        if (shoulder == null || elbow == null || wrist == null) {
            return ExerciseResult(count, false, 0.0)
        }

        val angle = calculateAngle(shoulder, elbow, wrist)

        if (angle < 70) {
            lastDown = true
        } else if (angle > 160 && lastDown) {
            count++
            lastDown = false
        }

        return ExerciseResult(count, angle in 60.0..175.0, angle)
    }

    override fun reset() {
        count = 0
        lastDown = false
    }

    private fun calculateAngle(a: PoseLandmark, b: PoseLandmark, c: PoseLandmark): Double {
        val abX = a.position.x - b.position.x
        val abY = a.position.y - b.position.y
        val cbX = c.position.x - b.position.x
        val cbY = c.position.y - b.position.y

        val dot = abX * cbX + abY * cbY
        val magAB = sqrt(abX * abX + abY * abY)
        val magCB = sqrt(cbX * cbX + cbY * cbY)

        return Math.toDegrees(acos(dot / (magAB * magCB)).toDouble())
    }
}
