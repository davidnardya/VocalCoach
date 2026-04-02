package org.davidnardya.vocalcoach.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.davidnardya.vocalcoach.domain.models.NotificationSettings
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler

class AndroidNotificationScheduler(private val context: Context) : NotificationScheduler {
    override fun reschedule(settings: NotificationSettings) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.cancel(pendingIntent)

        if (!settings.enabled) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // We should ideally handle this in the UI/ViewModel as requested
                // For now, we schedule inexactly or return
                return
            }
        }

        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        var scheduledTime = LocalDateTime(
            year = now.year,
            month = now.month,
            dayOfMonth = now.dayOfMonth,
            hour = settings.hour,
            minute = settings.minute,
            second = 0,
            nanosecond = 0
        )

        if (scheduledTime <= now) {
            scheduledTime = scheduledTime.toInstant(TimeZone.currentSystemDefault())
                .plus(1, DateTimeUnit.DAY, TimeZone.currentSystemDefault())
                .toLocalDateTime(TimeZone.currentSystemDefault())
        }

        val triggerAtMillis = scheduledTime.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }
}
