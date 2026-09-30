package com.smartclean.app.data.scanner

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 2️⃣ Exact Duplicate Files Scanner by SHA-256 Hash
 * 2-Phase Engine:
 * 1. Filter and group by file length (skips unique file sizes immediately to save CPU/battery)
 * 2. Calculate cryptographic SHA-256 chunked stream digest for files of identical size only
 */
@Singleton
class DuplicateFileScanner @Inject constructor() {

    fun findDuplicateFiles(files: List<File>): Map<String, List<File>> {
        // Phase 1: Group by file size in bytes (ignore unique sizes and 0-byte files)
        val filesBySize = files.asSequence()
            .filter { it.isFile && it.length() > 0 }
            .groupBy { it.length() }
            .filter { it.value.size > 1 }

        // Phase 2: Compute SHA-256 hash only for candidate groups
        val duplicatesByHash = mutableMapOf<String, MutableList<File>>()

        for ((_, candidateFiles) in filesBySize) {
            for (file in candidateFiles) {
                val hash = calculateSHA256(file)
                if (hash != null) {
                    duplicatesByHash.getOrPut(hash) { mutableListOf() }.add(file)
                }
            }
        }

        return duplicatesByHash.filter { it.value.size > 1 }
    }

    private fun calculateSHA256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }
}
