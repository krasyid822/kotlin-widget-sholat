package com.widgetsholat.app.presentation.overlay

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.widgetsholat.app.PrayerApp
import com.widgetsholat.app.data.scheduler.PrayerAlarmScheduler
import com.widgetsholat.app.domain.model.PrayerTrackingSession
import com.widgetsholat.app.domain.model.TrackingExecutionMode
import com.widgetsholat.app.domain.model.TrackingLocation
import com.widgetsholat.app.domain.repository.TrackingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.koin.android.ext.android.inject

class PrayerTrackerOverlayService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val trackingRepository: TrackingRepository by inject()
    private val alarmScheduler: PrayerAlarmScheduler by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val activeSessionFlow = MutableStateFlow<PrayerTrackingSession?>(null)
    private val showDoneStepFlow = MutableStateFlow(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, -1L) ?: -1L
        serviceScope.launch {
            val session = if (sessionId > 0L) {
                trackingRepository.getSessionById(sessionId)
            } else {
                trackingRepository.getLatestPendingSession()
            }
            if (session == null) {
                stopSelf()
                return@launch
            }
            showDoneStepFlow.value = false
            activeSessionFlow.value = session
            showOverlay()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (overlayView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@PrayerTrackerOverlayService)
            setViewTreeSavedStateRegistryOwner(this@PrayerTrackerOverlayService)
            setContent {
                val session by activeSessionFlow.collectAsStateWithLifecycle()
                val isDone by showDoneStepFlow.collectAsStateWithLifecycle()
                session?.let { current ->
                    SmartTrackerModal(
                        session = current,
                        isDoneStep = isDone,
                        onDismissOrLater = { dismissAndReschedule(current.id) },
                        onSudah = { handleP0(current.id) },
                        onSelectMode = { mode -> handleP1(current.id, mode) },
                        onSelectLocation = { loc -> handleP2(current.id, loc) }
                    )
                }
            }
        }
        overlayView = view
        wm.addView(view, params)
    }

    private fun dismissAndReschedule(sessionId: Long) {
        alarmScheduler.scheduleTrackingReschedule(sessionId, delayMinutes = 5L)
        removeOverlayAndStop()
    }

    private fun handleP0(sessionId: Long) {
        serviceScope.launch {
            trackingRepository.recordP0(sessionId, kotlinx.datetime.Instant.fromEpochMilliseconds(System.currentTimeMillis()))
            activeSessionFlow.value = trackingRepository.getSessionById(sessionId)
        }
    }

    private fun handleP1(sessionId: Long, mode: TrackingExecutionMode) {
        serviceScope.launch {
            trackingRepository.recordP1(sessionId, mode)
            activeSessionFlow.value = trackingRepository.getSessionById(sessionId)
        }
    }

    private fun handleP2(sessionId: Long, location: TrackingLocation) {
        serviceScope.launch {
            trackingRepository.recordP2AndComplete(sessionId, location)
            showDoneStepFlow.value = true
            delay(600L)
            val nextPending = trackingRepository.getLatestPendingSession()
            if (nextPending != null) {
                showDoneStepFlow.value = false
                activeSessionFlow.value = nextPending
            } else {
                removeOverlayAndStop()
            }
        }
    }

    private fun removeOverlayAndStop() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        overlayView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        overlayView?.let { view ->
            runCatching { windowManager?.removeView(view) }
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, PrayerApp.CHANNEL_TRACKER_OVERLAY)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("Tracking Sholat")
            .setContentText("Menunggu konfirmasi tracking sholat")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    companion object {
        const val EXTRA_SESSION_ID = "extra_session_id"
        private const val NOTIFICATION_ID = 2001
    }
}
