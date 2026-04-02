package org.davidnardya.vocalcoach.data

import org.davidnardya.vocalcoach.domain.models.NotificationSettings
import org.davidnardya.vocalcoach.domain.repository.NotificationScheduler
import platform.UserNotifications.*
import platform.Foundation.*

class IosNotificationScheduler : NotificationScheduler {
    override fun reschedule(settings: NotificationSettings) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removeAllPendingNotificationRequests()

        if (!settings.enabled) return

        val content = UNMutableNotificationContent().apply {
            setTitle("Time to practice!")
            setBody("Don't forget to practice your growls today.")
            setSound(UNNotificationSound.defaultSound)
            setUserInfo(mapOf("url" to "growlgame://start"))
        }

        val dateComponents = NSDateComponents().apply {
            setHour(settings.hour.toLong())
            setMinute(settings.minute.toLong())
            setSecond(0)
        }

        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
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
}
