package com.widgetsholat.app.presentation.aod

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.widgetsholat.app.domain.model.AodClockState
import com.widgetsholat.app.domain.repository.PrayerRepository
import com.widgetsholat.app.domain.repository.SettingsRepository
import com.widgetsholat.app.domain.util.PrayerArcCalculator
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.android.ext.android.inject

class AodClockActivity : ComponentActivity() {

    private val settingsRepository: SettingsRepository by inject()
    private val prayerRepository: PrayerRepository by inject()
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val uiStateFlow = MutableStateFlow(
        AodClockState(
            hourAngle = -90f,
            minuteAngle = -90f,
            shiftXDp = 0f,
            shiftYDp = 0f,
            timeText = "--:--",
            arcState = null
        )
    )

    private val timeTickReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_TIME_TICK -> refreshState()
                Intent.ACTION_USER_PRESENT -> {
                    isActive = false
                    finish()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableShowOnLockScreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        applyLowPowerDisplayConfig()
        hideSystemBars()
        applySystemGestureExclusion()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Intentionally ignored so system back gesture cannot exit AOD
                }
            }
        )

        setContent {
            val state by uiStateFlow.collectAsStateWithLifecycle()
            AodClockScreen(state = state)
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean = true

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        enableShowOnLockScreen()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        applyLowPowerDisplayConfig()
        hideSystemBars()
        refreshState()
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        currentInstance = WeakReference(this)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(ScreenOffReceiver.NOTIFICATION_ID_LAUNCH)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            timeTickReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        refreshState()
    }

    override fun onPause() {
        isActive = false
        lastPausedAtMs = if (isFinishing) 0L else SystemClock.elapsedRealtime()
        if (currentInstance?.get() === this) {
            currentInstance = null
        }
        runCatching { unregisterReceiver(timeTickReceiver) }
        super.onPause()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        isActive = false
        finish()
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }

    private fun refreshState() {
        activityScope.launch {
            val now = kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis())
            val tz = TimeZone.currentSystemDefault()
            val local = now.toLocalDateTime(tz)

            val coords = settingsRepository.settingsFlow.first().coordinates
            val schedule = coords?.let { prayerRepository.getScheduleForDate(local.date, it) }
            val activeWindow = schedule?.resolveActiveWindow(now)
            val arcState = activeWindow?.let { PrayerArcCalculator.calculateArc(now, it, tz) }
            val syuruqArc = schedule?.let {
                PrayerArcCalculator.calculateSyuruqDurationArc(it.syuruqStartTime, it.syuruqEndTime, tz)
            }

            val hourAngle = PrayerArcCalculator.timeToDialAngle(local.hour, local.minute)
            val minuteAngle = local.minute * 6f - 90f

            // Deterministic OLED burn-in pixel shift based on current minute
            val shiftXDp = ((local.minute % 7) - 3) * 4f
            val shiftYDp = (((local.minute / 7) % 5) - 2) * 4f

            uiStateFlow.value = AodClockState(
                hourAngle = hourAngle,
                minuteAngle = minuteAngle,
                shiftXDp = shiftXDp,
                shiftYDp = shiftYDp,
                timeText = "%02d:%02d".format(local.hour, local.minute),
                dayText = formatIndonesianDay(local.dayOfWeek),
                batteryPercent = readBatteryPercent(),
                arcState = arcState,
                syuruqArc = syuruqArc
            )
        }
    }

    private fun formatIndonesianDay(dayOfWeek: DayOfWeek): String = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "Senin"
        DayOfWeek.TUESDAY -> "Selasa"
        DayOfWeek.WEDNESDAY -> "Rabu"
        DayOfWeek.THURSDAY -> "Kamis"
        DayOfWeek.FRIDAY -> "Jumat"
        DayOfWeek.SATURDAY -> "Sabtu"
        DayOfWeek.SUNDAY -> "Minggu"
        else -> ""
    }

    private fun readBatteryPercent(): Int? {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun applySystemGestureExclusion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.decorView.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
                view.systemGestureExclusionRects = listOf(Rect(0, 0, view.width, view.height))
            }
        }
    }

    private fun enableShowOnLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun applyLowPowerDisplayConfig() {
        val params = window.attributes
        params.screenBrightness = 0.08f
        val lowestMode = display?.supportedModes?.minByOrNull { it.refreshRate }
        if (lowestMode != null) {
            params.preferredDisplayModeId = lowestMode.modeId
        }
        window.attributes = params
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    companion object {
        @Volatile
        var isActive: Boolean = false
            private set

        @Volatile
        private var lastPausedAtMs: Long = 0L

        @Volatile
        private var currentInstance: WeakReference<AodClockActivity>? = null

        fun wasRecentlyActive(windowMs: Long = 1_500L): Boolean =
            lastPausedAtMs > 0L && (SystemClock.elapsedRealtime() - lastPausedAtMs) < windowMs

        fun exitFromPowerButton(context: Context) {
            isActive = false
            val activity = currentInstance?.get()
            currentInstance = null
            activity?.runOnUiThread { activity.finish() }

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            @Suppress("DEPRECATION")
            powerManager?.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "WidgetSholat:AodPowerExit"
            )?.acquire(1_000L)
        }
    }
}
