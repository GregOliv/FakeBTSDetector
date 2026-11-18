package id.antifakebts.detector.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.antifakebts.detector.data.export.DataExporter
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.repository.AnomalyRepository
import id.antifakebts.detector.data.repository.BaselineRepository
import id.antifakebts.detector.data.repository.CellRepository
import id.antifakebts.detector.data.repository.SettingsRepository
import id.antifakebts.detector.device.DevicePolicyManagerAdapter
import id.antifakebts.detector.device.MitigationStatus
import id.antifakebts.detector.domain.engine.EvaluationResult
import id.antifakebts.detector.service.CellMonitoringService
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val cellRepository: CellRepository,
    private val baselineRepository: BaselineRepository,
    private val anomalyRepository: AnomalyRepository,
    private val settingsRepository: SettingsRepository,
    private val devicePolicyAdapter: DevicePolicyManagerAdapter,
    private val dataExporter: DataExporter
) : ViewModel() {

    val isServiceRunning: StateFlow<Boolean> = CellMonitoringService.isServiceRunning

    val activeRiskLevel: StateFlow<AnomalyLevel> = CellMonitoringService.activeRiskLevel

    val latestEvaluation: StateFlow<EvaluationResult?> = CellMonitoringService.latestEvaluation

    val currentServingCell: StateFlow<CellObservation?> = cellRepository.currentServingCell

    val neighboringCells: StateFlow<List<CellObservation>> = cellRepository.recentBatch
        .map { list -> list.filter { !it.isRegistered } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val anomalyLogs: StateFlow<List<AnomalyLogEntity>> = anomalyRepository.getAllLogs(100)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mitigationStatus: StateFlow<MitigationStatus> = devicePolicyAdapter.status

    val isAutoMitigationEnabled: StateFlow<Boolean> = settingsRepository.isAutoMitigationEnabled
    val isSmsScanEnabled: StateFlow<Boolean> = settingsRepository.isSmsPhishingScanEnabled

    val totalObservations: StateFlow<Int> = cellRepository.getTotalObservationCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalLearnedCells: StateFlow<Int> = baselineRepository.getTotalBaselineCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalAlerts: StateFlow<Int> = anomalyRepository.getTotalAlertCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startMonitoring(context: Context) {
        CellMonitoringService.start(context)
    }

    fun stopMonitoring(context: Context) {
        CellMonitoringService.stop(context)
    }

    fun setAutoMitigationEnabled(enabled: Boolean) {
        settingsRepository.setAutoMitigationEnabled(enabled)
    }

    fun setSmsScanEnabled(enabled: Boolean) {
        settingsRepository.setSmsPhishingScanEnabled(enabled)
    }

    fun manualOverride() {
        devicePolicyAdapter.manualOverride()
    }

    fun markLogSafe(log: AnomalyLogEntity) {
        viewModelScope.launch {
            anomalyRepository.markSafe(log.id)
            log.cellId?.let { cid ->
                baselineRepository.setWhitelisted(cid, true)
            }
        }
    }

    fun exportAndShareCsv() {
        viewModelScope.launch {
            val file = dataExporter.exportToCsv()
            dataExporter.shareExportedFile(file)
        }
    }

    fun exportAndShareJson() {
        viewModelScope.launch {
            val file = dataExporter.exportToJson()
            dataExporter.shareExportedFile(file)
        }
    }
}
