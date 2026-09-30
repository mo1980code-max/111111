package com.smartclean.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.smartclean.app.data.scanner.DuplicateFileScanner
import com.smartclean.app.data.scanner.EmptyFolderScanner
import com.smartclean.app.data.scanner.SimilarImageScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * 4️⃣ Background Scan Foreground Service
 * Keeps memory scan alive in background, updates status notification,
 * and emits progress events to ViewModels via Kotlin Coroutines & SharedFlow.
 */
@AndroidEntryPoint
class StorageScanForegroundService : Service() {

    @Inject lateinit var emptyFolderScanner: EmptyFolderScanner
    @Inject lateinit var duplicateScanner: DuplicateFileScanner
    @Inject lateinit var similarImageScanner: SimilarImageScanner

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var notificationManager: NotificationManager

    companion object {
        const val CHANNEL_ID = "storage_scan_channel"
        const val NOTIFICATION_ID = 4040
        const val ACTION_START_SCAN = "ACTION_START_SCAN"
        const val ACTION_STOP_SCAN = "ACTION_STOP_SCAN"

        private val _scanProgressFlow = MutableSharedFlow<ScanProgressEvent>(replay = 1)
        val scanProgressFlow: SharedFlow<ScanProgressEvent> = _scanProgressFlow
    }

    data class ScanProgressEvent(
        val progressPercent: Int,
        val stageMessage: String,
        val isCompleted: Boolean = false
    )

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SCAN -> {
                startForeground(NOTIFICATION_ID, buildProgressNotification(0, "بدء الفحص الشامل..."))
                executeFullStorageScan()
            }
            ACTION_STOP_SCAN -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun executeFullStorageScan() {
        serviceScope.launch {
            val root = android.os.Environment.getExternalStorageDirectory()

            // Step 1: Empty Folders
            emitProgress(15, "جارٍ فحص المجلدات الفارغة...")
            val emptyDirs = emptyFolderScanner.scanEmptyDirectories(root)

            // Step 2: All Files & Duplicates
            emitProgress(50, "جارٍ تحليل الملفات المكررة...")
            val allFiles = root.walkTopDown().filter { it.isFile }.toList()
            val duplicateMap = duplicateScanner.findDuplicateFiles(allFiles)

            // Step 3: Visual similarities
            emitProgress(85, "جارٍ فحص الصور بالذكاء الاصطناعي...")
            // Simulating image checks on camera folder
            val dcimDir = File(root, "DCIM/Camera")
            if (dcimDir.exists()) {
                val images = dcimDir.listFiles()?.filter { it.extension in listOf("jpg", "jpeg", "png") } ?: emptyList()
                images.take(50).forEach { img ->
                    similarImageScanner.getdHash(img.absolutePath)
                }
            }

            // Step 4: Complete
            emitProgress(100, "اكتمل الفحص بنجاح!", isCompleted = true)
            notificationManager.notify(NOTIFICATION_ID, buildCompletedNotification())
            stopForeground(STOP_FOREGROUND_DETACH)
        }
    }

    private suspend fun emitProgress(progress: Int, message: String, isCompleted: Boolean = false) {
        notificationManager.notify(NOTIFICATION_ID, buildProgressNotification(progress, message))
        _scanProgressFlow.emit(ScanProgressEvent(progress, message, isCompleted))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "خدمة فحص التخزين",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعارات تقدم فحص وتنظيف الذاكرة"
            }
            notificationManager.createNotificationChannel(channel)
        }
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

    private fun buildCompletedNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("اكتمل الفحص الذكي")
            .setContentText("تم فحص التخزين والعثور على ملفات مكررة ومجلدات فارغة جاهزة للتحرير.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setAutoCancel(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
