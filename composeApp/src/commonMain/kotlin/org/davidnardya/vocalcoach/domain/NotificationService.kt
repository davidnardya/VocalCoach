package org.davidnardya.vocalcoach.domain

interface NotificationService {
    fun sendGameReminder()
}
//todo - why error?
expect fun provideNotificationService(): NotificationService
