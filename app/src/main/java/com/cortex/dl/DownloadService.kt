package com.cortex.dl

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

private const val TAG = "DownloadService"
private const val SERVICE_CHANNEL_ID = "download_service"
private const val SERVICE_NOTIFICATION_ID = 123

class DownloadService : Service() {

    companion object {
        @SuppressLint("StaticFieldLeak")
        var instance: DownloadService? = null
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createServiceNotificationChannel()
    }

    private fun createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    SERVICE_CHANNEL_ID,
                    "Download Service",
                    NotificationManager.IMPORTANCE_MIN
                ).apply {
                    setSound(null, null)
                    enableVibration(false)
                    enableLights(false)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createServiceNotificationChannel()
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Cortex DL")
            .setContentText("Downloading")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()

        runCatching {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                startForeground(SERVICE_NOTIFICATION_ID, notification)
            }
        }.onFailure { th ->
            Log.e(TAG, "Failed to start foreground service", th)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return DownloadServiceBinder()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d(TAG, "onUnbind: ")
        return true
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy: ")
        instance = null
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                stopForeground(true)
            }
        }
        super.onDestroy()
    }

    inner class DownloadServiceBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }
}
