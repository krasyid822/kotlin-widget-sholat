package com.widgetsholat.app.presentation.ui

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.widgetsholat.app.data.location.OfflineLocationProvider
import com.widgetsholat.app.data.scheduler.PrayerAlarmScheduler
import com.widgetsholat.app.domain.model.CoordinatesInput
import com.widgetsholat.app.domain.model.PrayerName
import com.widgetsholat.app.domain.repository.AppSettings
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.util.CoordinateParser
import com.widgetsholat.app.presentation.aod.AodClockActivity
import com.widgetsholat.app.presentation.aod.AodMonitorService
import com.widgetsholat.app.presentation.service.PrayerAlertService
import com.widgetsholat.app.presentation.widget.WidgetUpdater
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    settingsRepository: SettingsRepository = koinInject(),
    prayerRepository: PrayerRepository = koinInject(),
    locationProvider: OfflineLocationProvider = koinInject(),
    alarmScheduler: PrayerAlarmScheduler = koinInject()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    var latText by remember(settings.coordinates) {
        mutableStateOf(settings.coordinates?.latitude?.toString().orEmpty())
    }
    var lonText by remember(settings.coordinates) {
        mutableStateOf(settings.coordinates?.longitude?.toString().orEmpty())
    }
    var labelText by remember(settings.coordinates) {
        mutableStateOf(settings.coordinates?.label.orEmpty())
    }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showMapsGuide by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasFullScreenIntentPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                    ?.canUseFullScreenIntent() == true
        )
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
                hasNotificationPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                hasFullScreenIntentPermission =
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
                        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
                            ?.canUseFullScreenIntent() == true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result handled by system; AOD can still use overlay or full-screen intent */ }

    val gpsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        val ok = granted[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            granted[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            val pair = locationProvider.getLastKnownCoordinates()
            if (pair != null) {
                latText = pair.first.toString()
                lonText = pair.second.toString()
                statusMessage = "Koordinat GPS luring ditemukan."
            } else {
                statusMessage = "GPS aktif, namun belum ada lokasi terakhir."
            }
        } else {
            statusMessage = "Izin lokasi ditolak."
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            scope.launch { settingsRepository.saveCustomAudioUri(uri.toString()) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Pengaturan Luring",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = latText,
            onValueChange = { input ->
                val parsed = CoordinateParser.parseSmartPaste(input)
                if (parsed != null) {
                    latText = parsed.first.toString()
                    lonText = parsed.second.toString()
                } else {
                    latText = input
                }
            },
            label = { Text("Latitude (atau tempel 'lat, lon')") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = lonText,
            onValueChange = { lonText = it },
            label = { Text("Longitude") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = labelText,
            onValueChange = { labelText = it },
            label = { Text("Label Tempat (opsional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val lat = latText.trim().toDoubleOrNull()
                    val lon = lonText.trim().toDoubleOrNull()
                    if (lat != null && lon != null && CoordinateParser.isValid(lat, lon)) {
                        val coords = CoordinatesInput(lat, lon, labelText.trim())
                        scope.launch {
                            settingsRepository.saveCoordinates(coords)
                            val today = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
                                .toLocalDateTime(TimeZone.currentSystemDefault()).date
                            val schedule = prayerRepository.getScheduleForDate(today, coords)
                            alarmScheduler.scheduleDailyAlarms(schedule, settings.trackingEnabled)
                            WidgetUpdater.updateAll(context)
                            statusMessage = "Koordinat disimpan & jadwal diperbarui."
                        }
                    } else {
                        statusMessage = "Koordinat tidak valid."
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Simpan Koordinat") }

            OutlinedButton(
                onClick = {
                    gpsPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                modifier = Modifier.weight(1f)
            ) { Text("GPS Luring") }
        }

        TextButton(onClick = { showMapsGuide = true }) {
            Text("Cara ambil koordinat dari Google Maps")
        }

        statusMessage?.let { msg ->
            Text(text = msg, color = MaterialTheme.colorScheme.primary)
        }

        HorizontalDivider()

        Text(text = "Audio & TTS Luring", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = { audioPickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.weight(1f)
            ) { Text("Pilih Audio Kustom") }

            OutlinedButton(
                onClick = {
                    val intent = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Data Suara TTS") }
        }
        Button(
            onClick = {
                val intent = Intent(context, PrayerAlertService::class.java).apply {
                    putExtra(PrayerAlertService.EXTRA_PRAYER_NAME, PrayerName.DZUHUR.name)
                    putExtra(PrayerAlertService.EXTRA_REMAINING_HOURS, 2L)
                }
                ContextCompat.startForegroundService(context, intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Uji Coba Pengingat TTS + Audio") }

        HorizontalDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Always-On Display (AOD) Analog", fontWeight = FontWeight.Medium)
                Text("Layar hitam murni dengan busur batas waktu sholat", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = settings.aodEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !hasNotificationPermission
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        AodMonitorService.start(context)
                    } else {
                        AodMonitorService.stop(context)
                    }
                    scope.launch { settingsRepository.setAodEnabled(enabled) }
                }
            )
        }
        if (settings.aodEnabled) {
            if (hasOverlayPermission && hasNotificationPermission && hasFullScreenIntentPermission) {
                Text(
                    text = "Semua izin AOD aktif.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!hasOverlayPermission) {
                        OutlinedButton(
                            onClick = {
                                val overlayIntent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(overlayIntent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Izinkan Tampil di Atas Aplikasi") }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        !hasFullScreenIntentPermission
                    ) {
                        OutlinedButton(
                            onClick = {
                                val fsiIntent = Intent(
                                    Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                    Uri.parse("package:${context.packageName}")
                                )
                                runCatching { context.startActivity(fsiIntent) }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Izinkan Notifikasi Layar Penuh") }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !hasNotificationPermission
                    ) {
                        OutlinedButton(
                            onClick = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Izinkan Notifikasi") }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = { context.startActivity(Intent(context, AodClockActivity::class.java)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Buka Preview AOD") }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tracking Sholat & Smart Modal", fontWeight = FontWeight.Medium)
                Text("Tampilkan modal +5 menit setelah adzan", style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = settings.trackingEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !Settings.canDrawOverlays(context)) {
                        val overlayIntent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(overlayIntent)
                    }
                    scope.launch { settingsRepository.setTrackingEnabled(enabled) }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showMapsGuide) {
        GoogleMapsGuideDialog(onDismiss = { showMapsGuide = false })
    }
}

@Composable
private fun GoogleMapsGuideDialog(onDismiss: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Panduan Koordinat Google Maps") },
        text = {
            Column {
                TabRow(selectedTabIndex = tab) {
                    Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Mobile") })
                    Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Desktop") })
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (tab == 0) {
                    Text(
                        "1. Buka aplikasi Google Maps.\n" +
                            "2. Tekan dan tahan (tap & hold) titik lokasi hingga muncul pin merah.\n" +
                            "3. Geser ke atas panel 'Pin yang dipasang'.\n" +
                            "4. Salin angka desimal (contoh: 3.595196, 98.672226) lalu tempel ke kolom Latitude."
                    )
                } else {
                    Text(
                        "1. Buka maps.google.com di browser.\n" +
                            "2. Klik kanan tepat pada titik lokasi di peta.\n" +
                            "3. Klik kiri angka koordinat di baris paling atas menu konteks untuk menyalin.\n" +
                            "4. Tempel ke kolom Latitude di aplikasi ini."
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Tutup") }
        }
    )
}
