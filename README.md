# FakeBTS Detector

Aplikasi Android untuk mendeteksi anomali jaringan seluler (indikasi Fake BTS, IMSI Catcher, dan SMS Blaster) menggunakan API publik Android dan kebijakan Device Policy, berjalan tanpa akses root.

## 1. Batasan Teknis dan Prinsip Kerja

Aplikasi ini beroperasi di lapisan aplikasi (user space) Android standar dengan prinsip transparansi probabilitas:

1. **Bukan Pemblokir Fisik Baseband**: Aplikasi tidak dapat membatalkan atau memblokir koneksi radio ke BTS tertentu di tingkat perangkat keras. Keputusan pemilihan sel (cell selection dan handover) dikendalikan langsung oleh firmware baseband modem.
2. **Tanpa Akses Root**:
   - Aplikasi tidak dapat mengaktifkan Airplane Mode lewat kode karena membutuhkan izin tanda tangan sistem (`android.permission.WRITE_SECURE_SETTINGS`).
   - Aplikasi tidak dapat mematikan data seluler secara langsung karena memerlukan izin `MODIFY_PHONE_STATE` atau hak carrier UICC.
   - Aplikasi tidak dapat memodifikasi bitmask jaringan (`setPreferredNetworkTypeBitmask`) tanpa hak akses carrier atau sistem.
3. **Format Luaran**: Seluruh hasil analisis disajikan dalam bentuk Skor Risiko, Tingkat Keyakinan (Confidence: Rendah / Sedang / Tinggi), serta Daftar Alasan Teknis dan Alasan yang Melemahkan Dugaan.

---

## 2. Arsitektur dan Spesifikasi Teknis

- **Bahasa**: Kotlin 1.9.24
- **Kompatibilitas**: minSdk 29 (Android 10), targetSdk 34 (Android 14)
- **Antarmuka Pengguna**: Jetpack Compose dengan Material 3
- **Injeksi Dependensi**: Koin 3.5.6
- **Penyimpanan Lokal**: Room Database 2.6.1 (dengan enkripsi dan pengecualian cadangan data)
- **Asinkron**: Kotlin Coroutines & Flow
- **Pola Arsitektur**: MVVM (Model-View-ViewModel) terpisah dalam boundary Domain, Data, Service, dan Presentation

---

## 3. Komponen Utama dan Alur Kerja

```
[ TelephonyCallback / PhoneStateListener ]
                    │
                    ▼
          [ CellInfoParser ]
                    │
                    ▼
         [ RuleEngine & Filters ]
      ┌─────────────┼─────────────┐
      ▼             ▼             ▼
[Korelasi]    [Persistensi]   [Konteks Gerak]
      └─────────────┬─────────────┘
                    ▼
          [ AnomalyScorer ]
                    │
                    ▼
       [ Tiered Response Manager ]
      ┌─────────────┴─────────────┐
      ▼                           ▼
[Mode Reguler]           [Mode Device Owner]
(Notifikasi + Panduan)   (DISALLOW_CELLULAR_2G + DISALLOW_SMS)
```

### 3.1 Pengumpulan Data Seluler (Monitoring Service)
- Berjalan sebagai Foreground Service dengan tipe `location`.
- Menggunakan `TelephonyCallback` untuk Android 12+ (API 31+) dan fallback `PhoneStateListener` untuk Android 10-11 (API 29-30).
- Memantau parameter radio:
  - Identitas Sel: Cell Identity (CID/CI), Tracking Area Code (TAC) / Location Area Code (LAC), Mobile Country Code (MCC), Mobile Network Code (MNC).
  - Kekuatan Sinyal: RSRP, RSSI, RSRQ, SINR (tergantung teknologi radio).
  - Frekuensi Saluran: EARFCN (LTE), ARFCN (GSM), UARFCN (UMTS), NR-ARFCN (5G).
  - Daftar Sel Tetangga (Neighboring Cells).

### 3.2 Aturan Deteksi (Rule Engine)
Aturan deteksi memiliki bobot individual yang dapat dikonfigurasi:
1. **Forced 2G Downgrade**: Penurunan paksa dari 4G/5G ke 2G (GSM) saat sinyal 4G/5G sebelumnya berada pada level penerimaan yang baik.
2. **Unknown Cell ID**: Sel tidak terdaftar pada baseline observasi lokal maupun database OpenCellID.
3. **Sudden High Signal**: Sinyal seluler sangat kuat muncul mendadak pada lokasi yang historisnya memiliki sinyal lemah.
4. **Rapid TAC/LAC Fluctuation**: Perubahan nilai TAC/LAC berulang kali tanpa adanya pergerakan fisik perangkat.
5. **Cell ID Conflict**: Cell ID yang identik terdeteksi memiliki TAC/LAC berbeda di lokasi yang sama.
6. **Isolated Cell (No Neighbors)**: Sel aktif tidak menyiarkan daftar sel tetangga (neighbor list), tipikal pada perangkat portable IMSI catcher komersial.
7. **PLMN Mismatch**: Operator jaringan sel (MCC-MNC) tidak sesuai dengan profil kartu SIM aktif.

---

## 4. Pipeline Anti False-Positive

Untuk mencegah alarm palsu, diterapkan filter berjenjang:

