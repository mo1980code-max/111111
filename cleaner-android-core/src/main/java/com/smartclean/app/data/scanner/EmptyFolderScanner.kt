package com.smartclean.app.data.scanner

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 1️⃣ Empty Folders Cleaner Algorithm
 * Recursively scans directories to detect completely empty folders,
 * strictly skipping system directories, app sandbox containers, and protected paths.
 */
@Singleton
class EmptyFolderScanner @Inject constructor() {

    // Protected paths that MUST NEVER be pruned or scanned as empty
    private val blacklistedPaths = setOf(
        "/Android/data",
        "/Android/obb",
        "/system",
        "/sys",
        "/proc",
        "/.thumbnails",
        "/DCIM/.thumbnails"
    )

    fun scanEmptyDirectories(rootDir: File): List<File> {
        val emptyDirs = mutableListOf<File>()
        scanRecursive(rootDir, emptyDirs)
        return emptyDirs
    }

    private fun scanRecursive(dir: File, result: MutableList<File>): Boolean {
        if (!dir.exists() || !dir.isDirectory || isBlacklisted(dir)) {
            return false
        }

        val files = dir.listFiles()
        // If directory has no children files or subfolders, it is empty
        if (files == null || files.isEmpty()) {
            result.add(dir)
            return true
        }

        var allChildrenAreEmpty = true
        for (file in files) {
            if (file.isDirectory) {
                val isChildEmpty = scanRecursive(file, result)
                if (!isChildEmpty) {
                    allChildrenAreEmpty = false
                }
            } else {
                allChildrenAreEmpty = false
            }
        }

        // If all subfolders inside this folder are empty and no actual files exist, add this parent too
        if (allChildrenAreEmpty) {
            result.add(dir)
        }
        return allChildrenAreEmpty
    }

    private fun isBlacklisted(dir: File): Boolean {
        val path = dir.absolutePath
        return blacklistedPaths.any { path.contains(it) } || dir.name.startsWith(".")
    }
}
