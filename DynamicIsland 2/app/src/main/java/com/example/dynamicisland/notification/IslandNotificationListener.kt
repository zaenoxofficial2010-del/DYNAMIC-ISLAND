package com.example.dynamicisland.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.dynamicisland.DynamicIslandService

/**
 * Standard Android NotificationListenerService. Only receives notifications after
 * the user explicitly grants Notification Access in system settings — this is
 * the only legitimate way for a normal app to observe other apps' notifications.
 *
 * We don't store or forward the full notification; we just relay a lightweight
 * event to the running service if it's alive. We never duplicate an Android
 * notification of our own from this.
 */
class IslandNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Ignore our own foreground-service notification.
        if (sbn.packageName == packageName) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: return
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val appName = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(sbn.packageName, 0)
            ).toString()
        }.getOrDefault(sbn.packageName)

        DynamicIslandService.instance?.onExternalNotification(appName, title, text)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // No action needed; the Island's own notification event auto-collapses.
    }

    override fun onListenerConnected() {
        DynamicIslandService.instance?.onNotificationListenerConnected(this)
    }
}
