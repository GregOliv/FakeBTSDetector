package id.antifakebts.detector.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import id.antifakebts.detector.FakeBtsApplication
import id.antifakebts.detector.R

class CellMonitoringService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification: Notification = NotificationCompat.Builder(this, FakeBtsApplication.CHANNEL_STATUS_ID)
            .setContentTitle(getString(R.string.notification_channel_status))
            .setContentText(getString(R.string.notification_channel_status_desc))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
    }
}
