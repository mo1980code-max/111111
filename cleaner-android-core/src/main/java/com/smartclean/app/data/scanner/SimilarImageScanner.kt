package com.smartclean.app.data.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 3️⃣ Difference Hash (dHash) Image Similarity Engine
 * Produces a 64-bit perceptual fingerprint of images.
 * Calculates Hamming distance (distance <= 5 represents near-identical visual content).
 */
@Singleton
class SimilarImageScanner @Inject constructor() {

    // Converts an image to 64-bit dHash fingerprint
    fun getdHash(filePath: String): Long {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4 // Subsample to speed up bitmap decoding
        }
        val originalBitmap = BitmapFactory.decodeFile(filePath, options) ?: return 0L

        // Resize bitmap to 9x8 pixels (72 pixels total, gives 8x8 = 64 comparisons)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 9, 8, true)
        var hash = 0L

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val leftPixel = getGrayscale(scaledBitmap.getPixel(col, row))
                val rightPixel = getGrayscale(scaledBitmap.getPixel(col + 1, row))

                // If left pixel is brighter than right pixel, set corresponding bit
                if (leftPixel > rightPixel) {
                    hash = hash or (1L shl (row * 8 + col))
                }
            }
        }
        return hash
    }

    // Calculates Hamming Distance (differing bits). Distance <= 5 indicates high visual similarity
    fun calculateSimilarityDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    // Convert similarity distance to percentage (0 distance = 100%, 64 distance = 0%)
    fun distanceToPercentage(distance: Int): Int {
        val similarity = (1.0 - (distance.toDouble() / 64.0)) * 100.0
        return similarity.toInt().coerceIn(0, 100)
    }

    private fun getGrayscale(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (0.3 * r + 0.59 * g + 0.11 * b).toInt()
    }
}
