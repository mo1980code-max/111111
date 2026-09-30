/**
 * Smart Clean AI - Storage State & Simulated Data Store
 * Includes Empty Folders, dHash Perceptual Photo Groups, and Kotlin Code Catalog
 */

const initialStorageState = {
  totalCapacityGB: 128.0,
  usedCapacityGB: 104.2,
  freeCapacityGB: 23.8,
  ramTotalGB: 8.0,
  ramUsedGB: 5.9,
  batteryLevel: 84,
  batteryTempC: 36.5,
  batteryHoursRemaining: 7.2,
  batteryMode: 'normal',
  healthScore: 68,

  // Empty folders dataset
  emptyFolders: [
    { id: 'ef_1', name: 'Telegram_Temp_Unused', path: '/storage/emulated/0/Telegram/Telegram Documents/temp', selected: true },
    { id: 'ef_2', name: 'Download_Old_Unpack', path: '/storage/emulated/0/Download/unpacked_cache_2025', selected: true },
    { id: 'ef_3', name: 'WhatsApp_Animated_Gifs_Empty', path: '/storage/emulated/0/WhatsApp/Media/WhatsApp Animated Gifs/Sent', selected: true },
    { id: 'ef_4', name: 'Old_Recorder_Session_04', path: '/storage/emulated/0/Recordings/Session_4_Trash', selected: true },
    { id: 'ef_5', name: 'Instagram_Story_Drafts', path: '/storage/emulated/0/Pictures/Instagram/draft_temp', selected: true },
    { id: 'ef_6', name: 'Bluetooth_Received_Trash', path: '/storage/emulated/0/Bluetooth/empty_sync', selected: true },
    { id: 'ef_7', name: 'PDF_Reader_Annotation_Tmp', path: '/storage/emulated/0/Documents/PDFReader/tmp', selected: true },
    { id: 'ef_8', name: 'Video_Editor_Cache_Dir', path: '/storage/emulated/0/Movies/CapCut/cache_old', selected: true }
  ],
  
  // Quick clean categories
  quickScanCategories: [
    {
      id: 'emptyDirs',
      key: 'catEmptyDirs',
      descKey: 'catEmptyDirsDesc',
      sizeMB: 12,
      icon: 'folder-x',
      color: 'amber',
      selected: true,
      cleaned: false,
      details: [
        { name: '18 Empty Directories in Download & Telegram', size: '12 MB', path: '/storage/emulated/0/...' }
      ]
    },
    {
      id: 'cache',
      key: 'catAppCache',
      descKey: 'catAppCacheDesc',
      sizeMB: 4320,
      icon: 'zap',
      color: 'amber',
      selected: true,
      cleaned: false,
      details: [
        { name: 'TikTok Cache & Temp Video Blobs', size: '1.45 GB', path: '/data/data/com.zhiliaoapp.musically/cache' },
        { name: 'Chrome Browser Web Cache & Offline data', size: '980 MB', path: '/data/data/com.android.chrome/cache' },
        { name: 'Instagram Image & Reel Cache', size: '820 MB', path: '/data/data/com.instagram.android/cache' },
        { name: 'YouTube Video Chunk Cache', size: '640 MB', path: '/data/data/com.google.android.youtube/cache' },
        { name: 'System UI & WebView Cache', size: '430 MB', path: '/system/cache/webview' }
      ]
    },
    {
      id: 'residual',
      key: 'catResidual',
      descKey: 'catResidualDesc',
      sizeMB: 1850,
      icon: 'package',
      color: 'blue',
      selected: true,
      cleaned: false,
      details: [
        { name: 'base.apk (Telegram v10.4.1 Old Installer)', size: '82 MB', path: '/storage/emulated/0/Download/telegram_10_4.apk' },
        { name: 'pubg_mobile_v3.2_patch.apk.tmp', size: '1.1 GB', path: '/storage/emulated/0/Download/pubg_patch.apk' },
        { name: 'CapCut_VideoEditor_v11.apk', size: '210 MB', path: '/storage/emulated/0/Download/capcut.apk' },
        { name: 'Remnants of uninstalled Twitter (X) folder', size: '448 MB', path: '/storage/emulated/0/Android/data/com.twitter.android.trash' }
      ]
    },
    {
      id: 'duplicates',
      key: 'catDuplicates',
      descKey: 'catDuplicatesDesc',
      sizeMB: 3120,
      icon: 'copy',
      color: 'purple',
      selected: true,
      cleaned: false,
      details: [
        { name: '42 Duplicate Photo Pairs in DCIM/Camera (dHash Fingerprint)', size: '1.8 GB', path: 'DCIM/Camera' },
        { name: '12 Repeated Screen Recordings (SHA-256 Identical)', size: '890 MB', path: 'DCIM/ScreenRecorder' },
        { name: 'Repeated Downloaded PDFs & Invoices', size: '430 MB', path: 'Download' }
      ]
    },
    {
      id: 'social',
      key: 'catSocialJunk',
      descKey: 'catSocialJunkDesc',
      sizeMB: 3840,
      icon: 'message-circle',
      color: 'emerald',
      selected: true,
      cleaned: false,
      details: [
        { name: 'WhatsApp Voice Notes older than 60 days (4,120 memos)', size: '1.9 GB', path: 'WhatsApp/Media/WhatsApp Voice Notes' },
        { name: 'WhatsApp Sent Videos backup copies', size: '1.2 GB', path: 'WhatsApp/Media/WhatsApp Video/Sent' },
        { name: 'Telegram Cached Media & Stickers', size: '740 MB', path: 'Telegram/Telegram Video' }
      ]
    },
    {
      id: 'logs',
      key: 'catTempLogs',
      descKey: 'catTempLogsDesc',
      sizeMB: 1140,
      icon: 'file-text',
      color: 'rose',
      selected: true,
      cleaned: false,
      details: [
        { name: 'Android ANR & Crash Dumps', size: '480 MB', path: '/data/system/dropbox' },
        { name: '.thumbnails Legacy Corrupt Cache', size: '510 MB', path: '/storage/emulated/0/DCIM/.thumbnails' },
        { name: 'system_logcat_rotated.log', size: '150 MB', path: '/cache/recovery/last_log' }
      ]
    }
  ],

  // AI Duplicate Photos Gallery with Difference Hash (dHash)
  duplicateGroups: [
    {
      id: 'grp_1',
      titleAr: 'غروب الشمس على الشاطئ',
      titleEn: 'Sunset at the Beach',
      similarity: 99,
      dHashVal: '0x8FA43C21B40E81AA',
      hammingDistance: 1,
      sizeTotal: '14.2 MB',
      photos: [
        {
          id: 'p1_orig',
          name: 'IMG_20260814_183201.jpg',
          size: '7.2 MB',
          resolution: '4032 × 3024 (4K HDR)',
          date: '14 أغسطس 2026',
          isBest: true,
          selected: false,
          previewUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=80'
        },
        {
          id: 'p1_dup',
          name: 'IMG_20260814_183203_copy.jpg',
          size: '7.0 MB',
          resolution: '4032 × 3024 (Compressed)',
          date: '14 أغسطس 2026',
          isBest: false,
          selected: true,
          previewUrl: 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=600&q=60'
        }
      ]
    },
    {
      id: 'grp_2',
      titleAr: 'صورة شخصية في الطبيعة',
      titleEn: 'Nature Portrait',
      similarity: 97,
      dHashVal: '0x3C8120FF4EA90B12',
      hammingDistance: 2,
      sizeTotal: '18.6 MB',
      photos: [
        {
          id: 'p2_orig',
          name: 'PORTRAIT_20260901_1102.jpg',
          size: '9.8 MB',
          resolution: '4000 × 3000 (RAW HDR)',
          date: '01 سبتمبر 2026',
          isBest: true,
          selected: false,
          previewUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=80'
        },
        {
          id: 'p2_dup',
          name: 'PORTRAIT_20260901_1103_edit.jpg',
          size: '8.8 MB',
          resolution: '3840 × 2880',
          date: '01 سبتمبر 2026',
          isBest: false,
          selected: true,
          previewUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=600&q=60'
        }
      ]
    },
    {
      id: 'grp_3',
      titleAr: 'قهوة الصباح في المقهى',
      titleEn: 'Morning Coffee at Cafe',
      similarity: 95,
      dHashVal: '0x1F2A7E4B65009C34',
      hammingDistance: 3,
      sizeTotal: '11.4 MB',
      photos: [
        {
          id: 'p3_orig',
          name: 'COFFEE_ART_20260920.jpg',
          size: '6.1 MB',
          resolution: '3024 × 4032',
          date: '20 سبتمبر 2026',
          isBest: true,
          selected: false,
          previewUrl: 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=80'
        },
        {
          id: 'p3_dup',
          name: 'COFFEE_ART_BURST_2.jpg',
          size: '5.3 MB',
          resolution: '3024 × 4032',
          date: '20 سبتمبر 2026',
          isBest: false,
          selected: true,
          previewUrl: 'https://images.unsplash.com/photo-1509042239860-f550ce710b93?auto=format&fit=crop&w=600&q=60'
        }
      ]
    }
  ],

  // Blurry & Bad Photos
  blurryPhotos: [
    {
      id: 'blur_1',
      name: 'BLURRY_MOTION_20260910.jpg',
      size: '5.4 MB',
      blurScore: '89% ضبابية وحركة مهتزة',
      date: '10 سبتمبر 2026',
      selected: true,
      previewUrl: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?auto=format&fit=crop&w=600&q=40'
    },
    {
      id: 'blur_2',
      name: 'NIGHT_DARK_OUTOFOCUS.jpg',
      size: '4.8 MB',
      blurScore: '92% إضاءة منعدمة وفقدان التركيز',
      date: '18 سبتمبر 2026',
      selected: true,
      previewUrl: 'https://images.unsplash.com/photo-1509114397022-ed747cca3f65?auto=format&fit=crop&w=600&q=40'
    }
  ],

  // Screenshots
  screenshots: [
    {
      id: 'ss_1',
      name: 'Screenshot_20260710_Amazon_Order.png',
      size: '2.8 MB',
      age: 'منذ 82 يوماً',
      date: '10 يوليو 2026',
      selected: true,
      previewUrl: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=600&q=70'
    },
    {
      id: 'ss_2',
      name: 'Screenshot_20260802_Bank_Transfer.png',
      size: '3.4 MB',
      age: 'منذ 59 يوماً',
      date: '02 أغسطس 2026',
      selected: true,
      previewUrl: 'https://images.unsplash.com/photo-1554224155-8d04cb21cd6c?auto=format&fit=crop&w=600&q=70'
    },
    {
      id: 'ss_3',
      name: 'Screenshot_20260819_Map_Directions.png',
      size: '4.1 MB',
      age: 'منذ 42 يوماً',
      date: '19 أغسطس 2026',
      selected: true,
      previewUrl: 'https://images.unsplash.com/photo-1524661135-423995f22d0b?auto=format&fit=crop&w=600&q=70'
    }
  ],

  // Large Files Catalog
  largeFiles: [
    {
      id: 'lf_1',
      name: 'Trip_To_Switzerland_4K_60FPS.mp4',
      type: 'video',
      sizeMB: 3850,
      path: '/DCIM/Camera/Trip_To_Switzerland_4K_60FPS.mp4',
      date: '12 أغسطس 2026',
      selected: false,
      icon: 'film'
    },
    {
      id: 'lf_2',
      name: 'Course_Full_Stack_Development_Offline.zip',
      type: 'archive',
      sizeMB: 2400,
      path: '/Download/Course_Full_Stack_Development_Offline.zip',
      date: '28 يوليو 2026',
      selected: false,
      icon: 'archive'
    },
    {
      id: 'lf_3',
      name: 'Podcast_AI_Revolution_Season_3_HighRes.wav',
      type: 'audio',
      sizeMB: 890,
      path: '/Music/Podcasts/AI_Revolution_S3.wav',
      date: '05 سبتمبر 2026',
      selected: false,
      icon: 'music'
    },
    {
      id: 'lf_4',
      name: 'UnrealEngine5_Sample_Project_Backup.rar',
      type: 'archive',
      sizeMB: 4200,
      path: '/Download/UnrealEngine5_Backup.rar',
      date: '15 يونيو 2026',
      selected: false,
      icon: 'archive'
    },
    {
      id: 'lf_5',
      name: 'Company_Annual_Financial_Report_2025_PrintReady.pdf',
      type: 'document',
      sizeMB: 320,
      path: '/Documents/Work/Financial_Report_2025.pdf',
      date: '10 مارس 2026',
      selected: false,
      icon: 'file-text'
    },
    {
      id: 'lf_6',
      name: 'Genshin_Impact_Installer_v4.8.apk',
      type: 'apk',
      sizeMB: 1850,
      path: '/Download/Genshin_Installer.apk',
      date: '22 أغسطس 2026',
      selected: true,
      icon: 'package'
    }
  ],

  // Running Background Processes
  runningTasks: [
    { id: 'task_1', name: 'Social Media Sync & Push Service', ramMB: 680, cpuPercent: 14, icon: 'share-2', app: 'Instagram / Meta' },
    { id: 'task_2', name: 'Video Streaming Buffer Cache', ramMB: 940, cpuPercent: 22, icon: 'play-circle', app: 'TikTok Background' },
    { id: 'task_3', name: 'Cloud Photos Auto-Uploader & Indexer', ramMB: 520, cpuPercent: 11, icon: 'cloud', app: 'Google Photos' },
    { id: 'task_4', name: 'Game Engine Analytics Daemon', ramMB: 760, cpuPercent: 18, icon: 'gamepad-2', app: 'PUBG Services' },
    { id: 'task_5', name: 'Browser Tab Zombie Processes (14 tabs)', ramMB: 890, cpuPercent: 16, icon: 'globe', app: 'Chrome WebView' },
    { id: 'task_6', name: 'E-Commerce Advertising Tracker', ramMB: 310, cpuPercent: 6, icon: 'shopping-bag', app: 'Shopping App Sync' }
  ],

  // WhatsApp & Social Media Data
  socialMediaStats: {
    whatsapp: {
      totalMB: 9450,
      voiceNotesMB: 2450,
      voiceCount: 3420,
      sentVideosMB: 4800,
      videoCount: 185,
      statusesMB: 1400,
      statusCount: 890,
      stickersMB: 800,
      stickerCount: 4200
    },
    telegram: {
      totalMB: 4850,
      cacheMB: 3200,
      downloadsMB: 1650
    },
    tiktok: {
      totalMB: 3600,
      cacheMB: 2800,
      draftsMB: 800
    }
  },

  // Recycle bin items
  recycleBin: [
    {
      id: 'rb_1',
      name: 'Old_Meeting_Recording_20260714.m4a',
      size: '142 MB',
      deletedAt: '28 سبتمبر 2026',
      daysLeft: 28,
      category: 'Audio'
    },
    {
      id: 'rb_2',
      name: 'Invoice_Archive_2025_Q4.zip',
      size: '380 MB',
      deletedAt: '25 سبتمبر 2026',
      daysLeft: 25,
      category: 'Archive'
    }
  ],

  // Whitelist items
  whitelist: [
    { id: 'wl_1', pattern: '*.psd', type: 'ext', descAr: 'ملفات فوتوشوب', descEn: 'Photoshop Design Files' },
    { id: 'wl_2', pattern: '/DCIM/Favorites/*', type: 'folder', descAr: 'مجلد الصور المفضلة', descEn: 'Favorites Camera Folder' },
    { id: 'wl_3', pattern: '*.kdbx', type: 'ext', descAr: 'قواعد بيانات كلمات المرور المشفرة', descEn: 'KeePass Safe Passwords' },
    { id: 'wl_4', pattern: '/WhatsApp/Media/WhatsApp Documents/Work/*', type: 'folder', descAr: 'مستندات العمل الهامة', descEn: 'Work Documents' }
  ],

  // Kotlin Code Snippets Catalog for Android Code Studio Tab
  codeCatalog: [
    {
      id: 'empty_scanner',
      titleAr: '1. خوارزمية فحص المجلدات الفارغة (EmptyFolderScanner.kt)',
      titleEn: '1. Empty Folders Cleaner Algorithm',
      lang: 'kotlin',
      code: `import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmptyFolderScanner @Inject constructor() {

    // المسارات المحمية التي يُمنع حذفها أو اعتبارها فارغة
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
        if (files == null || files.isEmpty()) {
            result.add(dir)
            return true
        }

        var allChildrenAreEmpty = true
        for (file in files) {
            if (file.isDirectory) {
                val isChildEmpty = scanRecursive(file, result)
                if (!isChildEmpty) allChildrenAreEmpty = false
            } else {
                allChildrenAreEmpty = false
            }
        }

        if (allChildrenAreEmpty) {
            result.add(dir)
        }
        return allChildrenAreEmpty
    }

    private fun isBlacklisted(dir: File): Boolean {
        val path = dir.absolutePath
        return blacklistedPaths.any { path.contains(it) } || dir.name.startsWith(".")
    }
}`
    },
    {
      id: 'dup_scanner',
      titleAr: '2. خوارزمية تطابق الهاش SHA-256 (DuplicateFileScanner.kt)',
      titleEn: '2. Exact Duplicate SHA-256 Engine',
      lang: 'kotlin',
      code: `import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DuplicateFileScanner @Inject constructor() {

    fun findDuplicateFiles(files: List<File>): Map<String, List<File>> {
        // المرحلة 1: تصفية وتجميع الملفات حسب الحجم (تجاهل الأحجام الفريدة والملفات الصفرية)
        val filesBySize = files.asSequence()
            .filter { it.isFile && it.length() > 0 }
            .groupBy { it.length() }
            .filter { it.value.size > 1 }

        // المرحلة 2: احتساب هاش SHA-256 للمجموعات المتساوية في الحجم فقط
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
}`
    },
    {
      id: 'dhash_scanner',
      titleAr: '3. خوارزمية البصمة الرقمية للصور dHash (SimilarImageScanner.kt)',
      titleEn: '3. Perceptual Difference Hash (dHash)',
      lang: 'kotlin',
      code: `import android.graphics.Bitmap
import android.graphics.BitmapFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SimilarImageScanner @Inject constructor() {

    // تحويل الصورة إلى بصمة رقمية 64-bit
    fun getdHash(filePath: String): Long {
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4 // تقليل الدقة لتسريع التحميل وتوفير الذاكرة
        }
        val originalBitmap = BitmapFactory.decodeFile(filePath, options) ?: return 0L

        // تصغير الصورة إلى 9x8 بكسل (أبيض وأسود)
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, 9, 8, true)
        var hash = 0L

        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val leftPixel = getGrayscale(scaledBitmap.getPixel(col, row))
                val rightPixel = getGrayscale(scaledBitmap.getPixel(col + 1, row))

                if (leftPixel > rightPixel) {
                    hash = hash or (1L shl (row * 8 + col))
                }
            }
        }
        return hash
    }

    // حساب مسافة هامينغ (Hamming Distance): مسافة <= 5 تعني تطابق بصري شبه كامل
    fun calculateSimilarityDistance(hash1: Long, hash2: Long): Int {
        return java.lang.Long.bitCount(hash1 xor hash2)
    }

    private fun getGrayscale(color: Int): Int {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        return (0.3 * r + 0.59 * g + 0.11 * b).toInt()
    }
}`
    },
    {
      id: 'permission_manager',
      titleAr: '4. مدير الصلاحيات لأندرويد 11-15 (StoragePermissionManager.kt)',
      titleEn: '4. Storage Permission Manager (API 26-35)',
      lang: 'kotlin',
      code: `import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object StoragePermissionManager {

    fun hasStoragePermission(context: Context): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    fun requestStoragePermission(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:\${activity.packageName}")
                }
                activity.startActivityForResult(intent, requestCode)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                activity.startActivityForResult(fallbackIntent, requestCode)
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                requestCode
            )
        }
    }
}`
    },
    {
      id: 'foreground_service',
      titleAr: '5. خدمة الفحص في الخلفية مع الإشعارات (StorageScanForegroundService.kt)',
      titleEn: '5. Background Scan Foreground Service',
      lang: 'kotlin',
      code: `import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject

@AndroidEntryPoint
class StorageScanForegroundService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "storage_scan_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START_SCAN = "ACTION_START_SCAN"

        private val _scanProgressFlow = MutableSharedFlow<ScanProgressEvent>(replay = 1)
        val scanProgressFlow: SharedFlow<ScanProgressEvent> = _scanProgressFlow
    }

    data class ScanProgressEvent(val progressPercent: Int, val stageMessage: String, val isCompleted: Boolean = false)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_SCAN) {
            startForeground(NOTIFICATION_ID, buildProgressNotification(0, "بدء الفحص الشامل..."))
            executeFullStorageScan()
        }
        return START_NOT_STICKY
    }

    private fun executeFullStorageScan() {
        serviceScope.launch {
            emitProgress(20, "جارٍ فحص المجلدات الفارغة...")
            delay(800)
            emitProgress(55, "جارٍ تصفية الملفات المكررة...")
            delay(1000)
            emitProgress(85, "فحص الصور بالذكاء الاصطناعي (dHash)...")
            delay(900)
            emitProgress(100, "اكتمل الفحص بنجاح!", isCompleted = true)
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    private suspend fun emitProgress(progress: Int, message: String, isCompleted: Boolean = false) {
        notificationManager.notify(NOTIFICATION_ID, buildProgressNotification(progress, message))
        _scanProgressFlow.emit(ScanProgressEvent(progress, message, isCompleted))
    }

    private fun buildProgressNotification(progress: Int, message: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("منظف التخزين الذكي")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}`
    },
    {
      id: 'dashboard_compose',
      titleAr: '6. واجهة الشاشة الرئيسية بـ Jetpack Compose (DashboardScreen.kt)',
      titleEn: '6. Compose Dashboard Screen & 4 Cards',
      lang: 'kotlin',
      code: `@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onNavigateToEmptyFolders: () -> Unit,
    onNavigateToDuplicatePhotos: () -> Unit,
    onNavigateToDuplicateVideos: () -> Unit,
    onNavigateToDuplicateFiles: () -> Unit,
    onQuickScanClicked: () -> Unit
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onQuickScanClicked,
                containerColor = Color(0xFF0EA5E9),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Bolt, contentDescription = null) },
                text = { Text("فحص وتنظيف شامل", fontWeight = FontWeight.Bold) }
            )
        },
        containerColor = Color(0xFF0B0F19)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // مؤشر التخزين الدائري
            StorageOverviewCard(
                usedGB = (uiState.storageStats.usedBytes / (1024.0 * 1024 * 1024)).toFloat(),
                totalGB = (uiState.storageStats.totalBytes / (1024.0 * 1024 * 1024)).toFloat(),
                usedPercentage = uiState.storageStats.usedPercentage,
                healthScore = uiState.storageStats.healthScore
            )

            // شبكة البطاقات الـ 4 الرئيسية
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    CleanerActionCard(
                        title = "المجلدات الفارغة",
                        subtitle = "\${uiState.emptyFoldersCount} مجلد فارغ",
                        icon = Icons.Default.FolderOff,
                        accentColor = Color(0xFFF59E0B),
                        onClick = onNavigateToEmptyFolders
                    )
                }
                item {
                    CleanerActionCard(
                        title = "مكررات الصور (dHash)",
                        subtitle = "\${uiState.duplicatePhotosCount} صورة (\${uiState.duplicatePhotosSizeMB} MB)",
                        icon = Icons.Default.PhotoLibrary,
                        accentColor = Color(0xFFA855F7),
                        onClick = onNavigateToDuplicatePhotos
                    )
                }
                item {
                    CleanerActionCard(
                        title = "الفيديوهات المكررة",
                        subtitle = "\${uiState.duplicateVideosCount} مقاطع",
                        icon = Icons.Default.VideoLibrary,
                        accentColor = Color(0xFF06B6D4),
                        onClick = onNavigateToDuplicateVideos
                    )
                }
                item {
                    CleanerActionCard(
                        title = "الملفات المتطابقة (SHA-256)",
                        subtitle = "\${uiState.duplicateFilesCount} ملفات",
                        icon = Icons.Default.Difference,
                        accentColor = Color(0xFF10B981),
                        onClick = onNavigateToDuplicateFiles
                    )
                }
            }
        }
    }
}`
    }
  ]
};

