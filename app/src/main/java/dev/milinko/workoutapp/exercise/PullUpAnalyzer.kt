package dev.milinko.workoutapp.exercise

import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.acos
import kotlin.math.sqrt

class PullUpAnalyzer : ExerciseAnalyzer {
    private var count = 0
    private var isDown = true // Stanje: Dole (ruke opružene)
    private val angleBuffer = mutableListOf<Double>()
    private val BUFFER_SIZE = 8
    private var lastSmoothAngle = 0.0
    private val ALPHA = 0.2

    // Praćenje kretanja šaka i tela
    private var referenceWristY: Float? = null
    private var initialShoulderWristDist: Float? = null
    
    // Baferi za stabilnost šaka
    private val wristYBuffer = mutableListOf<Float>()
    private val WRIST_STABILITY_THRESHOLD = 1.25f // Još labaviji prag stabilnosti
    private var areHandsFixed = false
    private var handsFixedStartTime: Long = 0
    private val STABILITY_DURATION_MS = 150 // Malo duže za sigurnost, ali i dalje brzo
    private var lastStabilityCheckTime: Long = 0
    private var smoothWristY: Float? = null
    private val WRIST_ALPHA = 0.25f // Mirniji filter za šake
    
    private var hasStartedHanging = false // Da li smo se bar jednom skroz opružili (viseći stav)

