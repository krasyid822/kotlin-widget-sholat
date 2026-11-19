# AGENTS.md

## 1. Tech Stack & Ecosystem (Full Kotlin - Android)
- **Primary Language**: Gunakan **100% Kotlin** untuk seluruh lapisan aplikasi (UI, Widget, AOD Screen, Overlay Modal, Audio/TTS Service, Business Logic, Data Layer, dan Build Scripts).
- **Build System**: Wajib menggunakan **Gradle Kotlin DSL (`build.gradle.kts`)** dan **Version Catalog (`libs.versions.toml`)**. Hindari penggunaan Groovy (`build.gradle`).
- **UI Framework**: Gunakan **Jetpack Compose** untuk layar konfigurasi, layar AOD, layar tabel tracking, dan Smart Modal UI; serta **Jetpack Glance (`androidx.glance:glance-appwidget`)** untuk homescreen widget. Dilarang membuat layout UI berbasis XML (`RemoteViews` manual), kecuali metadata XML yang diwajibkan oleh sistem Android (`res/xml/*_widget_info.xml` dan `AndroidManifest.xml`).
- **Local Storage**: Gunakan **Room** (`androidx.room`) untuk penyimpanan relasional sesi tracking salat dan riwayat tabel tracking, serta **DataStore Preferences** (`androidx.datastore:datastore-preferences`) untuk koordinat, metode perhitungan, URI audio kustom, dan toggle fitur opsional.
- **Concurrency**: Wajib menggunakan **Kotlin Coroutines** dan **Flow** (`StateFlow`/`SharedFlow`). Dilarang menggunakan RxJava, `AsyncTask`, atau manajemen thread manual (`java.lang.Thread`).

## 2. Prayer Times Calculation (`adhan-kotlin`)
- **Library Reference**: Wajib menggunakan pustaka **`com.batoulapps.adhan:adhan2`** (`https://github.com/batoulapps/adhan-kotlin`) untuk seluruh kalkulasi waktu salat, waktu & durasi Syuruq, serta arah kiblat secara lokal di perangkat. Dilarang menulis rumus astronomi manual, menggunakan versi legacy `adhan-java`, atau memanggil API jadwal salat eksternal.
- **Time Handling**: `adhan2` menggunakan `kotlinx.datetime` (`Instant`) dan menghasilkan waktu dalam UTC. Selalu konversi output `Instant` dari `PrayerTimes` ke zona waktu lokal perangkat (`TimeZone.currentSystemDefault()`) pada lapisan mapper/formatter.
- **Core API Usage**:
  - Gunakan `Coordinates(latitude, longitude)` yang tervalidasi untuk input lokasi.
  - Gunakan `DateComponents(year, month, day)` untuk tanggal kalkulasi.
  - Konfigurasi metode perhitungan wajib eksplisit menggunakan parameter standar Kemenag RI: `CalculationParameters` dengan `fajrAngle = 20.0`, `ishaAngle = 18.0`, dan `madhab = Madhab.SHAFI` (atau menyediakan opsi metode lain dari `CalculationMethod` dengan default Kemenag RI).
  - Atur `HighLatitudeRule` secara eksplisit pada objek `CalculationParameters`.
