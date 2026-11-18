# FakeBTS Detector

FakeBTS Detector adalah aplikasi Android tanpa root yang memantau perubahan parameter jaringan seluler untuk mengidentifikasi indikasi anomali radio seperti IMSI Catcher atau SMS Blaster. Aplikasi ini ditujukan untuk peneliti keamanan, pemerhati privasi, dan pengguna yang ingin mengetahui metrik seluler di sekitarnya. Aplikasi ini berfungsi sebagai detektor anomali berbasis skor risiko dan aturan heuristik, bukan alat yang dapat memastikan keaslian menara pemancar atau memblokir koneksi radio seluler secara fisik.

## Status Proyek

Status implementasi komponen di dalam kode sumber saat ini:

* Pemantauan Telephony (CellInfo, TelephonyCallback, PhoneStateListener): **Sudah ada**
* Mesin Aturan Heuristik Berbobot (6 aturan seluler): **Sudah ada**
* Konfigurasi Aktif/Nonaktif per Aturan di UI: **Rencana** (seluruh aturan saat ini terdaftar aktif di kode dependency injection)
* Mekanisme Anti False-Positive (Korelasi lintas kategori, filter persistensi, hysteresis, cooldown alert, baseline lokal per grid): **Sudah ada**
* Mode Belajar (Shadow Mode 7 hari pertama): **Sudah ada**
* Klasifikasi Tingkat Risiko (SAFE, LOW, MEDIUM, HIGH) dan Tingkat Keyakinan (LOW, MEDIUM, HIGH): **Sudah ada**
* Integrasi Device Owner (DISALLOW_CELLULAR_2G, DISALLOW_SMS, timer auto-revert, tombol override manual): **Sudah ada**
* Pindai Tautan Mencurigakan pada Notifikasi SMS via NotificationListener: **Sudah ada**
* Riwayat Log Anomali dan Ekspor Data CSV/JSON via FileProvider: **Sudah ada**
* Klien API OpenCellID (lookup menara seluler): **Sebagian** (kelas klien `OpenCellIdClient` sudah ada dengan pembatasan rate limit dan cache memori, namun belum diintegrasikan ke alur UI atau Service)
* Pengiriman Event ke Endpoint HTTPS Eksternal: **Rencana** (belum ada kode pengiriman telemetri jaringan luar)
* Antarmuka Pengguna Jetpack Compose (Dashboard Status, Riwayat, Inspektor Sinyal, Setelan): **Sudah ada**

## Fitur

* **Pemantauan Seluler Berkelanjutan**: Menggunakan foreground service untuk membaca daftar sel aktif dan sel tetangga melalui API `TelephonyCallback` pada Android 12 ke atas, dengan fallback `PhoneStateListener` untuk Android 10 dan 11.
* **Mesin Aturan Heuristik Berbobot**: Mengalkulasi skor anomali dari 6 aturan: penurunan paksa ke 2G (`DowngradeRule`), identitas sel baru (`UnknownCellRule`), lonjakan daya sinyal tanpa perpindahan lokasi (`SuddenSignalRule`), ketidakcocokan area pelacakan LAC/TAC (`TacMismatchRule`), sel terisolasi tanpa daftar tetangga (`NeighborListRule`), dan diskrepansi PLMN terhadap kartu SIM (`PlmnMismatchRule`). Pengaturan on/off untuk tiap aturan secara individual saat ini berstatus **Rencana** karena aturan masih didaftarkan secara statis di modul injeksi dependensi.
* **Filter Anti False-Positive Bertingkat**: Mencegah alarm palsu dengan mewajibkan korelasi minimal dua kategori aturan independen untuk eskalasi risiko tinggi, filter persistensi (minimal pembacaan berturut-turut atau durasi waktu tertentu), peredaman osilasi state via hysteresis, jeda minimum (cooldown) antar peringatan, serta evaluasi faktor pelemah seperti pelemahan sinyal wajar di batas jangkauan atau pergerakan pengguna.
* **Baseline Seluler Lokal**: Menyimpan riwayat rata-rata sinyal dan parameter sel yang pernah terhubung pada grid koordinat geografis di database Room lokal, sehingga sel resmi tidak berulang kali dianggap mencurigakan.
* **Mode Belajar (Shadow Mode)**: Meredam notifikasi peringatan tinggi selama 7 hari pertama setelah instalasi agar aplikasi dapat membentuk baseline lokal tanpa membebani pengguna dengan peringatan awal.
* **Klasifikasi Risiko dan Penjelasan Transparan**: Menampilkan tingkat risiko (Aman, Rendah, Sedang, Tinggi) beserta derajat keyakinan dan daftar alasan teknis yang memicu kenaikan maupun penurunan skor.
* **Mitigasi Otomatis Bertingkat (Device Owner)**: Mengizinkan pemblokiran jaringan 2G (`DISALLOW_CELLULAR_2G` pada Android 14+) dan penonaktifan sementara fungsi SMS (`DISALLOW_SMS`) saat risiko tinggi terdeteksi, dilengkapi batas aksi per jam, penghitung mundur pemulihan otomatis (auto-revert), dan tombol pembatalan manual oleh pengguna.
* **Peringatan SMS Mencurigakan**: Memanfaatkan `NotificationListenerService` untuk mendeteksi notifikasi pesan masuk dari aplikasi SMS yang memuat tautan web saat status radio seluler terindikasi anomali.
* **Pencatatan Riwayat dan Ekspor Berkas**: Menyimpan riwayat anomali ke database lokal dengan kemampuan mengekspor data ke format CSV atau JSON melalui Android FileProvider untuk keperluan analisis mandiri.
* **Klien OpenCellID**: Menyediakan komponen pencarian informasi menara seluler publik untuk memvalidasi keberadaan menara di database global (berstatus **Sebagian**, belum terhubung ke antarmuka pengguna).
* **Antarmuka Pengguna Reaktif**: Memuat empat layar utama (Status, Riwayat, Inspektor Radio Seluler, dan Setelan) yang dibangun menggunakan Jetpack Compose dengan dukungan tema gelap dan terang.

