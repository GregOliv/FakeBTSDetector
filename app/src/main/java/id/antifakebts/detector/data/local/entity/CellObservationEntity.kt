package id.antifakebts.detector.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.model.NetworkType

@Entity(
    tableName = "cell_observations",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["cell_id"]),
        Index(value = ["is_registered"])
    ]
)
data class CellObservationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    @ColumnInfo(name = "network_type")
    val networkType: NetworkType,
    @ColumnInfo(name = "cell_id")
    val cellId: Long?,
    @ColumnInfo(name = "lac_tac")
    val lacTac: Int?,
    @ColumnInfo(name = "mcc")
    val mcc: String?,
    @ColumnInfo(name = "mnc")
    val mnc: String?,
    @ColumnInfo(name = "plmn")
    val plmn: String?,
    @ColumnInfo(name = "operator_alpha")
    val operatorAlpha: String?,
    @ColumnInfo(name = "signal_dbm")
    val signalDbm: Int?,
    @ColumnInfo(name = "signal_rsrp")
    val signalRsrp: Int?,
    @ColumnInfo(name = "signal_rsrq")
    val signalRsrq: Int?,
    @ColumnInfo(name = "channel_number")
    val channelNumber: Int?,
    @ColumnInfo(name = "physical_cell_id")
    val physicalCellId: Int?,
    @ColumnInfo(name = "is_registered")
    val isRegistered: Boolean,
    @ColumnInfo(name = "neighbor_count")
    val neighborCount: Int,
    @ColumnInfo(name = "latitude")
    val latitude: Double?,
    @ColumnInfo(name = "longitude")
    val longitude: Double?
) {
    fun toDomain(): CellObservation = CellObservation(
        id = id,
        timestamp = timestamp,
        networkType = networkType,
        cellId = cellId,
        lacTac = lacTac,
        mcc = mcc,
        mnc = mnc,
        plmn = plmn,
        operatorAlpha = operatorAlpha,
        signalDbm = signalDbm,
        signalRsrp = signalRsrp,
        signalRsrq = signalRsrq,
        channelNumber = channelNumber,
        physicalCellId = physicalCellId,
        isRegistered = isRegistered,
        neighborCount = neighborCount,
        latitude = latitude,
        longitude = longitude
    )

    companion object {
        fun fromDomain(model: CellObservation): CellObservationEntity = CellObservationEntity(
            id = model.id,
            timestamp = model.timestamp,
            networkType = model.networkType,
            cellId = model.cellId,
            lacTac = model.lacTac,
            mcc = model.mcc,
            mnc = model.mnc,
            plmn = model.plmn,
            operatorAlpha = model.operatorAlpha,
            signalDbm = model.signalDbm,
            signalRsrp = model.signalRsrp,
            signalRsrq = model.signalRsrq,
            channelNumber = model.channelNumber,
            physicalCellId = model.physicalCellId,
            isRegistered = model.isRegistered,
            neighborCount = model.neighborCount,
            latitude = model.latitude,
            longitude = model.longitude
        )
    }
}
