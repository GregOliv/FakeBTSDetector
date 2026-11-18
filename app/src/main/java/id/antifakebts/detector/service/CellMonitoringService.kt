package id.antifakebts.detector.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.CellInfo
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.app.NotificationCompat
import id.antifakebts.detector.FakeBtsApplication
import id.antifakebts.detector.R
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.repository.AnomalyRepository
import id.antifakebts.detector.data.repository.BaselineRepository
import id.antifakebts.detector.data.repository.CellRepository
import id.antifakebts.detector.device.DevicePolicyManagerAdapter
import id.antifakebts.detector.domain.engine.EvaluationResult
import id.antifakebts.detector.domain.engine.RuleEngine
import id.antifakebts.detector.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject

class CellMonitoringService : Service() {

    private val cellRepository: CellRepository by inject()
    private val baselineRepository: BaselineRepository by inject()
    private val anomalyRepository: AnomalyRepository by inject()
    private val cellInfoParser: CellInfoParser by inject()
    private val locationTracker: LocationTracker by inject()
    private val ruleEngine: RuleEngine by inject()
    private val devicePolicyAdapter: DevicePolicyManagerAdapter by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var telephonyManager: TelephonyManager? = null

    private var telephonyCallbackApi31: Any? = null
    @Suppress("DEPRECATION")
    private var phoneStateListenerApi29: PhoneStateListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        _isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createStatusNotification("Memulai pemantauan seluler...")
        startForeground(NOTIFICATION_ID, notification)

