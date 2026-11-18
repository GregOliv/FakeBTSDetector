package id.antifakebts.detector.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import id.antifakebts.detector.data.local.converter.DatabaseConverters
import id.antifakebts.detector.data.local.dao.AnomalyLogDao
import id.antifakebts.detector.data.local.dao.CellBaselineDao
import id.antifakebts.detector.data.local.dao.CellObservationDao
import id.antifakebts.detector.data.local.entity.AnomalyLogEntity
import id.antifakebts.detector.data.local.entity.CellBaselineEntity
import id.antifakebts.detector.data.local.entity.CellObservationEntity

@Database(
    entities = [
        CellObservationEntity::class,
        CellBaselineEntity::class,
        AnomalyLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cellObservationDao(): CellObservationDao
    abstract fun cellBaselineDao(): CellBaselineDao
    abstract fun anomalyLogDao(): AnomalyLogDao

    companion object {
        const val DATABASE_NAME = "fakebts_detector.db"
    }
}