// Global Store singleton
window.cleanerStore = {
  state: JSON.parse(JSON.stringify(initialStorageState)),
  listeners: [],

  subscribe(listener) {
    this.listeners.push(listener);
    return () => {
      this.listeners = this.listeners.filter(l => l !== listener);
    };
  },

  notify() {
    this.listeners.forEach(fn => fn(this.state));
  },

  // Perform quick clean
  performQuickClean(selectedCategoryIds) {
    let freedMB = 0;
    this.state.quickScanCategories.forEach(cat => {
      if (selectedCategoryIds.includes(cat.id)) {
        freedMB += cat.sizeMB;
        cat.cleaned = true;
        cat.sizeMB = 0;
      }
    });

    if (selectedCategoryIds.includes('emptyDirs')) {
      this.state.emptyFolders = [];
    }

    const freedGB = (freedMB / 1024);
    this.state.usedCapacityGB = Math.max(20.0, +(this.state.usedCapacityGB - freedGB).toFixed(1));
    this.state.freeCapacityGB = +(this.state.totalCapacityGB - this.state.usedCapacityGB).toFixed(1);
    this.state.healthScore = Math.min(98, this.state.healthScore + 28);
    this.state.batteryTempC = Math.max(31.0, +(this.state.batteryTempC - 3.2).toFixed(1));
    this.notify();
    return { freedMB, freedGB };
  },

  // Delete empty folders
  deleteSelectedEmptyFolders() {
    const deletedCount = this.state.emptyFolders.filter(f => f.selected).length;
    this.state.emptyFolders = this.state.emptyFolders.filter(f => !f.selected);
    const cat = this.state.quickScanCategories.find(c => c.id === 'emptyDirs');
    if (cat && this.state.emptyFolders.length === 0) {
      cat.cleaned = true;
      cat.sizeMB = 0;
    }
    this.notify();
    return deletedCount;
  },

  // Boost RAM
  performRamBoost() {
    const prevRam = this.state.ramUsedGB;
    const freedRam = +(prevRam * 0.42).toFixed(1);
    this.state.ramUsedGB = Math.max(2.4, +(prevRam - freedRam).toFixed(1));
    this.state.runningTasks = [];
    this.state.batteryTempC = Math.max(30.0, +(this.state.batteryTempC - 2.5).toFixed(1));
    this.notify();
    return freedRam;
  },

  // Delete duplicates
  deleteSelectedDuplicates() {
    let freedMB = 0;
    this.state.duplicateGroups.forEach(grp => {
      const remaining = grp.photos.filter(p => {
        if (p.selected) {
          freedMB += parseFloat(p.size);
          return false;
        }
        return true;
      });
      grp.photos = remaining;
    });
    this.state.duplicateGroups = this.state.duplicateGroups.filter(g => g.photos.length > 1);
    const freedGB = +(freedMB / 1024).toFixed(2);
    this.state.usedCapacityGB = Math.max(20.0, +(this.state.usedCapacityGB - freedGB).toFixed(1));
    this.state.freeCapacityGB = +(this.state.totalCapacityGB - this.state.usedCapacityGB).toFixed(1);
    this.notify();
    return freedMB;
  },

  // Delete Large Files
  deleteSelectedLargeFiles(fileIds) {
    let freedMB = 0;
    const deletedFiles = [];
    this.state.largeFiles = this.state.largeFiles.filter(f => {
      if (fileIds.includes(f.id)) {
        freedMB += f.sizeMB;
        deletedFiles.push(f);
        return false;
      }
      return true;
    });

    deletedFiles.forEach(f => {
      this.state.recycleBin.unshift({
        id: 'rb_' + Date.now() + '_' + Math.random().toString(36).substr(2, 4),
        name: f.name,
        size: f.sizeMB > 1024 ? (f.sizeMB/1024).toFixed(1) + ' GB' : f.sizeMB + ' MB',
        deletedAt: 'اليوم ' + new Date().toLocaleTimeString('ar-EG', {hour:'2-digit', minute:'2-digit'}),
        daysLeft: 30,
        category: f.type
      });
    });

    const freedGB = +(freedMB / 1024).toFixed(2);
    this.state.usedCapacityGB = Math.max(20.0, +(this.state.usedCapacityGB - freedGB).toFixed(1));
    this.state.freeCapacityGB = +(this.state.totalCapacityGB - this.state.usedCapacityGB).toFixed(1);
    this.notify();
    return freedMB;
  },

  // Clean WhatsApp Voice Notes
  cleanWhatsAppVoiceNotes() {
    const saved = this.state.socialMediaStats.whatsapp.voiceNotesMB;
    this.state.socialMediaStats.whatsapp.voiceNotesMB = 120;
    this.state.socialMediaStats.whatsapp.voiceCount = 80;
    const freedGB = +(saved / 1024).toFixed(2);
    this.state.usedCapacityGB = Math.max(20.0, +(this.state.usedCapacityGB - freedGB).toFixed(1));
    this.state.freeCapacityGB = +(this.state.totalCapacityGB - this.state.usedCapacityGB).toFixed(1);
    this.notify();
    return saved;
  },

  restoreRecycleItem(id) {
    this.state.recycleBin = this.state.recycleBin.filter(item => item.id !== id);
    this.notify();
  },

  emptyRecycleBin() {
    this.state.recycleBin = [];
    this.notify();
  }
};