## Cara Kerja

```
[ Antena Perangkat ]
        │
        ▼
[ TelephonyManager & LocationTracker ] ── (CellInfo, RSRP/RSSI, LAC/TAC, PLMN, GPS)
        │
        ▼
[ CellInfoParser ] ── (Pembersihan nilai Integer.MAX_VALUE / UNAVAILABLE)
        │
        ▼
[ Local Baseline Repository ] ── (Pengecekan riwayat menara pada koordinat lokal)
        │
        ▼
[ RuleEngine (6 Aturan Heuristik) ] ── (Evaluasi bobot skor & faktor pelemah)
        │
        ▼
[ PersistenceFilter & HysteresisTracker ] ── (Verifikasi durasi & stabilisasi state)
        │
        ├────────────────────────────────┬────────────────────────────────┐
        ▼                                ▼                                ▼
[ Notifikasi Sistem ]         [ Dashboard Compose UI ]       [ DevicePolicyManager ]
(Peringatan risiko)            (Status & Inspektor)          (DISALLOW_CELLULAR_2G & SMS)
```

Alur dimulai dari pembacaan data seluler berkala oleh layanan latar depan melalui `TelephonyManager`. Data mentah dibersihkan oleh parser dari nilai anomali bawaan vendor (seperti `Integer.MAX_VALUE`), lalu dibandingkan dengan riwayat baseline lokal dan status pergerakan pengguna. Seluruh aturan mengevaluasi data untuk menghasilkan skor indikasi anomali beserta faktor pelemah normal. Skor tersebut disaring oleh filter persistensi dan penstabil hysteresis. Jika indikasi anomali terbukti persisten dan memenuhi ambang batas, aplikasi memperbarui dashboard antarmuka, menerbitkan notifikasi peringatan, dan jika diizinkan dalam mode Device Owner, menerapkan pembatasan sementara jaringan 2G atau SMS.

## Batasan

Aplikasi ini beroperasi dalam batasan sistem operasi Android standar tanpa akses root. Batasan teknis yang tidak dapat dihindari mencakup:

* **Tidak Dapat Memblokir Koneksi BTS Palsu Secara Fisik**: Aplikasi tidak memiliki kontrol atas modul baseband/modem radio seluler. Keputusan pemilihan sel dan registrasi menara dilakukan sepenuhnya oleh firmware modem perangkat keras.
* **Tidak Dapat Membaca Pesan Protokol Pensinyalan Seluler**: Lapisan protokol pensinyalan tingkat rendah (seperti pesan NAS/RRC, parameter penolakan autentikasi, ciphering indicator A5/1 atau A5/3) tidak diekspos oleh API publik Android kepada aplikasi non-root.
* **Tidak Dapat Mengubah Pengaturan Jaringan dari Kode Non-Owner**: Aplikasi non-root standar tidak dapat mematikan data seluler, tidak dapat mengaktifkan mode pesawat secara terprogram, dan tidak dapat mengubah mode preferensi jaringan (`setPreferredNetworkTypeBitmask`). Tindakan mitigasi yang dimungkinkan terbatas pada kebijakan restriksi pengguna via mode Device Owner.
* **Tidak Ada Jaminan Deteksi Mutlak**: Perangkat IMSI Catcher modern yang meniru identitas dan daya pancar menara resmi secara presisi dapat lolos dari pengamatan (false negative).
* **Potensi Indikasi Salah (False Positive dan False Negative)**: Fluktuasi sinyal alami akibat cuaca, material gedung, perjalanan cepat antar-menara, atau konfigurasi pemeliharaan dari operator seluler resmi dapat memicu skor anomali. Seluruh hasil yang ditampilkan bersifat indikasi kemungkinan risiko, bukan pembuktian forensik definitif.