- **Waktu & Durasi Syuruq via `adhan2` (Parameter Kemenag RI - 7 Hasta)**:
  - **Dilarang melakukan hardcode durasi Syuruq** (seperti menambah angka statis `+15` atau `+16` menit secara manual). Seluruh kalkulasi titik mulai, titik akhir, dan durasi Syuruq wajib dihitung secara astronomis oleh pustaka `adhan2` berdasarkan koordinat lokasi dan tanggal harian.
  - **Implementasi Parameter 7 Hasta ($+3.5^\circ$ di atas ufuk)**:
    - Waktu mulai Syuruq (`syuruqStartTime`) diambil langsung dari `prayerTimes.sunrise` (saat piringan atas matahari menyentuh ufuk).
    - Berdasarkan ketetapan hisab Kemenag RI, batas akhir waktu larangan salat Syuruq adalah ketika matahari naik setinggi satu tombak / **7 hasta**, yaitu pada sudut ketinggian matahari $+3^\circ 30'$ ($3.5^\circ$) di atas ufuk.
    - Hitung `syuruqEndTime` menggunakan instance `PrayerTimes` kedua dari `adhan2` pada `Coordinates` dan `DateComponents` yang sama dengan `CalculationParameters` khusus di mana sudut ketinggian matahari diatur ke `-3.5` (`fajrAngle = -3.5`, karena `adhan2` mendefinisikan sudut di bawah ufuk sebagai positif sehingga sudut $+3.5^\circ$ di atas ufuk bernilai negatif `-3.5`), lalu ambil properti `.fajr` sebagai `syuruqEndTime`.
    - Hitung durasi Syuruq secara dinamis dari selisih keduanya: `syuruqDuration = syuruqEndTime - syuruqStartTime`.
    - Model domain wajib mengekspos `syuruqStartTime`, `syuruqEndTime`, `syuruqDuration`, serta status boolean apakah waktu saat ini berada dalam interval `[syuruqStartTime, syuruqEndTime)`.
- **Domain Isolation**: Bungkus pemanggilan kelas `PrayerTimes`, `SunnahTimes`, dan `Qibla` di dalam repository atau use case pada layer `domain`/`data` agar tipe data dari library tidak bocor langsung ke komponen UI, Glance Widget, AOD Canvas, maupun Service.

## 3. Android Homescreen Widget (`Jetpack Glance`)
- **Widget Architecture**:
  - Implementasikan widget menggunakan subclass `GlanceAppWidget` dan receiver `GlanceAppWidgetReceiver`.
  - Gunakan komponen dan modifier dari package `androidx.glance.*` (seperti `GlanceModifier`, `Column`, `Row`, `Text`, `GlanceTheme`). **Dilarang mencampur composable dari `androidx.compose.material3` atau `androidx.compose.ui` ke dalam pohon UI Glance**, karena akan menyebabkan runtime crash saat translasi ke `RemoteViews`.
- **Scheduling & Updates**:
  - Jangan mengandalkan `updatePeriodMillis` pada `appwidget-provider` untuk transisi waktu salat (karena batas minimum sistem adalah 30 menit).
  - Gunakan **WorkManager** untuk sinkronisasi jadwal harian (saat pergantian hari/tengah malam) dan perubahan lokasi/zona waktu.
  - Gunakan **AlarmManager** (`setExactAndAllowWhileIdle` dengan izin `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`) untuk memicu pembaruan state widget, eksekusi audio, pembaruan pada `syuruqEndTime` hasil kalkulasi `adhan2`, dan pemicu `sesStart` tracking salat.
  - Tangani system broadcast penting (`ACTION_TIME_CHANGED`, `ACTION_TIMEZONE_CHANGED`, `ACTION_DATE_CHANGED`, `ACTION_BOOT_COMPLETED`) untuk menjadwalkan ulang seluruh alarm.
- **Responsiveness & Constraints**:
  - Dukung ukuran widget yang fleksibel (`SizeMode.Responsive` atau `SizeMode.Exact`) agar tampilan menyesuaikan grid launcher (misalnya ukuran compact 2x1/4x1 dan expanded 4x2).
  - Hindari operasi I/O berat atau network call langsung di dalam fungsi `provideGlance()`. Baca data yang sudah dihitung dari DataStore/Repository.
  - Sediakan fallback UI yang jelas (misalnya tombol aksi "Atur Lokasi") jika koordinat belum tersimpan.

## 4. Scheduled Notifications, TTS & Custom Audio Playback
- **Execution Architecture**:
  - Pemutaran TTS dan audio terjadwal harus dijalankan melalui **Foreground Service** khusus (misalnya `PrayerAlertService` dengan `foregroundServiceType="mediaPlayback"`) yang dipicu oleh `AlarmManager` + `BroadcastReceiver` agar eksekusi tidak dihentikan oleh sistem Android (Doze mode / background execution limits) saat layar mati.
  - Tampilkan notifikasi prioritas tinggi (`NotificationCompat.PRIORITY_HIGH` / `IMPORTANCE_HIGH`) dengan tombol aksi **"Hentikan"** (*Dismiss/Stop*) untuk menghentikan pemutaran TTS maupun audio kustom seketika.
