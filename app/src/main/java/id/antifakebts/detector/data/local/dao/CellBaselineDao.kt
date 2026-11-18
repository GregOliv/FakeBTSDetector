package id.antifakebts.detector.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import id.antifakebts.detector.data.local.entity.CellBaselineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CellBaselineDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CellBaselineEntity)

    @Query("SELECT * FROM cell_baseline WHERE grid_key = :gridKey LIMIT 1")
    suspend fun getByGridKey(gridKey: String): CellBaselineEntity?

    @Query("SELECT * FROM cell_baseline WHERE cell_id = :cellId")
    suspend fun getByCellId(cellId: Long): List<CellBaselineEntity>

    @Query("SELECT * FROM cell_baseline WHERE cell_id = :cellId AND lac_tac = :lacTac LIMIT 1")
    suspend fun getByCellAndTac(cellId: Long, lacTac: Int): CellBaselineEntity?

    @Query("SELECT * FROM cell_baseline WHERE is_user_whitelisted = 1 ORDER BY last_seen_timestamp DESC")
    fun getAllWhitelisted(): Flow<List<CellBaselineEntity>>

    @Query("UPDATE cell_baseline SET is_user_whitelisted = :whitelisted WHERE cell_id = :cellId")
    suspend fun setWhitelisted(cellId: Long, whitelisted: Boolean)

    @Query("SELECT COUNT(*) FROM cell_baseline")
    fun getTotalBaselineCount(): Flow<Int>
}
