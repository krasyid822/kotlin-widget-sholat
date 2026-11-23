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
- Build APK Release (unsigned, kecuali sudah dikonfigurasi signingConfigs): 
```code
./gradlew assembleRelease
```
- Hasil APK ada di `app/build/outputs/apk/release/`
- Jalankan unit test: 
```code
./gradlew testDebugUnitTest
```