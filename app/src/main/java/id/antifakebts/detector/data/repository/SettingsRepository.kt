package id.antifakebts.detector.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isAutoMitigationEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_MITIGATION, false)
    )
    val isAutoMitigationEnabled: StateFlow<Boolean> = _isAutoMitigationEnabled.asStateFlow()

    private val _isSmsPhishingScanEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_SMS_SCAN, false)
    )
    val isSmsPhishingScanEnabled: StateFlow<Boolean> = _isSmsPhishingScanEnabled.asStateFlow()

    fun setAutoMitigationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_MITIGATION, enabled).apply()
        _isAutoMitigationEnabled.value = enabled
    }

    fun setSmsPhishingScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SMS_SCAN, enabled).apply()
        _isSmsPhishingScanEnabled.value = enabled
    }

    fun getAppInstallTime(): Long {
        var installTime = prefs.getLong(KEY_INSTALL_TIME, 0L)
        if (installTime == 0L) {
            installTime = System.currentTimeMillis()
            prefs.edit().putLong(KEY_INSTALL_TIME, installTime).apply()
        }
        return installTime
    }

    fun getActionCountInCurrentHour(): Int {
        val lastTimestamp = prefs.getLong(KEY_LAST_ACTION_HOUR_TIMESTAMP, 0L)
        val now = System.currentTimeMillis()
        return if (now - lastTimestamp < 3600000L) {
            prefs.getInt(KEY_ACTION_COUNT_HOUR, 0)
        } else {
            0
        }
    }

    fun incrementActionCount() {
        val currentCount = getActionCountInCurrentHour()
        prefs.edit()
            .putLong(KEY_LAST_ACTION_HOUR_TIMESTAMP, System.currentTimeMillis())
            .putInt(KEY_ACTION_COUNT_HOUR, currentCount + 1)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "fakebts_detector_prefs"
        private const val KEY_AUTO_MITIGATION = "pref_auto_mitigation"
        private const val KEY_SMS_SCAN = "pref_sms_scan"
        private const val KEY_INSTALL_TIME = "pref_install_time"
        private const val KEY_LAST_ACTION_HOUR_TIMESTAMP = "pref_last_action_hour"
        private const val KEY_ACTION_COUNT_HOUR = "pref_action_count_hour"
    }
}
