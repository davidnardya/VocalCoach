package org.davidnardya.vocalcoach.data

import android.content.Context
import android.util.Log
import androidx.lifecycle.Observer
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    override fun scheduleReminder(intervalHours: Int, targetHour: Int, targetMinute: Int) {
        val workManager = WorkManager.getInstance(context)
        saveScheduleToPrefs(intervalHours, targetHour, targetMinute)
        if (intervalHours <= 0) {
            workManager.cancelUniqueWork(REMINDER_WORK_NAME)
            return
        }

        val calculatedDelayMillis = calculateDelay(targetHour, targetMinute)
        val safeInterval = intervalHours.toLong()
        val workRequest = PeriodicWorkRequestBuilder<VocalReminderWorker>(
            repeatInterval = safeInterval,
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setConstraints(Constraints.NONE)
            .setInitialDelay(calculatedDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        Log.d(
            TAG,
            "Scheduling periodic reminder in ${calculatedDelayMillis / 60000}m ${(calculatedDelayMillis / 1000) % 60}s"
        )

        observeWorkState(workManager, workRequest.id)

        workManager.enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    override fun triggerImmediateReminder() {
        val workManager = WorkManager.getInstance(context)
        val testDelayMillis = TimeUnit.MINUTES.toMillis(1)
        val workRequest = OneTimeWorkRequestBuilder<VocalReminderWorker>()
            .setConstraints(Constraints.NONE)
            .setInitialDelay(testDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        Log.d(
            TAG,
            "Scheduling one-time test reminder in ${testDelayMillis / 60000}m ${(testDelayMillis / 1000) % 60}s"
        )
        observeWorkState(workManager, workRequest.id)
        workManager.enqueueUniqueWork(
            TEST_REMINDER_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun ensureReminderScheduledOnStartup() {
        val schedule = readScheduleFromPrefs()
        val intervalHours = schedule.intervalHours
        if (intervalHours <= 0) return
        val calculatedDelayMillis = calculateDelay(schedule.targetHour, schedule.targetMinute)
        val workManager = WorkManager.getInstance(context)
        val workRequest = PeriodicWorkRequestBuilder<VocalReminderWorker>(
            repeatInterval = intervalHours.toLong(),
            repeatIntervalTimeUnit = TimeUnit.HOURS
        ).setConstraints(Constraints.NONE)
            .setInitialDelay(calculatedDelayMillis, TimeUnit.MILLISECONDS)
            .build()
        Log.d(
            TAG,
            "Startup KEEP scheduling in ${calculatedDelayMillis / 60000}m ${(calculatedDelayMillis / 1000) % 60}s"
        )
        observeWorkState(workManager, workRequest.id)
        workManager.enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    fun calculateDelay(targetHour: Int, targetMinute: Int): Long {
        val now = ZonedDateTime.now()
        var nextTarget = now
            .withHour(targetHour.coerceIn(0, 23))
            .withMinute(targetMinute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!nextTarget.isAfter(now)) {
            nextTarget = nextTarget.plusDays(1)
        }
        return Duration.between(now, nextTarget).toMillis().coerceAtLeast(0L)
    }

    private fun observeWorkState(workManager: WorkManager, workId: java.util.UUID) {
        val liveData = workManager.getWorkInfoByIdLiveData(workId)
        val observer = object : Observer<androidx.work.WorkInfo?> {
            override fun onChanged(value: androidx.work.WorkInfo?) {
                if (value == null) return
                Log.d(TAG, "Work ${value.id} state=${value.state}")
                if (value.state.isFinished) {
                    liveData.removeObserver(this)
                }
            }
        }
        liveData.observeForever(observer)
    }

    private fun saveScheduleToPrefs(intervalHours: Int, targetHour: Int, targetMinute: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_INTERVAL_HOURS, intervalHours)
            .putInt(KEY_TARGET_HOUR, targetHour)
            .putInt(KEY_TARGET_MINUTE, targetMinute)
            .apply()
    }

    private fun readScheduleFromPrefs(): ReminderSchedule {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ReminderSchedule(
            intervalHours = prefs.getInt(KEY_INTERVAL_HOURS, 24),
            targetHour = prefs.getInt(KEY_TARGET_HOUR, 17),
            targetMinute = prefs.getInt(KEY_TARGET_MINUTE, 0)
        )
    }

    private data class ReminderSchedule(
        val intervalHours: Int,
        val targetHour: Int,
        val targetMinute: Int
    )

    private companion object {
        const val REMINDER_WORK_NAME = "vocal_reminder_work"
        const val TEST_REMINDER_WORK_NAME = "vocal_test_reminder_work"
        const val TAG = "AndroidNotificationScheduler"
        const val PREFS_NAME = "vocal_notification_prefs"
        const val KEY_INTERVAL_HOURS = "interval_hours"
        const val KEY_TARGET_HOUR = "target_hour"
        const val KEY_TARGET_MINUTE = "target_minute"
    }
}
