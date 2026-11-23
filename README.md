<img width="72" height="160" alt="image" src="https://github.com/user-attachments/assets/22c8105f-2020-4da8-a34e-aa62a07bd34d" /><img width="72" height="160" alt="image" src="https://github.com/user-attachments/assets/fbed4fd2-9305-4fe4-a430-83e4a549bab7" />

## Download kebutuhan Gradle
```bash
/home/rasyidk/.gradle/wrapper/dists/gradle-9.8.2-bin/8wepis63pzuhrlfaiy04bagwu/gradle-9.8.2/bin/gradle testDebugUnitTest assembleDebug --no-daemon running
```

## Build

Jalankan perintah Gradle berikut dari folder root proyek (/home/rasyidk/vscode/kotlin-widget-sholat):

- Build APK Debug:
```code
./gradlew assembleDebug
```
- Hasil APK ada di `app/build/outputs/apk/debug/app-debug.apk`
- Langsung build & install ke HP/Emulator (via ADB):
```code
./gradlew installDebug
```
- Build APK Release (unsigned jika `keystore.properties` belum diisi): 
```code
./gradlew assembleRelease
```
- Hasil APK ada di `app/build/outputs/apk/release/`
- Konfigurasi Signing Release:
  1. Buat keystore baru (jika belum ada):
     ```bash
     keytool -genkeypair -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias widgetsholat
     ```
  2. Salin `keystore.properties.example` menjadi `keystore.properties` dan sesuaikan nilainya (atau set environment variable `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`).
  3. Jalankan `./gradlew assembleRelease` untuk menghasilkan APK yang sudah ditandatangani.
- Jalankan unit test: 
```code
./gradlew testDebugUnitTest
```

## Keystore

What is your first and last name?
  [Unknown]:  Rasyid Kurniawan
What is the name of your organizational unit?
  [Unknown]:  krasyid822
What is the name of your organization?
  [Unknown]:  krasyid822
What is the name of your City or Locality?
  [Unknown]:  Medan
What is the name of your State or Province?
  [Unknown]:  Sumatera Utara
What is the two-letter country code for this unit?
  [Unknown]:  ID
Is CN=Rasyid Kurniawan, OU=krasyid822, O=krasyid822, L=Medan, ST=Sumatera Utara, C=ID correct?
  [no]:  yes
