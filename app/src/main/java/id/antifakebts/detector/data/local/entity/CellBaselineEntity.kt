package id.antifakebts.detector.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.antifakebts.detector.data.model.NetworkType

@Entity(
    tableName = "cell_baseline",
    indices = [
        Index(value = ["cell_id"]),
        Index(value = ["lac_tac"]),
        Index(value = ["plmn"]),
        Index(value = ["is_user_whitelisted"])
    ]
)
data class CellBaselineEntity(
    @PrimaryKey
    @ColumnInfo(name = "grid_key")
    val gridKey: String,
    @ColumnInfo(name = "cell_id")
    val cellId: Long,
    @ColumnInfo(name = "lac_tac")
    val lacTac: Int,
    @ColumnInfo(name = "plmn")
    val plmn: String,
    @ColumnInfo(name = "network_type")
    val networkType: NetworkType,
    @ColumnInfo(name = "first_seen_timestamp")
    val firstSeenTimestamp: Long,
    @ColumnInfo(name = "last_seen_timestamp")
    val lastSeenTimestamp: Long,
    @ColumnInfo(name = "observation_count")
    val observationCount: Int,
    @ColumnInfo(name = "avg_signal_dbm")
    val avgSignalDbm: Double,
    @ColumnInfo(name = "min_signal_dbm")
    val minSignalDbm: Int,
    @ColumnInfo(name = "max_signal_dbm")
    val maxSignalDbm: Int,
    @ColumnInfo(name = "is_user_whitelisted")
    val isUserWhitelisted: Boolean = false
)
