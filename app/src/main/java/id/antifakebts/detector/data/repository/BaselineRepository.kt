package id.antifakebts.detector.data.repository

import id.antifakebts.detector.data.local.dao.CellBaselineDao
import id.antifakebts.detector.data.local.entity.CellBaselineEntity
import id.antifakebts.detector.data.model.CellObservation
import kotlinx.coroutines.flow.Flow

class BaselineRepository(
    private val cellBaselineDao: CellBaselineDao
) {
    suspend fun recordObservation(obs: CellObservation) {
        val cellId = obs.cellId ?: return
        val lacTac = obs.lacTac ?: return
        val plmn = obs.plmn ?: return
        val signal = obs.signalDbm ?: -100

        val gridKey = "${obs.getCoarseGridKey()}_${plmn}_${cellId}"
        val existing = cellBaselineDao.getByGridKey(gridKey)

        if (existing == null) {
            val newEntry = CellBaselineEntity(
                gridKey = gridKey,
                cellId = cellId,
                lacTac = lacTac,
                plmn = plmn,
                networkType = obs.networkType,
                firstSeenTimestamp = obs.timestamp,
                lastSeenTimestamp = obs.timestamp,
                observationCount = 1,
                avgSignalDbm = signal.toDouble(),
                minSignalDbm = signal,
                maxSignalDbm = signal,
                isUserWhitelisted = false
            )
            cellBaselineDao.upsert(newEntry)
        } else {
            val updatedCount = existing.observationCount + 1
            val updatedAvg = ((existing.avgSignalDbm * existing.observationCount) + signal) / updatedCount
            val updatedEntry = existing.copy(
                lastSeenTimestamp = obs.timestamp,
                observationCount = updatedCount,
                avgSignalDbm = updatedAvg,
                minSignalDbm = minOf(existing.minSignalDbm, signal),
                maxSignalDbm = maxOf(existing.maxSignalDbm, signal),
                lacTac = lacTac
            )
            cellBaselineDao.upsert(updatedEntry)
        }
    }

    suspend fun isCellKnown(cellId: Long): Boolean {
        val entries = cellBaselineDao.getByCellId(cellId)
        return entries.isNotEmpty()
    }

    suspend fun isCellWhitelisted(cellId: Long): Boolean {
        val entries = cellBaselineDao.getByCellId(cellId)
        return entries.any { it.isUserWhitelisted }
    }

    suspend fun getBaselineForCellAndTac(cellId: Long, lacTac: Int): CellBaselineEntity? {
        return cellBaselineDao.getByCellAndTac(cellId, lacTac)
    }

    suspend fun getBaselineForCell(cellId: Long): List<CellBaselineEntity> {
        return cellBaselineDao.getByCellId(cellId)
    }

    fun getAllWhitelisted(): Flow<List<CellBaselineEntity>> = cellBaselineDao.getAllWhitelisted()

    suspend fun setWhitelisted(cellId: Long, whitelisted: Boolean) {
        cellBaselineDao.setWhitelisted(cellId, whitelisted)
    }

    fun getTotalBaselineCount(): Flow<Int> = cellBaselineDao.getTotalBaselineCount()
}
