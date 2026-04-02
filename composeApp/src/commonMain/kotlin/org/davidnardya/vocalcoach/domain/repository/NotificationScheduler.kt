package org.davidnardya.vocalcoach.domain.repository

import org.davidnardya.vocalcoach.domain.models.NotificationSettings

interface NotificationScheduler {
    fun reschedule(settings: NotificationSettings)
}
