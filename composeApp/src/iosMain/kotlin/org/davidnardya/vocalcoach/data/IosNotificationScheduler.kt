package org.davidnardya.vocalcoach.data

import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

class IosNotificationScheduler : NotificationScheduler {
    override fun scheduleReminder(intervalHours: Int, targetHour: Int, targetMinute: Int) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removeAllPendingNotificationRequests()
        if (intervalHours <= 0) return

        val content = UNMutableNotificationContent().apply {
            setTitle("Time to practice!")
            setBody("Don't forget to practice your growls today.")
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf("url" to "growlgame://start"))
        }

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = (intervalHours.coerceAtLeast(1) * 3600).toDouble(),
            repeats = true
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "daily_reminder",
            content = content,
            trigger = trigger
        )

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling notification: ${error.localizedDescription}")
            }
        }
    }

    override fun triggerImmediateReminder() {
        // Temporary no-op for parity; debug button is intended for Android WorkManager verification.
    }
}