- **Temporary Full Volume Management (`AudioManager`)**:
  - Sebelum memulai TTS atau audio, simpan level volume awal perangkat dari `AudioManager` (gunakan stream `AudioManager.STREAM_ALARM` atau `AudioManager.STREAM_MUSIC` dengan `AudioAttributes.USAGE_ALARM` agar tetap terdengar jelas).
  - Naikkan volume stream terkait ke level maksimum (`getStreamMaxVolume`) secara sementara.
  - **Wajib memulihkan volume awal** di dalam blok `finally` / callback selesai (`UtteranceProgressListener.onDone`/`onError` dan `MediaPlayer.setOnCompletionListener`/`setOnErrorListener`, atau saat pengguna menekan tombol Stop) agar volume perangkat tidak tertinggal di posisi maksimal.
  - Kelola `AudioFocusRequest` (`AUDIOFOCUS_GAIN_TRANSIENT`) selama pemutaran berlangsung dan lepaskan kembali setelah selesai.
- **5 Waktu Salat Wajib (Subuh, Dzuhur, Ashar, Maghrib, Isya)**:
  - Hitung sisa durasi menuju batas akhir waktu salat tersebut (Subuh berakhir di `syuruqStartTime` / `sunrise`, Dzuhur di `asr`, Ashar di `maghrib`, Maghrib di `isha`, Isya di `fajr` hari berikutnya) ke satuan jam.
  - Ucapkan kalimat TTS berbahasa Indonesia (`Locale("id", "ID")`):
    > `"Waktu sholat [Nama Sholat] telah tiba. Segera bersiap sebelum habis [Sisa Waktu] jam lagi."`
  - Segera setelah TTS selesai (`onDone`), periksa konfigurasi pengguna: jika pengguna telah menentukan file audio kustom (URI lokal via Storage Access Framework dengan `takePersistableUriPermission`), putar file tersebut menggunakan `MediaPlayer` hingga selesai sebelum memulihkan volume dan menghentikan service. Jika tidak ada file audio yang ditentukan, langsung pulihkan volume dan hentikan service.
- **Waktu Syuruq (`syuruqStartTime`)**:
  - Saat `syuruqStartTime` tiba, ucapkan kalimat TTS:
    > `"Memasuki Syuruq, waktu sholat subuh telah berakhir. Tunggu syuruq berakhir sebelum mengerjakan sholat sunah apapun."`
  - Segera setelah TTS selesai, bunyikan nada **bip** singkat (menggunakan `ToneGenerator` seperti `ToneGenerator.TONE_PROP_BEEP` atau aset audio bip lokal di `res/raw`), kemudian pulihkan volume dan hentikan service.
  - Jadwalkan pembaruan state widget tepat pada `syuruqEndTime` (hasil kalkulasi 7 hasta dari `adhan2`) untuk menandai bahwa waktu larangan salat telah berakhir.

## 5. Full Offline Architecture (100% Luring & Zero Static Dataset)
- **Zero Network Dependency**:
  - Aplikasi wajib beroperasi **100% luring (offline)** tanpa koneksi internet sama sekali.
  - **Dilarang menambahkan izin `android.permission.INTERNET` maupun `android.permission.ACCESS_NETWORK_STATE`** di dalam `AndroidManifest.xml`.
  - Dilarang menyertakan library HTTP client (seperti Ktor Client, Retrofit, OkHttp) atau SDK analitik/iklan berbasis jaringan.
