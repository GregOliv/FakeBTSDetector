package id.antifakebts.detector.device

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.UserManager
import id.antifakebts.detector.data.repository.SettingsRepository
import id.antifakebts.detector.domain.engine.RuleEngineConfig
import id.antifakebts.detector.receiver.FakeBtsDeviceAdminReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MitigationStatus(
    val isDeviceOwner: Boolean,
    val isAutoMitigationActive: Boolean,
    val appliedRestrictions: Set<String>,
    val autoRevertRemainingSeconds: Long,
    val auditLogReason: String?
)

class DevicePolicyManagerAdapter(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val config: RuleEngineConfig = RuleEngineConfig()
) {
    private val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
    private val adminComponent = ComponentName(context, FakeBtsDeviceAdminReceiver::class.java)
    private val adapterScope = CoroutineScope(Dispatchers.Default)

    private var autoRevertJob: Job? = null
    private var absoluteTimeoutJob: Job? = null

    private val _status = MutableStateFlow(
        MitigationStatus(
            isDeviceOwner = checkIsDeviceOwner(),
            isAutoMitigationActive = false,
            appliedRestrictions = emptySet(),
            autoRevertRemainingSeconds = 0L,
            auditLogReason = null
        )
    )
    val status: StateFlow<MitigationStatus> = _status.asStateFlow()

    fun checkIsDeviceOwner(): Boolean {
        return dpm?.isDeviceOwnerApp(context.packageName) == true
    }

    fun applyHighRiskMitigations(reason: String): Boolean {
        if (!settingsRepository.isAutoMitigationEnabled.value) return false
        if (!checkIsDeviceOwner()) return false
        if (settingsRepository.getActionCountInCurrentHour() >= config.maxMitigationsPerHour) return false

        val active = mutableSetOf<String>()
        val devicePolicy = dpm ?: return false

        try {
            // Apply DISALLOW_CELLULAR_2G on Android 14+ (API 34)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                devicePolicy.addUserRestriction(adminComponent, UserManager.DISALLOW_CELLULAR_2G)
                active.add(UserManager.DISALLOW_CELLULAR_2G)
            }

            // Apply temporary DISALLOW_SMS
            devicePolicy.addUserRestriction(adminComponent, UserManager.DISALLOW_SMS)
            active.add(UserManager.DISALLOW_SMS)

            settingsRepository.incrementActionCount()

            _status.value = _status.value.copy(
                isDeviceOwner = true,
                isAutoMitigationActive = true,
                appliedRestrictions = active,
                autoRevertRemainingSeconds = config.autoRevertDurationMinutes * 60L,
                auditLogReason = reason
            )

            startRevertTimers()
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun revertAllMitigations(reason: String): Boolean {
        autoRevertJob?.cancel()
        absoluteTimeoutJob?.cancel()

        val devicePolicy = dpm ?: return false
        if (!checkIsDeviceOwner()) return false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                devicePolicy.clearUserRestriction(adminComponent, UserManager.DISALLOW_CELLULAR_2G)
            }
            devicePolicy.clearUserRestriction(adminComponent, UserManager.DISALLOW_SMS)

            _status.value = _status.value.copy(
                isAutoMitigationActive = false,
                appliedRestrictions = emptySet(),
                autoRevertRemainingSeconds = 0L,
                auditLogReason = "Mitigasi dicabut: $reason"
            )
            return true
        } catch (e: SecurityException) {
            return false
        }
    }

    fun manualOverride(): Boolean {
        return revertAllMitigations("Dicabut secara manual oleh pengguna melalui tombol override")
    }

    private fun startRevertTimers() {
        autoRevertJob?.cancel()
        absoluteTimeoutJob?.cancel()

        // Countdown timer for normal auto-revert
        autoRevertJob = adapterScope.launch {
            var seconds = config.autoRevertDurationMinutes * 60L
            while (seconds > 0) {
                delay(1000L)
                seconds--
                _status.value = _status.value.copy(autoRevertRemainingSeconds = seconds)
            }
            revertAllMitigations("Batas waktu normal tercapai (${config.autoRevertDurationMinutes} menit)")
        }

        // Absolute maximum timeout safeguard
        absoluteTimeoutJob = adapterScope.launch {
            delay(config.absoluteMitigationTimeoutMinutes * 60L * 1000L)
            revertAllMitigations("Batas waktu timeout mutlak tercapai (${config.absoluteMitigationTimeoutMinutes} menit)")
        }
    }
}
