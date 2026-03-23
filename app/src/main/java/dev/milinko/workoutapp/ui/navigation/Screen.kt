package dev.milinko.workoutapp.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Training : Screen("training", "Training", Icons.Default.PlayArrow)
    object Statistics : Screen("statistics", "Stats", Icons.Default.History)
}
