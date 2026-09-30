package com.smartclean.app.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartclean.app.permission.StoragePermissionManager
import com.smartclean.app.presentation.screens.DashboardScreen
import com.smartclean.app.presentation.screens.DuplicatePhotosScreen
import com.smartclean.app.presentation.screens.EmptyFoldersScreen
import com.smartclean.app.presentation.viewmodel.DashboardViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val dashboardViewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check & request storage permissions if not granted
        if (!StoragePermissionManager.hasStoragePermission(this)) {
            StoragePermissionManager.requestStoragePermission(this)
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF0B0F19)
                ) {
                    SmartCleanAppNavigation(dashboardViewModel = dashboardViewModel)
                }
            }
        }
    }
}

@Composable
fun SmartCleanAppNavigation(dashboardViewModel: DashboardViewModel) {
    val navController = rememberNavController()
    val uiState by dashboardViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "dashboard"
    ) {
        composable("dashboard") {
            DashboardScreen(
                uiState = uiState,
                onNavigateToEmptyFolders = { navController.navigate("empty_folders") },
                onNavigateToDuplicatePhotos = { navController.navigate("duplicate_photos") },
                onNavigateToDuplicateVideos = { navController.navigate("dashboard") },
                onNavigateToDuplicateFiles = { navController.navigate("dashboard") },
                onQuickScanClicked = { dashboardViewModel.onQuickCleanClicked() }
            )
        }

        composable("empty_folders") {
            EmptyFoldersScreen(
                emptyFolders = emptyList(),
                onDeleteFolders = { /* Handled via repository */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("duplicate_photos") {
            DuplicatePhotosScreen(
                photoGroups = emptyList(),
                onDeleteSelectedPhotos = { /* Handled via repository */ },
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
