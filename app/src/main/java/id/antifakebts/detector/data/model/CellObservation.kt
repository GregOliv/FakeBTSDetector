package id.antifakebts.detector.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CellObservation(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val networkType: NetworkType = NetworkType.UNKNOWN,
    val cellId: Long? = null,
    val lacTac: Int? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val plmn: String? = null,
    val operatorAlpha: String? = null,
    val signalDbm: Int? = null,
    val signalRsrp: Int? = null,
    val signalRsrq: Int? = null,
    val channelNumber: Int? = null,
    val physicalCellId: Int? = null,
    val isRegistered: Boolean = false,
    val neighborCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun hasValidCellIdentity(): Boolean {
        return cellId != null && cellId > 0 && cellId != 2147483647L && cellId != Long.MAX_VALUE
    }

    fun getDisplayCellId(): String {
        return if (hasValidCellIdentity()) cellId.toString() else "Tidak Tersedia"
    }

    fun getDisplayTac(): String {
        return if (lacTac != null && lacTac > 0 && lacTac != 65535 && lacTac != 2147483647) {
            lacTac.toString()
        } else {
            "Tidak Tersedia"
        }
    }

    fun getCoarseGridKey(): String {
        val latKey = latitude?.let { (Math.round(it * 100.0) / 100.0).toString() } ?: "unknown"
        val lonKey = longitude?.let { (Math.round(it * 100.0) / 100.0).toString() } ?: "unknown"
        return "${latKey}_${lonKey}"
    }
}
