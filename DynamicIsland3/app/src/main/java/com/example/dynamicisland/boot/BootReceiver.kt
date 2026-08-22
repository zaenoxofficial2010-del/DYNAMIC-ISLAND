package com.example.dynamicisland.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.dynamicisland.DynamicIslandService
import com.example.dynamicisland.permissions.PermissionManager
import com.example.dynamicisland.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context)
                val shouldStart = settings.startOnBoot.first() && settings.islandEnabled.first()
                if (shouldStart && PermissionManager.hasOverlayPermission(context)) {
                    val serviceIntent = Intent(context, DynamicIslandService::class.java)
                    ContextCompat.startForegroundService(context, serviceIntent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