1. **Korelasi Multi-Kategori**:
   - Status risiko "Tinggi" hanya aktif jika minimal 2 aturan dari kategori sinyal berbeda terpicu bersamaan (contoh: Anomali Identitas Sel dikorelasikan dengan Anomali Perilaku Radio).
   - Satu aturan yang berdiri sendiri dibatasi maksimal pada tingkat risiko "Rendah".
2. **Filter Persistensi**:
   - Indikasi anomali harus bertahan selama jendela waktu tertentu ($N$ detik atau $M$ pembacaan berturut-turut) sebelum skor dinaikkan.
3. **Hysteresis dan Cooldown**:
   - Ambang skor untuk menaikkan status peringatan disetel lebih tinggi daripada ambang skor untuk menurunkannya.
   - Jeda waktu minimum (cooldown) diberlakukan antar peringatan agar tidak terjadi spam notifikasi.
4. **Analisis Penyebab Normal (Weakening Factors)**:
   - Penurunan ke 2G diperiksa terhadap riwayat sinyal sebelumnya. Jika sinyal memang melemah secara bertahap (misal: berada di basement, lift, atau perbatasan jangkauan operator), skor anomali ditekan.
   - Status roaming internasional diperlakukan sebagai kondisi wajar saat terjadi ketidakcocokan PLMN.
5. **Konteks Pergerakan Pengguna**:
   - Perubahan Cell ID atau TAC yang disertai perpindahan lokasi fisik (berdasarkan perubahan lokasi kasar) diidentifikasi sebagai handover normal.
6. **Baseline Adaptif & Mode Belajar**:
   - Mode belajar (shadow mode) berjalan pada hari awal instalasi: sistem hanya mencatat sel tanpa memicu notifikasi aktif.
   - Sel yang sering diobservasi di lokasi yang sama akan menurunkan bobot risiko secara otomatis. Pengguna dapat menandai sel tertentu sebagai aman (whitelist).

---

## 5. Respons Bertingkat (Tiered Response)

### 5.1 Tingkat Rendah (Low Risk)
- Tindakan: Pencatatan ke log Room Database lokal.
- Notifikasi: Notifikasi senyap pada tray sistem tanpa getaran atau suara.

### 5.2 Tingkat Sedang (Medium Risk)
- Tindakan: Notifikasi informatif jelas.
- Mitigasi Tambahan: Pemeriksaan notifikasi SMS masuk yang mengandung tautan URL mencurigakan melalui `NotificationListenerService` (hanya aktif jika izin diberikan secara eksplisit oleh pengguna).

### 5.3 Tingkat Tinggi (High Risk)
1. **Pada Perangkat Standar (Tanpa Device Owner)**:
   - Notifikasi prioritas tinggi dengan pola getar peringatan.
   - Tautan satu ketukan langsung ke halaman pengaturan jaringan sistem Android (`Settings.ACTION_NETWORK_OPERATOR_SETTINGS`).
   - Panduan praktis: petunjuk mematikan 2G secara manual lewat menu setelan perangkat dan himbauan untuk tidak memasukkan kode OTP atau menekan tautan SMS.
2. **Pada Perangkat dengan Status Device Owner (MDM Mode)**:
   - Hanya dieksekusi jika fitur diaktifkan secara eksplisit oleh pengguna (default: Nonaktif).
   - Menerapkan restriksi kebijakan `UserManager.DISALLOW_CELLULAR_2G` pada Android 14+ (API 34) jika didukung oleh HAL vendor radio.
   - Menerapkan restriksi kebijakan sementara `UserManager.DISALLOW_SMS` untuk mencegah eksfiltrasi atau injeksi SMS spoofing.
   - Pengaman Otomatis:
     - **Auto-Revert**: Pembatasan dicabut otomatis setelah kondisi sinyal kembali normal selama durasi tertentu atau mencapai batas waktu timeout maksimum.
     - **Manual Override**: Tombol manual di aplikasi untuk mencabut pembatasan seketika.
     - **Rate Limiting**: Pembatasan otomatis dibatasi frekuensinya (maksimal $X$ kali per jam) untuk mencegah loop aktivasi.

---

## 6. Daftar Izin Aplikasi dan Kebutuhan

| Izin | Alasan Teknis |
|---|---|
| `ACCESS_FINE_LOCATION` | Diwajibkan oleh Android OS agar TelephonyManager dapat mengakses Cell Identity dan Cell Info. |
| `ACCESS_COARSE_LOCATION` | Digunakan untuk korelasi perpindahan lokasi kasar guna menghemat konsumsi baterai. |
| `READ_PHONE_STATE` | Membaca status SIM, operator PLMN terdaftar, dan status radio seluler. |
| `POST_NOTIFICATIONS` | Menampilkan status pemantauan layanan latar depan dan peringatan anomali (Android 13+). |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION` | Menjaga keberlanjutan pemantauan saat aplikasi berada di latar belakang pada Android 14+. |
| `RECEIVE_BOOT_COMPLETED` | Mengaktifkan kembali pemantauan setelah perangkat dihidupkan ulang sesuai preferensi pengguna. |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Memeriksa teks notifikasi SMS masuk untuk identifikasi URL berbahaya saat status Sedang. |
| `BIND_DEVICE_ADMIN` | Menerima wewenang Device Owner untuk penegakan kebijakan pembatasan 2G dan SMS. |