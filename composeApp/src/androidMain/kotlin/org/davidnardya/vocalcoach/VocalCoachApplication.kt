package org.davidnardya.vocalcoach

import android.app.Application
import androidx.work.WorkManager
import org.davidnardya.vocalcoach.data.AndroidNotificationScheduler
import org.davidnardya.vocalcoach.data.NotificationHelper
import org.davidnardya.vocalcoach.platform.AndroidPlatformContext

class VocalCoachApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatformContext.init(this)
        WorkManager.getInstance(this)
        NotificationHelper.ensureNotificationChannel(this)
        AndroidNotificationScheduler(this).ensureReminderScheduledOnStartup()
    }
}
