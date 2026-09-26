package com.zolarm.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.zolarm.app.MainActivity
import com.zolarm.app.R
import com.zolarm.app.ZolarmApp

class ZolarmForegroundService : Service() {
    companion object { const val NOTIF_ID = 7101 }

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, buildNotification())
        return START_STICKY
    }

    private fun buildNotification() = NotificationCompat.Builder(this, ZolarmApp.CHANNEL_ENGINE)
        .setContentTitle("زولارم نشط")
        .setContentText("محرك المنبّه يعمل — لا أعذار.")
        .setSmallIcon(R.drawable.ic_zolarm_logo)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .build()

    override fun onBind(intent: Intent?): IBinder? = null
}