- **Coordinate-Only Location & Built-in Google Maps Extraction Guide**:
  - Kalkulasi jadwal salat murni menggunakan pasangan koordinat **`latitude` dan `longitude`**. **Dilarang menyertakan dataset kota statis** di `assets/`/`res/raw` maupun menggunakan `android.location.Geocoder`.
  - Sediakan dua metode input koordinat luring:
    1. **Deteksi GPS Luring**: Melalui **`android.location.LocationManager`** bawaan Android (`GPS_PROVIDER` dan `PASSIVE_PROVIDER` / `getLastKnownLocation`) tanpa Google Play Services.
    2. **Input Manual Desimal**: Kolom input angka desimal `Latitude` (`-90.0..90.0`) dan `Longitude` (`-180.0..180.0`), ditambah dukungan *smart paste* (otomatis memecah format salinan Google Maps seperti `"-6.2088, 106.8456"` langsung ke masing-masing kolom saat pengguna menempelkan teks).
  - **Kompensasi UX (Panduan Langkah-demi-Langkah di Layar Pengaturan Lokasi)**:
    - Di dalam layar pengaturan koordinat, wajib disertakan kartu/dialog bantuan interaktif (dengan tab/bagian terpisah untuk **Google Maps Mobile** dan **Google Maps Desktop**) yang menjelaskan langkah rinci mendapatkan koordinat:
      - **Cara 1 — Google Maps Mobile (Android / iOS)**:
        1. Buka aplikasi **Google Maps**.
        2. Cari lokasi atau **tekan dan tahan (*tap & hold*)** titik rumah/masjid di peta hingga muncul **pin merah** (*Dropped pin*).
        3. Geser ke atas (*swipe up*) panel informasi **"Pin yang dipasang"** di bagian bawah layar (atau ketuk kolom pencarian di atas).
        4. Salin deretan angka desimal yang tertera di samping ikon pin lokasi (contoh: `3.595196, 98.672226`), lalu tempel (*paste*) di aplikasi ini.
      - **Cara 2 — Google Maps Desktop (Browser PC / Laptop)**:
        1. Buka `maps.google.com` di browser.
        2. Arahkan kursor ke titik lokasi yang diinginkan di peta, lalu **klik kanan** tepat pada titik tersebut.
        3. Pada menu konteks yang muncul, baris paling atas akan langsung menampilkan angka koordinat desimal (contoh: `3.59519, 98.67222`).
        4. **Klik kiri** angka koordinat tersebut untuk menyalinnya otomatis ke clipboard, lalu kirim/tempel ke kolom koordinat di aplikasi ini.
  - Pada tampilan widget dan pengaturan, tampilkan koordinat terformat (contoh: `3.5952° N, 98.6722° E`) atau label nama tempat kustom opsional yang diketik sendiri oleh pengguna.
- **Offline Text-to-Speech (TTS)**:
  - Konfigurasikan `TextToSpeech` agar memprioritaskan suara lokal yang tidak membutuhkan koneksi jaringan (`!voice.isNetworkConnectionRequired`).
  - Sediakan indikator status kesiapan TTS Bahasa Indonesia (`Locale("id", "ID")`) pada layar pengaturan aplikasi, beserta tombol pintasan (`TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`) jika pengguna perlu mengunduh data suara lokal dari pengaturan sistem Android, serta fallback nada bip (`ToneGenerator`) jika engine TTS belum terpasang di perangkat.

## 6. Optional Feature: High-Legibility Analog Always-On Display (AOD)
- **Activation & Lifecycle (Optional / Opt-in)**:
  - Fitur AOD bersifat **opsional** (dinonaktifkan secara default, dapat diaktifkan pengguna melalui pengaturan aplikasi atau dipicu secara manual / saat layar mati via `Intent.ACTION_SCREEN_OFF` ketika perangkat sedang terhubung daya atau sesuai preferensi pengguna).
  - Implementasikan menggunakan Activity khusus layar penuh (`AodClockActivity`) dengan konfigurasi `setShowWhenLocked(true)`, `setTurnScreenOn(true)`, `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`, mode imersif penuh (menyembunyikan status bar dan navigation bar), serta pengaturan kecerahan layar rendah (`screenBrightness` minimal/redup).
  - Sediakan mekanisme keluar yang mudah dan anti-salah sentuh (misalnya *double-tap* atau tombol geser/tahan untuk menutup AOD) agar tidak tertutup tidak sengaja di saku.
