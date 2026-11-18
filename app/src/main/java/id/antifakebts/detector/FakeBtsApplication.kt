package id.antifakebts.detector

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

class FakeBtsApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val statusChannel = NotificationChannel(
                CHANNEL_STATUS_ID,
                getString(R.string.notification_channel_status),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_status_desc)
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                getString(R.string.notification_channel_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_alerts_desc)
                enableVibration(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannels(listOf(statusChannel, alertsChannel))
        }
    }

    companion object {
        const val CHANNEL_STATUS_ID = "fakebts_monitoring_status"
        const val CHANNEL_ALERTS_ID = "fakebts_anomaly_alerts"
    }
}
