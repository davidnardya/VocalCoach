package org.davidnardya.vocalcoach.domain.repository

interface NotificationScheduler {
    fun scheduleReminder(intervalHours: Int, targetHour: Int, targetMinute: Int)
    fun triggerImmediateReminder()
}
