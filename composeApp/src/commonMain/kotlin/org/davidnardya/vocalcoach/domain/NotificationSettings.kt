package org.davidnardya.vocalcoach.domain

import kotlinx.serialization.Serializable

@Serializable
data class NotificationSettings(
    val enabled: Boolean = true,
    val hour: Int = 17,
    val minute: Int = 0,
    val intervalHours: Long = 24
)
