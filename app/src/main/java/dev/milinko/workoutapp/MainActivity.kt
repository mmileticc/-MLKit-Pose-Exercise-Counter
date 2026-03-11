package dev.milinko.workoutapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.milinko.workoutapp.ui.ExerciseScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Launcher za runtime permission
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                startCamera()
            } else {
                // Ako korisnik odbije, možeš prikazati poruku ili fallback UI
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Provera da li je kamera već odobrena
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            // Ako nije, traži dozvolu
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        setContent {
            ExerciseScreen() // Tvoj Compose UI sa kamerom i overlay-om
        }
    }
}