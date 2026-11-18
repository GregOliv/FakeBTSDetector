package id.antifakebts.detector.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SmsNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // Notification analysis logic will be bound to the risk engine in subsequent stages
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Notification removal tracking
    }
}
