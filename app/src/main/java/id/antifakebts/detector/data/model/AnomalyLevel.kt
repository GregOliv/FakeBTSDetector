package id.antifakebts.detector.data.model

enum class AnomalyLevel(val labelId: String) {
    SAFE("Aman"),
    LOW("Rendah"),
    MEDIUM("Sedang"),
    HIGH("Tinggi")
}

enum class ConfidenceLevel(val labelId: String) {
    LOW("Rendah"),
    MEDIUM("Sedang"),
    HIGH("Tinggi")
}