- **Strict Zero-Idle-Recomposition & Low-Power Rendering (`Compose Canvas`)**:
  - Gambar jam analog menggunakan **`androidx.compose.foundation.Canvas`** dengan latar belakang hitam pekat murni (`Color.Black` / `#000000`) dan elemen bergaya garis tipis/stroke kontras tinggi (`#FFFFFF` / `#E0E0E0`) untuk meminimalkan jumlah piksel OLED yang menyala.
  - **Kontrak Nol Rekomposisi & Nol Redraw di Luar Pergantian Menit**:
    - **Hilangkan jarum detik** sepenuhnya.
    - **Dilarang keras** menggunakan `rememberInfiniteTransition`, `Animatable`, `withFrameNanos`, `LaunchedEffect` dengan `delay()`, `TextClock`, atau membaca `Clock.System.now()` di dalam scope `@Composable`.
    - Satu-satunya sumber state waktu untuk layar AOD adalah sebuah `StateFlow<AodUiState>` (yang dibaca via `collectAsStateWithLifecycle()`) yang **hanya di-emit ulang ketika `BroadcastReceiver` menerima `Intent.ACTION_TIME_TICK`** dari sistem Android (tepat 1 kali setiap pergantian menit) atau saat Activity baru dibuka (`onResume`).
    - Gunakan parameter data immutable (`@Immutable data class AodUiState(...)`) pada composable `Canvas` agar Jetpack Compose melewati (*skip*) seluruh proses recomposition dan draw phase selama 59 detik di antara dua `ACTION_TIME_TICK`.
  - Pada `WindowManager.LayoutParams` di `AodClockActivity`, atur `preferredDisplayModeId` ke mode layar dengan **`refreshRate` terendah** yang didukung oleh `Display.getSupportedModes()` perangkat untuk menekan konsumsi daya panel saat idle.
  - Terapkan **OLED Burn-in Protection (Pixel Shifting)**: hitung offset pergeseran piksel ($\pm 8\text{dp}$ hingga $16\text{dp}$) secara deterministik berdasarkan nilai menit saat ini di dalam `AodUiState` (sehingga pergeseran terjadi bersamaan dengan `ACTION_TIME_TICK` tanpa memicu recomposition tambahan).
- **Prayer Window Arc Parser (Visualisasi Batas Habis Waktu Salat)**:
  - Pada lingkaran dial luar jam analog (12 jam), gambar **busur sektor (*arc segment*)** yang memetakan sisa waktu salat saat ini hingga waktu habisnya (*end time*).
  - **Aturan Pemetaan Sudut Busur (UI Parser)**:
    - Konversi waktu saat ini (`now`) dan waktu habis salat aktif (`currentPrayerEndTime`, termasuk `syuruqEndTime` hasil kalkulasi `adhan2` saat berada di interval Syuruq) ke sudut pada dial 12 jam: $\theta = ((\text{jam} \bmod 12) + \frac{\text{menit}}{60}) \times 30^\circ - 90^\circ$.
    - Gambar busur tersisa dari posisi sudut jarum jam saat ini hingga sudut `currentPrayerEndTime` dengan warna aksen yang jelas (misalnya hijau/amber untuk waktu salat normal, dan merah/oranye peringatan saat sisa waktu $< 30$ menit atau saat berada dalam interval larangan Syuruq).
    - Jika durasi sisa waktu melebihi 12 jam (kasus khusus pada waktu Isya menuju Subuh di lintang tertentu), batasi busur penuh $360^\circ$ hingga selisih waktu masuk ke rentang $\le 12$ jam, dan tampilkan label teks ringkas di bawah poros jam (contoh: `"Dzuhur • Habis 15:12 (-1j 40m)"` atau `"Syuruq • Selesai 06:18"`).