        registerCellListener()
        requestInitialCellInfo()

        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    private fun registerCellListener() {
        val tm = telephonyManager ?: return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val callback = object : TelephonyCallback(), TelephonyCallback.CellInfoListener {
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>) {
                        processCellInfoList(cellInfo)
                    }
                }
                tm.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallbackApi31 = callback
            } else {
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCellInfoChanged(cellInfo: MutableList<CellInfo>?) {
                        processCellInfoList(cellInfo)
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CELL_INFO)
                phoneStateListenerApi29 = listener
            }
        } catch (e: SecurityException) {
            updateStatusNotification("Izin lokasi/telepon belum diberikan")
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestInitialCellInfo() {
        val tm = telephonyManager ?: return
        try {
            val initialList = tm.allCellInfo
            processCellInfoList(initialList)
        } catch (e: SecurityException) {
            // Handled when permissions are granted
        }
    }

    private fun processCellInfoList(cellInfoList: List<CellInfo>?) {
        val tm = telephonyManager
        val operatorAlpha = tm?.networkOperatorName?.takeIf { it.isNotBlank() }
            ?: tm?.simOperatorName?.takeIf { it.isNotBlank() }
        val coarseLocation = locationTracker.getLastKnownCoarseLocation()
        val isRoaming = tm?.isNetworkRoaming ?: false

        val parsedObservations = cellInfoParser.parse(
            cellInfoList = cellInfoList,
            operatorAlpha = operatorAlpha,
            latitude = coarseLocation?.first,
            longitude = coarseLocation?.second
        )

        cellRepository.updateCurrentBatch(parsedObservations)

        val servingCell = parsedObservations.firstOrNull { it.isRegistered }
        if (servingCell != null) {
            val statusSummary = buildStatusString(servingCell)
            updateStatusNotification(statusSummary)

            serviceScope.launch {
                cellRepository.saveObservations(parsedObservations)
                baselineRepository.recordObservation(servingCell)

                val recentHistory = cellRepository.getRecentServingObservations(10).first()
                val evaluation = ruleEngine.evaluate(servingCell, recentHistory, isRoaming)

                _activeRiskLevel.value = evaluation.riskLevel
                _latestEvaluation.value = evaluation

                handleTieredResponse(servingCell, evaluation)
            }
        }
    }

    private suspend fun handleTieredResponse(cell: CellObservation, eval: EvaluationResult) {
        if (eval.riskLevel != AnomalyLevel.SAFE) {
            val entity = AnomalyLogEntity(
                timestamp = cell.timestamp,
                riskLevel = eval.riskLevel,
                confidence = eval.confidence,
                triggeredRulesJson = Json.encodeToString(eval.triggeredRules),
                weakeningFactorsJson = Json.encodeToString(eval.weakeningReasons),
                primaryReason = eval.primaryReasons.firstOrNull() ?: "Anomali sinyal seluler terdeteksi",
                cellId = cell.cellId,
                lacTac = cell.lacTac,
                plmn = cell.plmn,
                networkType = cell.networkType,
                signalDbm = cell.signalDbm,
                latitude = cell.latitude,
                longitude = cell.longitude,
                isResolved = false,
                userMarkedSafe = false
            )
            anomalyRepository.recordAnomaly(entity)
        }

        if (eval.shouldNotify) {
            when (eval.riskLevel) {
                AnomalyLevel.LOW -> {
                    // Logged silently, status notification updated
                }
                AnomalyLevel.MEDIUM -> {
                    postMediumAlertNotification(eval.primaryReasons.firstOrNull() ?: "Ada indikasi sel yang tidak biasa")
                }
                AnomalyLevel.HIGH -> {
                    postHighAlertNotification(eval.primaryReasons.firstOrNull() ?: "Terdeteksi anomali seluler berisiko tinggi")
                    devicePolicyAdapter.applyHighRiskMitigations("Anomali risiko tinggi terdeteksi: ${eval.primaryReasons.firstOrNull()}")
                }
                AnomalyLevel.SAFE -> Unit
            }
        }
    }

    private fun postMediumAlertNotification(reason: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val openAppIntent = PendingIntent.getActivity(
            this,
            2,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FakeBtsApplication.CHANNEL_ALERTS_ID)
            .setContentTitle("Peringatan: Indikasi Anomali Seluler")
            .setContentText(reason)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun postHighAlertNotification(reason: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val networkSettingsIntent = PendingIntent.getActivity(
            this,
            3,
            Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val openAppIntent = PendingIntent.getActivity(
            this,
            4,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, FakeBtsApplication.CHANNEL_ALERTS_ID)
            .setContentTitle("Perhatian: Potensi Anomali Seluler Berisiko")
            .setContentText("$reason. Buka pengaturan jaringan untuk memeriksa koneksi atau matikan 2G.")
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_menu_preferences, "Buka Pengaturan Jaringan", networkSettingsIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun buildStatusString(cell: CellObservation): String {
        val op = cell.operatorAlpha ?: cell.plmn ?: "Operator"
        val net = cell.networkType.displayName
        val dbm = cell.signalDbm?.let { "${it} dBm" } ?: ""
        val cid = cell.getDisplayCellId()
        return "$net | $op | CID: $cid $dbm"
    }

    private fun createStatusNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenApp = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, CellMonitoringService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FakeBtsApplication.CHANNEL_STATUS_ID)
            .setContentTitle("Pengawasan Seluler Aktif")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingOpenApp)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hentikan", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateStatusNotification(contentText: String) {
        val notification = createStatusNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        val tm = telephonyManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (telephonyCallbackApi31 as? TelephonyCallback)?.let {
                tm?.unregisterTelephonyCallback(it)
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListenerApi29?.let {
                tm?.listen(it, PhoneStateListener.LISTEN_NONE)
            }
        }
        serviceScope.cancel()
        _isServiceRunning.value = false
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ALERT_NOTIFICATION_ID = 1002
        const val ACTION_START_SERVICE = "id.antifakebts.detector.START_MONITORING"
        const val ACTION_STOP_SERVICE = "id.antifakebts.detector.STOP_MONITORING"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        val activeRiskLevel = MutableStateFlow(AnomalyLevel.SAFE)
        private val _activeRiskLevel = activeRiskLevel

        private val _latestEvaluation = MutableStateFlow<EvaluationResult?>(null)
        val latestEvaluation: StateFlow<EvaluationResult?> = _latestEvaluation.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, CellMonitoringService::class.java).apply {
                action = ACTION_START_SERVICE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CellMonitoringService::class.java).apply {
                action = ACTION_STOP_SERVICE
            }
            context.startService(intent)
        }
    }
}
