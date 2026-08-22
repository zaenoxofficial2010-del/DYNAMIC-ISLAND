package com.example.dynamicisland

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.example.dynamicisland.events.CallEventSource
import com.example.dynamicisland.events.ChargingEventSource
import com.example.dynamicisland.events.EventManager
import com.example.dynamicisland.events.IslandEvent
import com.example.dynamicisland.events.MediaEventSource
import com.example.dynamicisland.events.TimerEventSource
import com.example.dynamicisland.notification.IslandNotificationListener
import com.example.dynamicisland.overlay.IslandOverlayManager
import kotlinx.coroutines.launch

/**
 * The Island's actual runtime. This is a Service — NOT tied to MainActivity.
 * Starting it adds ONE WindowManager overlay; it keeps running (and the overlay
 * keeps showing) after MainActivity is closed, until the user explicitly stops it.
 */
class DynamicIslandService : LifecycleService() {

    companion object {
        const val CHANNEL_ID = "island_service_channel"
        const val NOTIFICATION_ID = 1001 // fixed ID — never create a second notification
        const val ACTION_STOP = "com.example.dynamicisland.action.STOP"

        /** Lets the NotificationListenerService relay events without a full IPC layer. */
        var instance: DynamicIslandService? = null
            private set
    }

    private lateinit var eventManager: EventManager
    private lateinit var overlayManager: IslandOverlayManager
    private lateinit var timerSource: TimerEventSource

    private var mediaSource: MediaEventSource? = null
    private var callSource: CallEventSource? = null
    private var chargingSource: ChargingEventSource? = null

    override fun onCreate() {
        super.onCreate()
        instance = this

        eventManager = EventManager(lifecycleScope)
        overlayManager = IslandOverlayManager(this, eventManager)
        timerSource = TimerEventSource(lifecycleScope, eventManager)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        if (!overlayManager.isAdded) {
            overlayManager.addOverlay(onLongPress = { openSettings() })
        }

        chargingSource = ChargingEventSource(this, eventManager).also { it.start() }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            callSource = CallEventSource(this, lifecycleScope, eventManager).also { it.start() }
        }

        mediaSource = MediaEventSource(
            this,
            ComponentName(this, IslandNotificationListener::class.java),
            eventManager
        ).also { it.start() }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent.action == ACTION_STOP) {
            stopSelfCompletely()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null // no client binding needed; control-center talks via startService/stopService
    }

    override fun onDestroy() {
        overlayManager.removeOverlay()
        mediaSource?.stop()
        callSource?.stop()
        chargingSource?.stop()
        timerSource.stop()
        instance = null
        super.onDestroy()
    }

    fun startTimer(label: String, totalSeconds: Int) = timerSource.start(label, totalSeconds)
    fun stopTimer() = timerSource.stop()

    fun onExternalNotification(appName: String, title: String, text: String) {
        eventManager.push("notification", IslandEvent.Notification(appName, title, text))
    }

    fun onNotificationListenerConnected(listener: IslandNotificationListener) {
        // Hook point if the listener needs to be handed back to sources that require it.
    }

    private fun stopSelfCompletely() {
        overlayManager.removeOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun openSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("open_settings", true)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    /** Exactly one notification, fixed ID, never recreated per Island event. */
    private fun buildNotification(): android.app.Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, DynamicIslandService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.presence_online)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }
}
