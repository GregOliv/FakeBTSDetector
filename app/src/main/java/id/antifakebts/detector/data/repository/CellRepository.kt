package id.antifakebts.detector.data.repository

import id.antifakebts.detector.data.local.dao.CellObservationDao
import id.antifakebts.detector.data.local.entity.CellObservationEntity
import id.antifakebts.detector.data.model.CellObservation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class CellRepository(
    private val cellObservationDao: CellObservationDao
) {
    private val _currentServingCell = MutableStateFlow<CellObservation?>(null)
    val currentServingCell: StateFlow<CellObservation?> = _currentServingCell.asStateFlow()

    private val _recentBatch = MutableStateFlow<List<CellObservation>>(emptyList())
    val recentBatch: StateFlow<List<CellObservation>> = _recentBatch.asStateFlow()

    fun updateCurrentBatch(observations: List<CellObservation>) {
        _recentBatch.value = observations
        val serving = observations.firstOrNull { it.isRegistered }
        if (serving != null) {
            _currentServingCell.value = serving
        }
    }

    suspend fun saveObservations(observations: List<CellObservation>) {
        if (observations.isEmpty()) return
        val entities = observations.map { CellObservationEntity.fromDomain(it) }
        cellObservationDao.insertAll(entities)
    }

    fun getRecentServingObservations(limit: Int = 50): Flow<List<CellObservation>> {
        return cellObservationDao.getRecentServingObservations(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getTotalObservationCount(): Flow<Int> = cellObservationDao.getTotalObservationCount()

    suspend fun pruneObservationsOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        cellObservationDao.deleteOlderThan(cutoff)
    }
}