## Persyaratan dan Izin

### Spesifikasi Sistem
* **minSdkVersion**: 29 (Android 10)
* **targetSdkVersion**: 34 (Android 14)
* **Kompilasi JDK**: Java 17

### Izin yang Diminta dan Alasannya
* `android.permission.ACCESS_FINE_LOCATION` dan `ACCESS_COARSE_LOCATION`: Diwajibkan oleh platform Android agar aplikasi diizinkan membaca data pengenal seluler (`CellInfo`) serta untuk menghitung konteks perpindahan posisi geografis.
* `android.permission.READ_PHONE_STATE`: Digunakan untuk memantau perubahan status layanan radio, nama operator SIM, dan status roaming internasional.
* `android.permission.POST_NOTIFICATIONS`: Diperlukan pada Android 13 (API 33) ke atas untuk menampilkan notifikasi foreground service dan peringatan anomali.
* `android.permission.FOREGROUND_SERVICE` dan `FOREGROUND_SERVICE_LOCATION`: Diperlukan untuk menjaga keberlangsungan pemantauan seluler di latar belakang tanpa dihentikan sistem. Tipe lokasi diwajibkan oleh Android 14 (API 34) saat service mengakses API seluler berbasis lokasi.
* `android.permission.RECEIVE_BOOT_COMPLETED`: Digunakan oleh komponen penerima siaran saat perangkat selesai dinyalakan.

### Fitur yang Memerlukan Dukungan Khusus
* **Restriksi 2G (`UserManager.DISALLOW_CELLULAR_2G`)**: Membutuhkan minimal Android 14 (API 34) dan status aplikasi sebagai Device Owner.
* **TelephonyCallback API**: Membutuhkan minimal Android 12 (API 31). Pada Android 10 dan 11, aplikasi menggunakan fallback `PhoneStateListener`.
* **Akses Notifikasi SMS**: Memerlukan persetujuan manual pengguna pada menu pengaturan sistem "Notification Access" agar `SmsNotificationListener` dapat berjalan.

## Privasi

* **Penyimpanan Lokal Eksklusif**: Seluruh data riwayat pengamatan sel (`CellObservationEntity`), data baseline menara (`CellBaselineEntity`), dan catatan log anomali (`AnomalyLogEntity`) disimpan secara eksklusif di memori penyimpanan internal perangkat menggunakan database SQLite lokal (Room) dengan nama berkas `fakebts_detector.db`. Konfigurasi pengguna disimpan di `SharedPreferences` lokal.
* **Nol Pengiriman Telemetri Otomatis**: Aplikasi tidak memuat pustaka pelacakan, analitik, iklan, atau pelaporan kerusakan pihak ketiga. Tidak ada data lokasi, data identitas seluler, maupun riwayat log yang dikirim ke server eksternal tanpa tindakan manual pengguna.
* **Pencarian OpenCellID Bersifat Manual**: Permintaan jaringan keluar hanya terjadi jika pengguna secara sadar memasukkan API key OpenCellID pada berkas/setelan untuk mencari data menara tertentu.
* **Berbagi Log Manual**: Berkas log CSV atau JSON hanya keluar dari perangkat jika pengguna secara eksplisit menekan tombol ekspor dan memilih aplikasi tujuan melalui pemilih sistem Android (`Intent.ACTION_SEND`).

## Cara Membangun dan Menjalankan

### Persyaratan Lingkungan
* Android SDK (API Level 34)
* JDK 17
* Gradle 8.4 (atau menggunakan `gradlew` bawaan)

### Perintah Pembangunan Lokal

Untuk menjalankan pengujian unit dan replay:
```bash
./gradlew testDebugUnitTest
```

Untuk mengompilasi APK debug:
```bash
./gradlew assembleDebug
```
Berkas APK yang dihasilkan berada di lokasi:
`app/build/outputs/apk/debug/app-debug.apk`

### Mengambil APK dari GitHub Actions
Bila proyek ini diunggah ke repositori GitHub yang mengaktifkan workflow `.github/workflows/build-apk.yml`:
1. Buka tab **Actions** pada halaman repositori GitHub.
2. Pilih alur kerja **Build Android APK** pada commit terbaru.
3. Gulir ke bagian **Artifacts** di bagian bawah ringkasan eksekusi.
4. Unduh arsip **FakeBTSDetector-debug** untuk memperoleh berkas `app-debug.apk`. Laporan pengujian unit juga dapat diunduh pada artefak **unit-test-reports**.

