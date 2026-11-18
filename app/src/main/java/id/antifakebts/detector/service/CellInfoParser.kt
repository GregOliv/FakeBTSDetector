package id.antifakebts.detector.service

import android.os.Build
import android.telephony.CellIdentityCdma
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellIdentityWcdma
import android.telephony.CellInfo
import android.telephony.CellInfoCdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.CellSignalStrengthWcdma
import id.antifakebts.detector.data.model.CellObservation
import id.antifakebts.detector.data.model.NetworkType

class CellInfoParser {

    fun parse(
        cellInfoList: List<CellInfo>?,
        operatorAlpha: String?,
        latitude: Double? = null,
        longitude: Double? = null
    ): List<CellObservation> {
        if (cellInfoList.isNullOrEmpty()) return emptyList()

        val totalNeighbors = cellInfoList.count { !it.isRegistered }

        return cellInfoList.mapNotNull { cellInfo ->
            parseSingleCell(cellInfo, operatorAlpha, totalNeighbors, latitude, longitude)
        }
    }

    private fun parseSingleCell(
        cellInfo: CellInfo,
        operatorAlpha: String?,
        neighborCount: Int,
        latitude: Double?,
        longitude: Double?
    ): CellObservation? {
        val now = System.currentTimeMillis()
        val isRegistered = cellInfo.isRegistered

        return when (cellInfo) {
            is CellInfoLte -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                val cid = sanitizeLong(identity.ci.toLong())
                val tac = sanitizeInt(identity.tac)
                val mcc = sanitizeString(identity.mccString)
                val mnc = sanitizeString(identity.mncString)
                val plmn = formatPlmn(mcc, mnc)
                val earfcn = sanitizeInt(identity.earfcn)
                val pci = sanitizeInt(identity.pci)
                val dbm = sanitizeDbm(signal.dbm)
                val rsrp = sanitizeDbm(signal.rsrp)
                val rsrq = sanitizeInt(signal.rsrq)

                CellObservation(
                    timestamp = now,
                    networkType = NetworkType.LTE,
                    cellId = cid,
                    lacTac = tac,
                    mcc = mcc,
                    mnc = mnc,
                    plmn = plmn,
                    operatorAlpha = operatorAlpha,
                    signalDbm = dbm,
                    signalRsrp = rsrp,
                    signalRsrq = rsrq,
                    channelNumber = earfcn,
                    physicalCellId = pci,
                    isRegistered = isRegistered,
                    neighborCount = neighborCount,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            is CellInfoNr -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val identity = cellInfo.cellIdentity as? CellIdentityNr
                    val signal = cellInfo.cellSignalStrength as? CellSignalStrengthNr
                    val nci = identity?.let { sanitizeLong(it.nci) }
                    val tac = identity?.let { sanitizeInt(it.tac) }
                    val mcc = identity?.let { sanitizeString(it.mccString) }
                    val mnc = identity?.let { sanitizeString(it.mncString) }
                    val plmn = formatPlmn(mcc, mnc)
                    val nrarfcn = identity?.let { sanitizeInt(it.nrarfcn) }
                    val pci = identity?.let { sanitizeInt(it.pci) }
                    val dbm = signal?.let { sanitizeDbm(it.dbm) }
                    val ssRsrp = signal?.let { sanitizeDbm(it.ssRsrp) }
                    val ssRsrq = signal?.let { sanitizeInt(it.ssRsrq) }

                    CellObservation(
                        timestamp = now,
                        networkType = NetworkType.NR,
                        cellId = nci,
                        lacTac = tac,
                        mcc = mcc,
                        mnc = mnc,
                        plmn = plmn,
                        operatorAlpha = operatorAlpha,
                        signalDbm = dbm,
                        signalRsrp = ssRsrp,
                        signalRsrq = ssRsrq,
                        channelNumber = nrarfcn,
                        physicalCellId = pci,
                        isRegistered = isRegistered,
                        neighborCount = neighborCount,
                        latitude = latitude,
                        longitude = longitude
                    )
                } else null
            }

            is CellInfoGsm -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                val cid = sanitizeLong(identity.cid.toLong())
                val lac = sanitizeInt(identity.lac)
                val mcc = sanitizeString(identity.mccString)
                val mnc = sanitizeString(identity.mncString)
                val plmn = formatPlmn(mcc, mnc)
                val arfcn = sanitizeInt(identity.arfcn)
                val bsic = sanitizeInt(identity.bsic)
                val dbm = sanitizeDbm(signal.dbm)

                CellObservation(
                    timestamp = now,
                    networkType = NetworkType.GSM,
                    cellId = cid,
                    lacTac = lac,
                    mcc = mcc,
                    mnc = mnc,
                    plmn = plmn,
                    operatorAlpha = operatorAlpha,
                    signalDbm = dbm,
                    channelNumber = arfcn,
                    physicalCellId = bsic,
                    isRegistered = isRegistered,
                    neighborCount = neighborCount,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            is CellInfoWcdma -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                val cid = sanitizeLong(identity.cid.toLong())
                val lac = sanitizeInt(identity.lac)
                val mcc = sanitizeString(identity.mccString)
                val mnc = sanitizeString(identity.mncString)
                val plmn = formatPlmn(mcc, mnc)
                val uarfcn = sanitizeInt(identity.uarfcn)
                val psc = sanitizeInt(identity.psc)
                val dbm = sanitizeDbm(signal.dbm)

                CellObservation(
                    timestamp = now,
                    networkType = NetworkType.WCDMA,
                    cellId = cid,
                    lacTac = lac,
                    mcc = mcc,
                    mnc = mnc,
                    plmn = plmn,
                    operatorAlpha = operatorAlpha,
                    signalDbm = dbm,
                    channelNumber = uarfcn,
                    physicalCellId = psc,
                    isRegistered = isRegistered,
                    neighborCount = neighborCount,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            is CellInfoCdma -> {
                val identity = cellInfo.cellIdentity
                val signal = cellInfo.cellSignalStrength
                val bid = sanitizeLong(identity.basestationId.toLong())
                val nid = sanitizeInt(identity.networkId)
                val dbm = sanitizeDbm(signal.dbm)

                CellObservation(
                    timestamp = now,
                    networkType = NetworkType.CDMA,
                    cellId = bid,
                    lacTac = nid,
                    operatorAlpha = operatorAlpha,
                    signalDbm = dbm,
                    isRegistered = isRegistered,
                    neighborCount = neighborCount,
                    latitude = latitude,
                    longitude = longitude
                )
            }

            else -> null
        }
    }

    private fun sanitizeInt(value: Int): Int? {
        return if (value == Int.MAX_VALUE || value == 2147483647 || value == -1 || value == 65535) null else value
    }

    private fun sanitizeLong(value: Long): Long? {
        return if (value == Long.MAX_VALUE || value == 2147483647L || value == -1L || value <= 0L) null else value
    }

    private fun sanitizeDbm(value: Int): Int? {
        return if (value == Int.MAX_VALUE || value == 2147483647 || value < -150 || value > 0) null else value
    }

    private fun sanitizeString(value: String?): String? {
        return if (value.isNullOrBlank() || value.equals("null", ignoreCase = true)) null else value.trim()
    }

    private fun formatPlmn(mcc: String?, mnc: String?): String? {
        return if (!mcc.isNullOrBlank() && !mnc.isNullOrBlank()) "${mcc}${mnc}" else null
    }
}
