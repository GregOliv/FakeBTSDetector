package id.antifakebts.detector.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnomalyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: AnomalyLogEntity): Long

    @Query("SELECT * FROM anomaly_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getAllLogs(limit: Int): Flow<List<AnomalyLogEntity>>

    @Query("SELECT * FROM anomaly_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentAnomalies(sinceTimestamp: Long): List<AnomalyLogEntity>

    @Query("UPDATE anomaly_logs SET user_marked_safe = 1, is_resolved = 1 WHERE id = :logId")
    suspend fun markSafe(logId: Long)

    @Query("SELECT * FROM anomaly_logs ORDER BY timestamp ASC")
    suspend fun getAllLogsForExport(): List<AnomalyLogEntity>

    @Query("DELETE FROM anomaly_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM anomaly_logs WHERE risk_level != 'SAFE'")
    fun getTotalAlertCount(): Flow<Int>
}
