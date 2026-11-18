package id.antifakebts.detector.data.local.converter

import androidx.room.TypeConverter
import id.antifakebts.detector.data.model.AnomalyLevel
import id.antifakebts.detector.data.model.ConfidenceLevel
import id.antifakebts.detector.data.model.NetworkType

class DatabaseConverters {

    @TypeConverter
    fun fromNetworkType(value: NetworkType): String = value.name

    @TypeConverter
    fun toNetworkType(value: String): NetworkType = try {
        NetworkType.valueOf(value)
    } catch (e: IllegalArgumentException) {
        NetworkType.UNKNOWN
    }

    @TypeConverter
    fun fromAnomalyLevel(value: AnomalyLevel): String = value.name

    @TypeConverter
    fun toAnomalyLevel(value: String): AnomalyLevel = try {
        AnomalyLevel.valueOf(value)
    } catch (e: IllegalArgumentException) {
        AnomalyLevel.SAFE
    }

    @TypeConverter
    fun fromConfidenceLevel(value: ConfidenceLevel): String = value.name

    @TypeConverter
    fun toConfidenceLevel(value: String): ConfidenceLevel = try {
        ConfidenceLevel.valueOf(value)
    } catch (e: IllegalArgumentException) {
        ConfidenceLevel.LOW
    }
}
