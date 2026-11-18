package id.antifakebts.detector.service

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import id.antifakebts.detector.FakeBtsApplication
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.repository.SettingsRepository
import java.util.regex.Pattern
import org.koin.android.ext.android.inject

class SmsNotificationListener : NotificationListenerService() {

    private val settingsRepository: SettingsRepository by inject()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        if (!settingsRepository.isSmsPhishingScanEnabled.value) return

        val pkg = sbn.packageName.lowercase()
        val isSmsApp = pkg.contains("messaging") || pkg.contains("mms") || pkg.contains("telephony")
        if (!isSmsApp) return

        val currentRisk = CellMonitoringService.activeRiskLevel.value
        if (currentRisk == AnomalyLevel.SAFE) return

        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val fullContent = "$title $text"

        if (URL_PATTERN.matcher(fullContent).find()) {
            postPhishingWarningNotification(title)
        }
    }

    private fun postPhishingWarningNotification(sender: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val alertNotification = NotificationCompat.Builder(this, FakeBtsApplication.CHANNEL_ALERTS_ID)
            .setContentTitle("Peringatan Keamanan SMS")
            .setContentText("SMS dari $sender mengandung tautan web saat terdeteksi anomali seluler. Hindari membuka tautan atau memasukkan OTP.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ALERT_SMS_ID, alertNotification)
    }

    companion object {
        private const val NOTIFICATION_ALERT_SMS_ID = 2002
        private val URL_PATTERN = Pattern.compile(
            "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE
        )
    }
}