    override fun analyze(poseLandmarks: Map<Int, PoseLandmark>): ExerciseResult {
        val leftShoulder = poseLandmarks[PoseLandmark.LEFT_SHOULDER]
        val leftElbow = poseLandmarks[PoseLandmark.LEFT_ELBOW]
        val leftWrist = poseLandmarks[PoseLandmark.LEFT_WRIST]
        val rightShoulder = poseLandmarks[PoseLandmark.RIGHT_SHOULDER]
        val rightElbow = poseLandmarks[PoseLandmark.RIGHT_ELBOW]
        val rightWrist = poseLandmarks[PoseLandmark.RIGHT_WRIST]

        val leftHip = poseLandmarks[PoseLandmark.LEFT_HIP]
        val rightHip = poseLandmarks[PoseLandmark.RIGHT_HIP]
        val leftKnee = poseLandmarks[PoseLandmark.LEFT_KNEE]
        val rightKnee = poseLandmarks[PoseLandmark.RIGHT_KNEE]
        val leftAnkle = poseLandmarks[PoseLandmark.LEFT_ANKLE]
        val rightAnkle = poseLandmarks[PoseLandmark.RIGHT_ANKLE]

        val leftConf = (leftShoulder?.inFrameLikelihood ?: 0f) + (leftElbow?.inFrameLikelihood ?: 0f) + (leftWrist?.inFrameLikelihood ?: 0f)
        val rightConf = (rightShoulder?.inFrameLikelihood ?: 0f) + (rightElbow?.inFrameLikelihood ?: 0f) + (rightWrist?.inFrameLikelihood ?: 0f)

        val (shoulder, elbow, wrist, hip, knee) = if (leftConf >= rightConf) {
            listOf(leftShoulder, leftElbow, leftWrist, leftHip, leftKnee)
        } else {
            listOf(rightShoulder, rightElbow, rightWrist, rightHip, rightKnee)
        }
        val ankle = if (leftConf >= rightConf) leftAnkle else rightAnkle

        // 1. Provera: Osnovni zglobovi u kadru
        val hasArms = shoulder != null && elbow != null && wrist != null &&
                      shoulder.inFrameLikelihood > 0.4f && wrist.inFrameLikelihood > 0.4f
        val hasUpperBody = hasArms && hip != null && hip.inFrameLikelihood > 0.3f
        val hasFullBody = hasUpperBody && knee != null && ankle != null

        if (!hasArms) {
            // Ako ruka nije u kadru, potpuno resetujemo sve za stabilnost
            handsFixedStartTime = 0L
            areHandsFixed = false
            wristYBuffer.clear()
            referenceWristY = null
            return ExerciseResult(
                count = count,
                isCorrectForm = false,
                currentAngle = 0.0,
                isUserInFrame = false,
                visibilityMessage = "GET IN FRAME (ARMS & SHOULDERS)",
                areHandsFixed = false
            )
        }

        // 2. Provera: Stabilnost šaka (EMA + Buffer)
        val currentWristY = wrist!!.position.y
        smoothWristY = if (smoothWristY == null) currentWristY else (WRIST_ALPHA * currentWristY) + ((1 - WRIST_ALPHA) * smoothWristY!!)
        
        wristYBuffer.add(smoothWristY!!)
        if (wristYBuffer.size > 15) wristYBuffer.removeAt(0) // Nešto veći bafer za bolji prosek
        
        // AKO SE DESI EKSTREMAN SKOK (npr. zamena korisnika), resetujemo
        if (Math.abs(currentWristY - (wristYBuffer.firstOrNull() ?: currentWristY)) > 15.0f) {
            wristYBuffer.clear()
            areHandsFixed = false
            handsFixedStartTime = 0L
            referenceWristY = null
            smoothWristY = null
        }
        
        val wristRange = if (wristYBuffer.size >= 3) wristYBuffer.max() - wristYBuffer.min() else 0.0f
        val isCurrentlyStable = wristRange < WRIST_STABILITY_THRESHOLD

        // Histereza: Čak i ako nisu savršeno fiksirane, koristimo prosečnu Y vrednost kao referencu
        if (!isCurrentlyStable) {
            // Ako već imamo fiksirane šake, tolerišemo OGROMNE oscilacije (pomeranje u toku zgiba)
            val breakThreshold = if (areHandsFixed) WRIST_STABILITY_THRESHOLD * 6.5f else WRIST_STABILITY_THRESHOLD
            if (wristRange > breakThreshold) {
                areHandsFixed = false
                handsFixedStartTime = 0L
            }
        } else {
            if (handsFixedStartTime == 0L) handsFixedStartTime = System.currentTimeMillis()
            if (System.currentTimeMillis() - handsFixedStartTime > STABILITY_DURATION_MS) {
                if (!areHandsFixed) {
                    areHandsFixed = true
                    // Uvek ažuriramo referentnu vrednost kad god je stabilno, ali blago
                    val newRef = wristYBuffer.average().toFloat()
                    referenceWristY = if (referenceWristY == null) newRef else (0.3f * newRef) + (0.7f * referenceWristY!!)
                    
                    if (initialShoulderWristDist == null) {
                        initialShoulderWristDist = Math.abs(referenceWristY!! - shoulder!!.position.y)
                    }
                }
            }
        }

        // Ako šake nisu fiksirane, ipak dopuštamo brojanje ako imamo neku referencu od ranije
        // (ovo sprečava gubljenje sesije usled sitnog šuma)
        if (!areHandsFixed && referenceWristY == null) {
            val stabilityProgress = if (handsFixedStartTime != 0L) {
                val progress = (System.currentTimeMillis() - handsFixedStartTime).toFloat() / STABILITY_DURATION_MS
                " (${(progress * 100).coerceAtMost(100f).toInt()}%)"
            } else ""
            return ExerciseResult(
                count = count,
                isCorrectForm = false,
                currentAngle = 0.0,
                isUserInFrame = true,
                visibilityMessage = "GRAB THE BAR AND KEEP HANDS STILL$stabilityProgress",
                areHandsFixed = false
            )
        }

        val angle = calculateAngle(shoulder!!, elbow!!, wrist)
        
        // Smoothing
        angleBuffer.add(angle)
        if (angleBuffer.size > BUFFER_SIZE) angleBuffer.removeAt(0)
        val averageAngle = angleBuffer.average()
        val smoothAngle = if (lastSmoothAngle == 0.0) averageAngle else (ALPHA * averageAngle) + ((1 - ALPHA) * lastSmoothAngle)
        lastSmoothAngle = smoothAngle

        // Rastojanje šaka-rame (po Y osi - visina)
        val currentDist = Math.abs((referenceWristY ?: wrist.position.y) - shoulder!!.position.y)

        // Ažuriramo maksimalno rastojanje (donji položaj) dinamički
        if (smoothAngle > 135) {
            if (initialShoulderWristDist == null || currentDist > initialShoulderWristDist!!) {
                initialShoulderWristDist = currentDist
            }
        }

        // DETEKCIJA POKRETA (Zgib)
        val distRatio = if (initialShoulderWristDist != null && initialShoulderWristDist!! > 0) currentDist / initialShoulderWristDist!! else 1.0f
        
        // Glavni uslov: Rame se podiglo uvis u odnosu na (fiksiranu) šaku
        // Dozvoljavamo veću grešku u Y poziciji šaka (pomeranje šipke/kadriranje)
        val bodyMovedUp = shoulder!!.position.y < ( (referenceWristY ?: wrist.position.y) - (initialShoulderWristDist ?: 0.2f) * 0.10f)

        // Pre nego što bilo šta brojimo, korisnik mora da se bar jednom SKROZ opruži
        // dok su mu ruke fiksirane (Hanging position) da bi se znalo da počinje serija
        if (!hasStartedHanging && smoothAngle > 140) {
            hasStartedHanging = true
            isDown = true
        }

        if (hasStartedHanging) {
            if (isDown && smoothAngle < 115 && (distRatio < 0.88f || bodyMovedUp)) {
                isDown = false
            } 
            else if (!isDown && (smoothAngle > 135 || (distRatio > 0.94f && smoothAngle > 120))) {
                count++
                isDown = true
            }
        }

        var visibilityMessage: String? = null
        if (!hasFullBody && !areHandsFixed) {
            visibilityMessage = if (!hasUpperBody) "STAND FURTHER TO SEE HIPS" else null
        }
        
        // Prikazujemo uputstva samo dok sesija nije započeta
        if (!hasStartedHanging) {
            if (areHandsFixed) {
                visibilityMessage = "FULLY EXTEND ARMS TO START"
            }
            
            // Ova poruka je bila dosadna, sada je prikazujemo samo ako je drastično pogrešno 
            // i ako serija još nije krenula kako treba (npr. pogrešan smer kamere)
            if (shoulder!!.position.y > (referenceWristY ?: wrist.position.y) + 0.1f && smoothAngle > 120) {
                visibilityMessage = "BAR SHOULD BE ABOVE HEAD"
            }
        }

        return ExerciseResult(
            count = count,
            isCorrectForm = smoothAngle in 35.0..180.0,
            currentAngle = smoothAngle,
            isUserInFrame = true,
            visibilityMessage = visibilityMessage,
            areHandsFixed = areHandsFixed
        )
    }

    override fun reset() {
        count = 0
        isDown = true
        hasStartedHanging = false
        angleBuffer.clear()
        lastSmoothAngle = 0.0
        referenceWristY = null
        initialShoulderWristDist = null
        wristYBuffer.clear()
        areHandsFixed = false
        handsFixedStartTime = 0L
        lastStabilityCheckTime = 0L
        smoothWristY = null
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
