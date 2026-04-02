package org.davidnardya.vocalcoach.domain

import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

class IosNotificationService : NSObject(), NotificationService, UNUserNotificationCenterDelegateProtocol {
    
    init {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.setDelegate(this)
        center.requestAuthorizationWithOptions(
            platform.UserNotifications.UNAuthorizationOptionAlert or 
            platform.UserNotifications.UNAuthorizationOptionSound
        ) { granted, error ->
            // todo - Handle error
        }
    }

    override fun sendGameReminder() {
        val content = UNMutableNotificationContent()
        content.setTitle("Time to Growl!")
        content.setBody("Your vocal level is waiting. Start your training now.")
        content.setUserInfo(mapOf("deepLink" to "growlgame://start"))
        
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)
        val request = UNNotificationRequest.requestWithIdentifier("game_reminder", content, trigger)
        
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
            //todo -  Handle error
        }
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val deepLink = userInfo["deepLink"] as? String
        if (deepLink != null) {
            // todo - In a real app, you'd emit this to a flow handled by the UI
        }
        withCompletionHandler()
    }
}
//todo - why no use?
actual fun provideNotificationService(): NotificationService = IosNotificationService()
