package org.davidnardya.vocalcoach.domain

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

class AndroidNotificationService(
    private val context: Context
) : NotificationService {
    override fun sendGameReminder() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "game_reminder_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Game Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Time to Growl!")
            .setContentText("Your vocal level is waiting. Start your training now.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(1, notification)
    }
}

actual fun provideNotificationService(): NotificationService = 
    AndroidNotificationService(AndroidPlatformContext.application)
