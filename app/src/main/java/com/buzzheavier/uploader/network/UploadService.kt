package com.buzzheavier.uploader.network

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.buzzheavier.uploader.MainActivity
import com.buzzheavier.uploader.R
import com.buzzheavier.uploader.UploadConstants
import com.buzzheavier.uploader.data.UploadProgress
import com.buzzheavier.uploader.data.UploadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class UploadService : LifecycleService() {

    companion object {
        val currentProgress = MutableStateFlow(UploadProgress())
        val currentStatus = MutableStateFlow(UploadStatus.IDLE)
        val currentFileName = MutableStateFlow("")
        val uploadResults = MutableStateFlow<List<String>>(emptyList())
    }

    private var uploadManager: UploadManager? = null

    override fun onCreate() {
        super.onCreate()
        uploadManager = UploadManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            "ACTION_UPLOAD" -> {
                val uri = intent.getParcelableExtra<Uri>("uri") ?: return START_NOT_STICKY
                val accountId = intent.getStringExtra("accountId") ?: ""
                val parentId = intent.getStringExtra("parentId") ?: ""
                val locationId = intent.getStringExtra("locationId") ?: ""
                val note = intent.getStringExtra("note") ?: ""

                startForegroundNotification()
                lifecycleScope.launch {
                    uploadManager?.uploadFile(
                        uri = uri,
                        accountId = accountId,
                        parentId = parentId,
                        locationId = locationId,
                        note = note,
                        onProgress = { progress ->
                            currentProgress.value = progress
                            updateNotification(progress)
                        }
                    )
                    stopSelf()
                }
            }
            "ACTION_CANCEL" -> {
                uploadManager?.cancelUpload()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startForegroundNotification() {
        val notification = buildNotification(UploadProgress())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(UploadConstants.NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(UploadConstants.NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(progress: UploadProgress): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = Intent(this, UploadService::class.java).apply {
            action = "ACTION_CANCEL"
        }
        val cancelPendingIntent = PendingIntent.getService(
            this, 1, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, UploadConstants.CHANNEL_ID)
            .setContentTitle(getString(R.string.uploading))
            .setContentText(currentFileName.value)
            .setSmallIcon(android.R.drawable.ic_menu_upload)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.cancel), cancelPendingIntent)
            .setProgress(100, progress.percentage, progress.percentage == 0)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(progress: UploadProgress) {
        val notification = buildNotification(progress)
        NotificationManagerCompat.from(this)
            .notify(UploadConstants.NOTIFICATION_ID, notification)
    }
}
