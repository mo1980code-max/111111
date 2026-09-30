package com.smartclean.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartclean.app.domain.model.StorageStats
import com.smartclean.app.domain.usecase.GetStorageStatsUseCase
import com.smartclean.app.service.StorageScanForegroundService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val storageStats: StorageStats = StorageStats(
        totalBytes = 128L * 1024 * 1024 * 1024,
        usedBytes = 104L * 1024 * 1024 * 1024,
        freeBytes = 24L * 1024 * 1024 * 1024,
        totalRamBytes = 8L * 1024 * 1024 * 1024,
        usedRamBytes = 5800L * 1024 * 1024,
        batteryLevel = 84,
        deviceTempCelsius = 36.5f,
        healthScore = 68
    ),
    val emptyFoldersCount: Int = 18,
    val duplicatePhotosCount: Int = 42,
    val duplicatePhotosSizeMB: Long = 3120,
    val duplicateVideosCount: Int = 8,
    val duplicateVideosSizeMB: Long = 4850,
    val duplicateFilesCount: Int = 24,
    val duplicateFilesSizeMB: Long = 1850,
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val scanStatusText: String = "جاهز للفحص"
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getStorageStatsUseCase: GetStorageStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeBackgroundScan()
    }

    private fun observeBackgroundScan() {
        viewModelScope.launch {
            StorageScanForegroundService.scanProgressFlow.collect { event ->
                _uiState.update { current ->
                    current.copy(
                        isScanning = !event.isCompleted,
                        scanProgress = event.progressPercent,
                        scanStatusText = event.stageMessage
                    )
                }
            }
        }
    }

    fun onQuickCleanClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, scanStatusText = "جارٍ التنظيف السريع...") }
            // Simulating instant cleanup
            kotlinx.coroutines.delay(1200)
            _uiState.update { current ->
                val freed = 14L * 1024 * 1024 * 1024
                current.copy(
                    isScanning = false,
                    scanProgress = 100,
                    scanStatusText = "تم التنظيف بنجاح!",
                    storageStats = current.storageStats.copy(
                        usedBytes = current.storageStats.usedBytes - freed,
                        freeBytes = current.storageStats.freeBytes + freed,
                        healthScore = 96
                    ),
                    emptyFoldersCount = 0,
                    duplicateFilesCount = 0
                )
            }
        }
    }
}
