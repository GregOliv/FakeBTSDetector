package id.antifakebts.detector.data.repository

import id.antifakebts.detector.data.local.dao.AnomalyLogDao
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import kotlinx.coroutines.flow.Flow

class AnomalyRepository(
    private val anomalyLogDao: AnomalyLogDao
) {
    suspend fun recordAnomaly(anomaly: AnomalyLogEntity): Long {
        return anomalyLogDao.insert(anomaly)
    }

    fun getAllLogs(limit: Int = 100): Flow<List<AnomalyLogEntity>> {
        return anomalyLogDao.getAllLogs(limit)
    }

    suspend fun getRecentAnomalies(sinceTimestamp: Long): List<AnomalyLogEntity> {
        return anomalyLogDao.getRecentAnomalies(sinceTimestamp)
    }

    suspend fun markSafe(logId: Long) {
        anomalyLogDao.markSafe(logId)
    }

    suspend fun getAllLogsForExport(): List<AnomalyLogEntity> {
        return anomalyLogDao.getAllLogsForExport()
    }

    fun getTotalAlertCount(): Flow<Int> = anomalyLogDao.getTotalAlertCount()

    suspend fun pruneLogsOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        anomalyLogDao.deleteOlderThan(cutoff)
    }
}