## 7. Optional Feature: Prayer Tracking Table & System-Wide Smart Modal UI
- **Activation & Permissions (Optional / Opt-in)**:
  - Fitur tabel tracking salat bersifat **opsional** (nonaktif secara default). Saat pengguna mengaktifkan fitur ini di pengaturan, arahkan pengguna untuk memberikan izin **`android.permission.SYSTEM_ALERT_WINDOW`** (*Display over other apps* via `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`) agar Smart Modal UI dapat muncul di tengah layar manapun meskipun aplikasi utama ditutup.
  - Hanya berlaku untuk 5 waktu salat wajib (`Subuh`, `Dzuhur`, `Ashar`, `Maghrib`, `Isya`), **tidak** untuk `Syuruq`.
- **Session Lifecycle (`sesStart` & Queue Prioritization)**:
  - **`sesStart`**: Dijadwalkan secara otomatis tepat **5 menit setelah waktu salat wajib masuk** (`prayerStartTime + 5 menit`) melalui `AlarmManager`.
  - **Deduplikasi & Antrean Tumpukan (`if sesStart duplicated!`)**:
    - Simpan setiap sesi ke dalam tabel database lokal (Room) dengan status `IN_PROGRESS` (belum selesai) atau `COMPLETED`.
    - Jika `sesStart` baru dipicu sementara masih terdapat sesi sebelumnya yang belum selesai (tertunda karena pengguna memilih `'belum'` atau menutup modal), **jangan timpa/hapus sesi lama**. Masukkan sesi baru ke antrean aktif dan **prioritaskan urutan eksekusi dari sesi terbaru ke terlama (`ORDER BY prayerStartTime DESC` / LIFO)**.
    - Setelah sesi terbaru selesai (`sesEnded`), jika masih ada sesi lama yang tertunda di antrean, langsung lanjutkan (atau jadwalkan ulang) ke sesi tertunda berikutnya.
- **System-Wide Smart Modal UI (`WindowManager` Overlay)**:
  - Tampilkan modal dialog mengambang di tengah layar melalui Foreground/Overlay Service (`PrayerTrackerOverlayService`) menggunakan `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY` dengan backdrop semi-transparan di sekelilingnya (`ComposeView` yang dibekali `LifecycleOwner` dan `SavedStateRegistryOwner` mandiri).
  - **Perilaku Close / Dismiss & Reschedule (+5 Menit)**:
    - Di tiap tahap pertanyaan manapun, apabila pengguna mengklik **area backdrop di luar modal**, menekan **tombol close (`X`) di pojok modal**, atau menekan tombol Back, tutup modal seketika dan **jadwalkan ulang (`AlarmManager`) otomatis 5 menit dari sekarang** untuk melanjutkan sesi tersebut.
  - **Persistence & Resume State (Kontekstual)**:
    - Setiap jawaban (`P0`, `P1`, `P2`) disimpan langsung ke database lokal saat tombol ditekan.
    - Saat sesi tertunda dibuka kembali 5 menit kemudian, modal wajib **langsung melompat ke pertanyaan terakhir yang belum dijawab** sekaligus menampilkan **ringkasan chip/label jawaban dari pertanyaan sebelumnya** di bagian atas modal (misalnya: *"Sholat Dzuhur • Sudah sholat (12:15) • Jamaah"*).
