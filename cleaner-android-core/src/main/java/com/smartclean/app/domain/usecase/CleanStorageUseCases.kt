package com.smartclean.app.domain.usecase

import com.smartclean.app.domain.model.DuplicateFileGroup
import com.smartclean.app.domain.model.EmptyFolderItem
import com.smartclean.app.domain.model.SimilarPhotoGroup
import com.smartclean.app.domain.model.StorageStats
import com.smartclean.app.domain.repository.StorageScannerRepository
import kotlinx.coroutines.flow.Flow
import java.io.File
import javax.inject.Inject

class GetStorageStatsUseCase @Inject constructor(
    private val repository: StorageScannerRepository
) {
    operator fun invoke(): Flow<StorageStats> = repository.getStorageStats()
}

class ScanEmptyFoldersUseCase @Inject constructor(
    private val repository: StorageScannerRepository
) {
    operator fun invoke(rootDir: File): Flow<List<EmptyFolderItem>> =
        repository.scanEmptyDirectories(rootDir)
}

class ScanDuplicateFilesUseCase @Inject constructor(
    private val repository: StorageScannerRepository
) {
    operator fun invoke(rootDir: File): Flow<List<DuplicateFileGroup>> =
        repository.scanDuplicateFiles(rootDir)
}

class ScanSimilarImagesUseCase @Inject constructor(
    private val repository: StorageScannerRepository
) {
    operator fun invoke(rootDir: File, thresholdDistance: Int = 5): Flow<List<SimilarPhotoGroup>> =
        repository.scanSimilarImages(rootDir, thresholdDistance)
}

class DeleteFilesUseCase @Inject constructor(
    private val repository: StorageScannerRepository
) {
    suspend operator fun invoke(files: List<File>): Result<Long> =
        repository.deleteFiles(files)

    suspend fun deleteEmptyDirs(dirs: List<File>): Result<Int> =
        repository.deleteEmptyDirectories(dirs)
}
