package id.antifakebts.detector.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.ConfidenceLevel
import id.antifakebts.detector.data.model.NetworkType

@Entity(
    tableName = "anomaly_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["risk_level"]),
        Index(value = ["cell_id"])
    ]
)
data class AnomalyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "risk_level")
    val riskLevel: AnomalyLevel,
    @ColumnInfo(name = "confidence")
    val confidence: ConfidenceLevel,
    @ColumnInfo(name = "triggered_rules_json")
    val triggeredRulesJson: String,
    @ColumnInfo(name = "weakening_factors_json")
    val weakeningFactorsJson: String,
    @ColumnInfo(name = "primary_reason")
    val primaryReason: String,
    @ColumnInfo(name = "cell_id")
    val cellId: Long?,
    @ColumnInfo(name = "lac_tac")
    val lacTac: Int?,
    @ColumnInfo(name = "plmn")
    val plmn: String?,
    @ColumnInfo(name = "network_type")
    val networkType: NetworkType,
    @ColumnInfo(name = "signal_dbm")
    val signalDbm: Int?,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?,
    @ColumnInfo(name = "is_resolved")
    val isResolved: Boolean = false,
    @ColumnInfo(name = "user_marked_safe")
    val userMarkedSafe: Boolean = false
)