- **Alur Interaksi & Pengumpulan Parameter (`P0`, `P1`, `P2`)**:
  - Header modal selalu menampilkan konteks waktu salat yang sedang ditanyakan (contoh: `"Tracking Sholat Dzuhur"` atau tag `"[Tertunda] Sholat Subuh"`).
  - **Tahap 1 (Status & `P0`)**:
    - Pertanyaan wajib menyebutkan nama waktu salat yang dimaksud sistem:
      > **`"udah sholat [Nama Sholat] belum?"`** *(contoh: `"udah sholat Dzuhur belum?"`)*
    - Opsi: **`'udah'`** | **`'belum'`**
    - Aksi `'belum'`: Tutup modal dan *reschedule* otomatis **+5 menit**.
    - Aksi `'udah'`: Rekam **`P0`** (timestamp epoch/waktu tepat saat tombol `'udah'` diklik) ke database, lalu lanjut ke Tahap 2.
  - **Tahap 2 (Cara Pelaksanaan & `P1`)**:
    - Pertanyaan: **`"sendiri atau jamaah?"`**
    - Opsi: **`'sendiri'`** | **`'jamaah'`**
    - Aksi: Rekam **`P1`** (`SENDIRI` atau `JAMAAH`) ke database, lalu lanjut ke Tahap 3.
  - **Tahap 3 (Lokasi Pelaksanaan & `P2`)**:
    - Pertanyaan: **`"dimana?"`**
    - Opsi: **`'masjid'`** | **`'nggak dimasjid'`**
    - Aksi: Rekam **`P2`** (`MASJID` atau `NON_MASJID`) ke database, lalu lanjut ke Tahap 4.
  - **Tahap 4 (Konfirmasi & `sesEnded`)**:
    - Tampilkan pernyataan penutup: **`"oke."`**
    - Tutup modal secara otomatis (setelah jeda singkat $\approx 600\text{ms}$ atau langsung) dan jalankan mekanisme **`sesEnded`**.
- **Klasifikasi Kategori Pengerjaan (`sesEnded` - 5 Level & Penyimpanan Utuh)**:
  - **Wajib menyimpan seluruh data `P0`, `P1`, dan `P2` secara lengkap** untuk setiap sesi yang selesai, termasuk ketika sesi terdeteksi sebagai **`QODHO`** (alur pertanyaan tetap berjalan sampai `P2` sebelum `sesEnded`).
  - Saat `sesEnded` berjalan, sistem mengevaluasi `P0` terhadap batas akhir waktu salat (`prayerEndTime`) serta nilai `P1` dan `P2` untuk menentukan salah satu dari **5 level kategori** (diurutkan dari level kualitas tertinggi ke terendah):
    1. **Level 1 (`JAMAAH_MASJID`)**: `P0 <= prayerEndTime` AND `P1 == JAMAAH` AND `P2 == MASJID`
    2. **Level 2 (`JAMAAH_NON_MASJID`)**: `P0 <= prayerEndTime` AND `P1 == JAMAAH` AND `P2 == NON_MASJID`
    3. **Level 3 (`SENDIRI_MASJID`)**: `P0 <= prayerEndTime` AND `P1 == SENDIRI` AND `P2 == MASJID`
    4. **Level 4 (`SENDIRI_NON_MASJID`)**: `P0 <= prayerEndTime` AND `P1 == SENDIRI` AND `P2 == NON_MASJID`
    5. **Level 5 (`QODHO`)**: `P0 > prayerEndTime` (tombol `'udah'` diklik setelah waktu salat tersebut habis; **data `P0`, `P1`, dan `P2` tetap disimpan utuh di kolom tabel** di samping status kategori `QODHO`).
  - Simpan hasil akhir (`tanggal`, `nama_sholat`, `jadwal_masuk`, `jadwal_habis`, `timestamp_P0`, `P1`, `P2`, `kategori_level`, `status = COMPLETED`) ke dalam **Tabel Tracking Salat** di Room Database.
  - Sediakan layar UI tabel tracking di dalam aplikasi utama untuk melihat riwayat harian/mingguan lengkap dengan badge level kategori serta detail `P0`, `P1`, dan `P2`.

## 8. Coding Standards & Idioms
- **Null Safety**: Manfaatkan sistem tipe nullable Kotlin secara ketat. Dilarang keras menggunakan operator `!!` (non-null asserted) kecuali dalam unit test. Gunakan `?.`, `?:`, atau `requireNotNull()` dengan pesan error yang jelas.
- **Immutability**: Prioritaskan `val` daripada `var`, dan gunakan koleksi read-only (`List`, `Map`, `Set`) secara default.
- **Idiomatic Kotlin**: Gunakan `data class`, `sealed class`/`sealed interface` untuk UI/Widget/AOD/Modal state, extension functions, serta scope functions (`let`, `apply`, `also`, `run`) secara proporsional tanpa mengorbankan keterbacaan kode.

