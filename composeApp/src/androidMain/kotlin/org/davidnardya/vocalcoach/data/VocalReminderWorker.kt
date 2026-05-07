package org.davidnardya.vocalcoach.data

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class VocalReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val intervalHours = prefs.getInt(KEY_INTERVAL_HOURS, 24)
        Log.d(TAG, "Worker started at ${System.currentTimeMillis()} with intervalHours=$intervalHours")
        NotificationHelper.showReminder(applicationContext)
        return Result.success()
    }

    private companion object {
        const val TAG = "VocalWorker"
        const val PREFS_NAME = "vocal_notification_prefs"
        const val KEY_INTERVAL_HOURS = "interval_hours"
    }
}
