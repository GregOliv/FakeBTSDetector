package id.antifakebts.detector.data.model

enum class NetworkType(val generation: String, val displayName: String) {
    GSM("2G", "GSM / 2G"),
    CDMA("2G/3G", "CDMA"),
    WCDMA("3G", "WCDMA / UMTS / 3G"),
    LTE("4G", "LTE / 4G"),
    NR("5G", "NR / 5G"),
    UNKNOWN("N/A", "Unknown");

    fun is2G(): Boolean = this == GSM || this == CDMA
    fun isModern(): Boolean = this == LTE || this == NR
}