## 9. Architecture & Project Structure
- **Pattern**: Terapkan **Clean Architecture** dengan pemisahan layer yang tegas:
  - `domain`: Pure Kotlin (tanpa dependensi framework Android/UI/Glance). Berisi Model jadwal salat, kalkulasi waktu & durasi Syuruq 7 hasta, parser input koordinat desimal, kalkulasi sudut busur AOD (`PrayerArcState`), evaluator 5 level kategori tracking salat (`PrayerTrackingEvaluator`), Repository Interface, dan Use Case.
  - `data`: Implementasi Repository, kalkulasi `adhan2` (jadwal salat & parameter Syuruq $-3.5^\circ$), Room Database (Entity & DAO untuk sesi dan tabel tracking salat), DataStore preferences, `LocationManager` wrapper, dan Worker/Alarm scheduler.
  - `presentation`:
    - `ui/`: Layar Compose untuk input koordinat (GPS luring / input manual + panduan Google Maps Mobile & Desktop), pemilihan file audio kustom (SAF picker), pengaturan fitur opsional (AOD & Tracking), tampilan tabel riwayat tracking salat, dan uji coba suara TTS/Alarm.
    - `overlay/`: `PrayerTrackerOverlayService` (`WindowManager` overlay host dengan Compose lifecycle) dan komponen Smart Modal UI.
    - `aod/`: `AodClockActivity`, `ScreenOffReceiver`, dan komponen `Canvas` jam analog + busur indikator waktu habis salat.
    - `widget/`: Implementasi `GlanceAppWidget`, `GlanceAppWidgetReceiver`, komponen UI Glance, dan `ActionCallback`.
    - `service/`: `PrayerAlertService` (Foreground Service untuk orchestrasi `AudioManager`, `TextToSpeech`, `MediaPlayer`, dan `ToneGenerator`) serta `AlarmReceiver`.
- **Dependency Injection**: Gunakan framework DI berbasis Kotlin seperti **Koin** atau **Dagger/Hilt** (pastikan injeksi ke `GlanceAppWidgetReceiver`, `BroadcastReceiver`, `Service`, dan `CoroutineWorker` ditangani dengan benar).

## 10. Testing & Quality Assurance
- **Unit Testing**: Gunakan `kotlin.test` atau **JUnit** + **MockK** untuk memverifikasi:
  - Akurasi kalkulasi `PrayerTimes` parameter Kemenag RI dan kalkulasi `syuruqStartTime`, `syuruqEndTime` (sudut $-3.5^\circ$ / 7 hasta via `adhan2`), serta `syuruqDuration`.
  - Konversi zona waktu, kalkulasi sisa jam menuju akhir waktu salat, dan parser validasi input koordinat desimal.
  - Perhitungan sudut mulai (`startAngle`) dan sudut sapuan (`sweepAngle`) untuk busur indikator AOD.
  - Evaluator 5 level kategori tracking salat (`JAMAAH_MASJID` hingga `QODHO`), memastikan `P0`, `P1`, dan `P2` tetap tersimpan pada kasus `QODHO`, serta prioritas antrean LIFO saat terjadi tumpukan `sesStart`.
- **Coroutines Testing**: Wajib menggunakan `kotlinx-coroutines-test` (`runTest`) untuk menguji kode asinkron, DAO Room, dan Worker.
- **Linter**: Pastikan kode mematuhi aturan **ktlint** atau **detekt** sebelum menyelesaikan tugas.

## 11. Agent Workflow Rules
- **Verifikasi Kompilasi**: Setiap perubahan kode harus dipastikan dapat dikompilasi tanpa error menggunakan `./gradlew assembleDebug` atau `./gradlew testDebugUnitTest`.
- **Minimal Diff**: Ubah hanya file yang relevan dengan instruksi. Jangan melakukan refactoring besar-besaran di luar cakupan tugas tanpa diminta.
- **Tanpa Asumsi Versi**: Periksa `gradle/libs.versions.toml` terlebih dahulu sebelum menambahkan atau memperbarui versi dependensi (pastikan menggunakan artefak `adhan2`, `androidx.glance:glance-appwidget`, dan `androidx.room`).