# Third-Party Notices

Proyek ini menggunakan sejumlah pustaka dan dependensi pihak ketiga. Dokumen ini mencatat lisensi dari masing-masing pustaka pihak ketiga yang digunakan dalam proses kompilasi dan pengujian aplikasi.

---

## Dependensi Aplikasi (Runtime & Compile Time)

| Nama Pustaka | Versi | Lisensi | Sumber Lisensi |
| :--- | :--- | :--- | :--- |
| `androidx.compose:compose-bom` | 2024.04.01 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.core:core-ktx` | 1.13.1 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.7.0 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.7.0 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.7.0 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.activity:activity-compose` | 1.8.2 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.compose.ui:ui` | via BOM (1.6.6) | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.compose.ui:ui-graphics` | via BOM (1.6.6) | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.compose.ui:ui-tooling-preview` | via BOM (1.6.6) | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.compose.material3:material3` | via BOM (1.2.1) | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.compose.material:material-icons-extended` | via BOM (1.6.6) | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.navigation:navigation-compose` | 2.7.7 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.room:room-runtime` | 2.6.1 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.room:room-ktx` | 2.6.1 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.room:room-compiler` (KSP) | 2.6.1 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `io.insert-koin:koin-android` | 3.5.6 | Apache License 2.0 | [Insert-Koin GitHub](https://github.com/InsertKoinIO/koin) |
| `io.insert-koin:koin-androidx-compose` | 3.5.6 | Apache License 2.0 | [Insert-Koin GitHub](https://github.com/InsertKoinIO/koin) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-android` | 1.8.0 | Apache License 2.0 | [Kotlin Coroutines GitHub](https://github.com/Kotlin/kotlinx.coroutines) |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.6.3 | Apache License 2.0 | [Kotlin Serialization GitHub](https://github.com/Kotlin/kotlinx.serialization) |

---

## Dependensi Pengujian (Test Scope Only)

Dependensi berikut hanya digunakan dalam pengujian unit (`testImplementation`) dan tidak dipaketkan ke dalam berkas biner APK produksi.

| Nama Pustaka | Versi | Lisensi | Sumber Lisensi |
| :--- | :--- | :--- | :--- |
| `junit:junit` | 4.13.2 | Eclipse Public License 1.0 (EPL-1.0) | [JUnit 4 GitHub](https://github.com/junit-team/junit4) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.8.0 | Apache License 2.0 | [Kotlin Coroutines GitHub](https://github.com/Kotlin/kotlinx.coroutines) |
| `io.mockk:mockk` | 1.13.10 | Apache License 2.0 | [MockK GitHub](https://github.com/mockk/mockk) |
| `androidx.test:core` | 1.5.0 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |
| `androidx.room:room-testing` | 2.6.1 | Apache License 2.0 | [Android Open Source Project](https://source.android.com/) |

---

## Evaluasi Kompatibilitas Lisensi Copyleft

* **Analisis:** Seluruh dependensi runtime berlisensi permisif (**Apache License 2.0**). Tidak ada dependensi runtime yang menggunakan lisensi copyleft kuat (GPL v2/v3, AGPL) maupun copyleft lemah (LGPL).
* **Pustaka Uji EPL-1.0:** JUnit 4 menggunakan EPL-1.0 yang bersifat copyleft lemah file-level, namun pustaka ini diisolasi sepenuhnya pada classpath pengujian unit lokal (`testImplementation`) dan tidak didistribusikan di dalam artefak APK aplikasi.
* **Kesimpulan:** Tidak ada konflik hukum antara dependensi pihak ketiga dan lisensi PolyForm Noncommercial 1.0.0 proyek ini.
