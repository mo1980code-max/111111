package com.smartclean.app.domain.repository

import com.smartclean.app.domain.model.DuplicateFileGroup
import com.smartclean.app.domain.model.EmptyFolderItem
import com.smartclean.app.domain.model.SimilarPhotoGroup
import com.smartclean.app.domain.model.StorageStats
import kotlinx.coroutines.flow.Flow
import java.io.File

interface StorageScannerRepository {
    fun getStorageStats(): Flow<StorageStats>
    fun scanEmptyDirectories(rootDir: File): Flow<List<EmptyFolderItem>>
    fun scanDuplicateFiles(rootDir: File): Flow<List<DuplicateFileGroup>>
    fun scanSimilarImages(rootDir: File, maxHammingDistance: Int = 5): Flow<List<SimilarPhotoGroup>>
    suspend fun deleteFiles(files: List<File>): Result<Long>
    suspend fun deleteEmptyDirectories(dirs: List<File>): Result<Int>
}
