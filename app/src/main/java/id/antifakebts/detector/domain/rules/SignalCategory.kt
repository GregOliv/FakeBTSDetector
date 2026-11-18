package id.antifakebts.detector.domain.rules

enum class SignalCategory(val displayName: String) {
    NETWORK_BEHAVIOR("Perilaku Sinyal Radio"),
    CELL_IDENTITY("Identitas & Konfigurasi Sel"),
    TOPOLOGY("Topologi & Sel Tetangga")
}
