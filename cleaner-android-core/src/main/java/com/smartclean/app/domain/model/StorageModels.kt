package com.smartclean.app.domain.model

import java.io.File

/**
 * Global Storage & RAM Telemetry model
 */
data class StorageStats(
    val totalBytes: Long,
    val usedBytes: Long,
    val freeBytes: Long,
    val totalRamBytes: Long,
    val usedRamBytes: Long,
    val batteryLevel: Int,
    val deviceTempCelsius: Float,
    val healthScore: Int
) {
    val usedPercentage: Int
        get() = if (totalBytes > 0) ((usedBytes.toDouble() / totalBytes) * 100).toInt() else 0

    val ramUsedPercentage: Int
        get() = if (totalRamBytes > 0) ((usedRamBytes.toDouble() / totalRamBytes) * 100).toInt() else 0

    val formattedTotal: String
        get() = formatBytes(totalBytes)

    val formattedUsed: String
        get() = formatBytes(usedBytes)

    val formattedFree: String
        get() = formatBytes(freeBytes)

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
        }
    }
}

/**
 * Model representing an empty directory found during scan
 */
data class EmptyFolderItem(
    val file: File,
    val name: String = file.name,
    val absolutePath: String = file.absolutePath,
    val lastModified: Long = file.lastModified(),
    var isSelected: Boolean = true
)

/**
 * Group of exact duplicate files matching SHA-256 hash
 */
data class DuplicateFileGroup(
    val sha256Hash: String,
    val fileSize: Long,
    val files: List<DuplicateFileItem>
) {
    val wastedBytes: Long
        get() = if (files.size > 1) fileSize * (files.size - 1) else 0L

    val formattedWastedSize: String
        get() = StorageStats.formatBytes(wastedBytes)
}

data class DuplicateFileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isOriginal: Boolean,
    var isSelectedForDeletion: Boolean = !isOriginal
)

/**
 * Group of visually similar photos detected by dHash + Hamming distance
 */
data class SimilarPhotoGroup(
    val groupId: String,
    val similarityPercentage: Int,
    val photos: List<SimilarPhotoItem>
)

data class SimilarPhotoItem(
    val file: File,
    val dHash: Long,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val isBestQuality: Boolean,
    var isSelectedForDeletion: Boolean = !isBestQuality
)