## Cara Menguji

### 1. Pengujian Unit dan Skenario Replay
Aplikasi dilengkapi pengujian unit otomatis yang dapat dijalankan tanpa emulator:
* **Uji Logika Ambang Batas (`RuleEngineTest`)**: Menguji batasan bahwa satu aturan tidak dapat menaikkan status ke risiko tinggi, serta memverifikasi kenaikan risiko saat minimal dua kategori aturan terpicu.
* **Uji Skenario False-Positive (`FalsePositiveScenariosTest`)**: Menguji lima kondisi lingkungan alami agar tidak menghasilkan alarm palsu: penurunan wajar sinyal di batas jangkauan (cell edge), redaman sinyal elevator/basement, perpindahan sel saat berkendara cepat, sel baru operator resmi yang memiliki tetangga, dan status roaming internasional.
* **Uji Replay Serangan (`ReplayEngineTest`)**: Menyimulasikan urutan pembacaan waktu nyata dari trafik 4G normal, dilanjutkan serangan penurunan paksa ke sel 2G rogue, hingga pemulihan kembali ke 4G normal melalui filter hysteresis.

Jalankan seluruh pengujian dengan perintah:
```bash
./gradlew testDebugUnitTest --info
```

### 2. Peringatan Keselamatan Pengujian Fisik
Dilarang keras menyiarkan atau memancarkan sinyal radio frekuensi seluler tiruan menggunakan perangkat lunak radio (SDR seperti HackRF atau BladeRF) tanpa izin lisensi spektrum frekuensi resmi dari regulator telekomunikasi pemerintah setempat. Penyiaran sinyal seluler ilegal melanggar hukum dan dapat mengganggu komunikasi publik serta panggilan darurat. Gunakan pengujian berbasis kode sintetis (unit test replay) untuk menguji respons aplikasi.

## Setup Device Owner untuk HP Uji

Fitur mitigasi otomatis (pemblokiran sementara jaringan 2G dan SMS) memerlukan hak istimewa Device Owner. Hak ini ditujukan untuk pengujian pada perangkat laboratorium khusus.

### Syarat Sebelum Aktivasi
1. Perangkat uji disarankan baru saja di-reset ke setelan pabrik (*factory reset*).
2. Tidak boleh ada akun pengguna yang terdaftar di perangkat (hapus seluruh akun Google, WhatsApp, email, dan akun lain dari menu Pengaturan > Akun).
3. USB Debugging harus aktif melalui Opsi Pengembang (Developer Options).

### Perintah Aktivasi via ADB
Jalankan perintah berikut dari komputer melalui terminal:

```bash
adb shell dpm set-device-owner id.antifakebts.detector/.receiver.FakeBtsDeviceAdminReceiver
```

Jika berhasil, terminal akan menampilkan konfirmasi:
`Success: Device owner set to package id.antifakebts.detector`

### Peringatan Pelepasan Device Owner
Sistem operasi Android tidak menyediakan antarmuka pengguna grafis untuk mencabut status Device Owner. Mencabut status ini umumnya memerlukan eksekusi perintah khusus programatik atau melakukan reset total perangkat ke setelan pabrik (*factory reset*). Jangan mengaktifkan Device Owner pada perangkat utama sehari-hari.

## Lisensi

Proyek ini berlisensi di bawah ketentuan **PolyForm Noncommercial License 1.0.0**.

Lisensi ini merupakan lisensi *source-available*, dan **BUKAN lisensi open source menurut definisi Open Source Initiative (OSI)** karena membatasi penggunaan komersial.

Ketentuan pokok lisensi:
* **Penggunaan yang Diizinkan**: Diperbolehkan untuk tujuan non-komersial, termasuk riset keamanan publik, eksperimen pribadi, studi edukasi, proyek hobi, dan penggunaan oleh organisasi nirlaba atau institusi pendidikan.
* **Kewajiban Distribusi dan Kredit**: Setiap salinan atau karya turunan wajib menyertakan naskah lisensi lengkap beserta baris pemberitahuan hak cipta (*Required Notice*).
* **Pembatasan Komersial**: Dilarang keras menggunakan, mendistribusikan, menyematkan, atau memanfaatkan perangkat lunak ini untuk kegiatan komersial, bisnis berbayar, atau mencari keuntungan finansial tanpa izin tertulis dari pemegang hak cipta.

Salinan naskah resmi lisensi dan Required Notice dapat dibaca pada berkas [LICENSE](file:///home/gregoriusoliverrachmadi/Documents/AntiFakeBTS/LICENSE).

## Kontak

* Email: oliver@gor.my.id
* Website: https://gor.my.id/
* LinkedIn: https://www.linkedin.com/in/gregoliverr/
* GitHub: https://github.com/GregOliv