package com.smartclean.app.data.repository

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.smartclean.app.data.scanner.DuplicateFileScanner
import com.smartclean.app.data.scanner.EmptyFolderScanner
import com.smartclean.app.data.scanner.SimilarImageScanner
import com.smartclean.app.domain.model.*
import com.smartclean.app.domain.repository.StorageScannerRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageScannerRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val emptyFolderScanner: EmptyFolderScanner,
    private val duplicateScanner: DuplicateFileScanner,
    private val similarImageScanner: SimilarImageScanner
) : StorageScannerRepository {

    override fun getStorageStats(): Flow<StorageStats> = flow {
        val stat = StatFs(Environment.getDataDirectory().path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong

        val totalBytes = totalBlocks * blockSize
        val freeBytes = availableBlocks * blockSize
        val usedBytes = totalBytes - freeBytes

        // RAM info
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalRam = memInfo.totalMem
        val usedRam = totalRam - memInfo.availMem

        val stats = StorageStats(
            totalBytes = totalBytes,
            usedBytes = usedBytes,
            freeBytes = freeBytes,
            totalRamBytes = totalRam,
            usedRamBytes = usedRam,
            batteryLevel = 84,
            deviceTempCelsius = 36.5f,
            healthScore = if (totalBytes > 0 && (usedBytes.toDouble() / totalBytes) > 0.85) 62 else 94
        )
        emit(stats)
    }.flowOn(Dispatchers.IO)

    override fun scanEmptyDirectories(rootDir: File): Flow<List<EmptyFolderItem>> = flow {
        val emptyFiles = emptyFolderScanner.scanEmptyDirectories(rootDir)
        val items = emptyFiles.map { EmptyFolderItem(file = it) }
        emit(items)
    }.flowOn(Dispatchers.IO)

    override fun scanDuplicateFiles(rootDir: File): Flow<List<DuplicateFileGroup>> = flow {
        val allFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.length() > 0 }
            .toList()

        val duplicatesMap = duplicateScanner.findDuplicateFiles(allFiles)
        val groups = duplicatesMap.map { (hash, files) ->
            val items = files.mapIndexed { index, file ->
                DuplicateFileItem(
                    file = file,
                    isOriginal = index == 0,
                    isSelectedForDeletion = index != 0
                )
            }
            DuplicateFileGroup(
                sha256Hash = hash,
                fileSize = files.firstOrNull()?.length() ?: 0L,
                files = items
            )
        }
        emit(groups)
    }.flowOn(Dispatchers.IO)

    override fun scanSimilarImages(rootDir: File, maxHammingDistance: Int): Flow<List<SimilarPhotoGroup>> = flow {
        val imageFiles = rootDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("jpg", "jpeg", "png", "webp") }
            .take(200)
            .toList()

        val hashedList = imageFiles.mapNotNull { file ->
            val hash = similarImageScanner.getdHash(file.absolutePath)
            if (hash != 0L) Pair(file, hash) else null
        }

        val groups = mutableListOf<SimilarPhotoGroup>()
        val visited = mutableSetOf<File>()

        for (i in hashedList.indices) {
            val (fileA, hashA) = hashedList[i]
            if (visited.contains(fileA)) continue

            val cluster = mutableListOf(fileA)
            visited.add(fileA)

            for (j in i + 1 until hashedList.size) {
                val (fileB, hashB) = hashedList[j]
                if (visited.contains(fileB)) continue

                val distance = similarImageScanner.calculateSimilarityDistance(hashA, hashB)
                if (distance <= maxHammingDistance) {
                    cluster.add(fileB)
                    visited.add(fileB)
                }
            }

            if (cluster.size > 1) {
                val sortedByQuality = cluster.sortedByDescending { it.length() }
                val photoItems = sortedByQuality.mapIndexed { index, file ->
                    SimilarPhotoItem(
                        file = file,
                        dHash = hashA,
                        width = 4032,
                        height = 3024,
                        fileSize = file.length(),
                        isBestQuality = index == 0,
                        isSelectedForDeletion = index != 0
                    )
                }

                groups.add(
                    SimilarPhotoGroup(
                        groupId = "grp_${groups.size + 1}",
                        similarityPercentage = 98,
                        photos = photoItems
                    )
                )
            }
        }
        emit(groups)
    }.flowOn(Dispatchers.IO)

    override suspend fun deleteFiles(files: List<File>): Result<Long> = withContext(Dispatchers.IO) {
        try {
            var freedBytes = 0L
            files.forEach { file ->
                if (file.exists() && file.isFile) {
                    val length = file.length()
                    if (file.delete()) {
                        freedBytes += length
                    }
                }
            }
            Result.success(freedBytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteEmptyDirectories(dirs: List<File>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var count = 0
            dirs.forEach { dir ->
                if (dir.exists() && dir.isDirectory) {
                    if (dir.delete()) {
                        count++
                    }
                }
            }
            Result.success(count)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
