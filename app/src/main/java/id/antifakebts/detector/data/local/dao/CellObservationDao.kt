package id.antifakebts.detector.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.antifakebts.detector.data.local.entity.CellObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CellObservationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CellObservationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<CellObservationEntity>)

    @Query("SELECT * FROM cell_observations ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentObservations(limit: Int): Flow<List<CellObservationEntity>>

    @Query("SELECT * FROM cell_observations WHERE is_registered = 1 ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentServingObservations(limit: Int): Flow<List<CellObservationEntity>>

    @Query("SELECT COUNT(*) FROM cell_observations")
    fun getTotalObservationCount(): Flow<Int>

    @Query("DELETE FROM cell_observations WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long): Int
}
