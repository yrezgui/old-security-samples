package com.samples.appinstaller

import android.app.NotificationChannel
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationManager(private val context: Context) {
    private val notificationManager: NotificationManagerCompat by lazy {
        NotificationManagerCompat.from(context)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun createChannel(id: String, name: String, description: String) {
        val channel = NotificationChannel(id, name, IMPORTANCE_HIGH).apply {
            this.description = description
        }

        notificationManager.createNotificationChannel(channel)
    }

    fun notify(
        id: Int,
        title: String,
        description: String,
        channel: String,
        contentIntent: PendingIntent?
    ) {
        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)

        notificationManager.notify(id, builder.build())
    }
}