package com.example.dynamicisland

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.dynamicisland.permissions.PermissionManager
import com.example.dynamicisland.settings.SettingsRepository
import com.example.dynamicisland.ui.theme.DynamicIslandTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var settings: SettingsRepository

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshAndMaybeStart.value++ }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val refreshAndMaybeStart = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = SettingsRepository(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !PermissionManager.hasPostNotificationsPermission(this)
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            DynamicIslandTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ControlCenterScreen(
                        settings = settings,
                        refreshTrigger = refreshAndMaybeStart.value,
                        onRequestOverlayPermission = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            overlayPermissionLauncher.launch(intent)
                        },
                        onRequestNotificationAccess = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onToggleIsland = { enable -> toggleIsland(enable) },
                        onRestartService = {
                            ContextCompat.startForegroundService(
                                this, Intent(this, DynamicIslandService::class.java)
                            )
                        },
                        onStopService = {
                            val intent = Intent(this, DynamicIslandService::class.java)
                                .setAction(DynamicIslandService.ACTION_STOP)
                            startService(intent)
                        }
                    )
                }
            }
        }
    }

    private fun toggleIsland(enable: Boolean) {
        lifecycleScope.launch { settings.setIslandEnabled(enable) }
        if (enable) {
            if (!PermissionManager.hasOverlayPermission(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
                return
            }
            ContextCompat.startForegroundService(this, Intent(this, DynamicIslandService::class.java))
        } else {
            val intent = Intent(this, DynamicIslandService::class.java)
                .setAction(DynamicIslandService.ACTION_STOP)
            startService(intent)
        }
    }
}

@Composable
fun ControlCenterScreen(
    settings: SettingsRepository,
    refreshTrigger: Int,
    onRequestOverlayPermission: () -> Unit,
    onRequestNotificationAccess: () -> Unit,
    onToggleIsland: (Boolean) -> Unit,
    onRestartService: () -> Unit,
    onStopService: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val islandEnabled by settings.islandEnabled.collectAsState(initial = false)
    val startOnBoot by settings.startOnBoot.collectAsState(initial = false)
    val eventMusic by settings.eventMusicEnabled.collectAsState(initial = true)
    val eventCalls by settings.eventCallsEnabled.collectAsState(initial = true)
    val eventNotifs by settings.eventNotificationsEnabled.collectAsState(initial = true)
    val eventCharging by settings.eventChargingEnabled.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    val overlayGranted = remember(refreshTrigger) { PermissionManager.hasOverlayPermission(context) }
    val notifAccessGranted = remember(refreshTrigger) { PermissionManager.hasNotificationAccess(context) }
    val serviceRunning = DynamicIslandService.instance != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Dynamic Island", style = MaterialTheme.typography.headlineMedium)

        Card {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Dynamic Island", style = MaterialTheme.typography.titleMedium)
                Switch(checked = islandEnabled, onCheckedChange = onToggleIsland)
            }
        }

        StatusRow("Overlay", overlayGranted, if (!overlayGranted) onRequestOverlayPermission else null)
        StatusRow("Notification Access", notifAccessGranted, if (!notifAccessGranted) onRequestNotificationAccess else null)
        StatusRow("Service", serviceRunning, null, positiveLabel = "Running", negativeLabel = "Stopped")

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Events", style = MaterialTheme.typography.titleMedium)
                SettingSwitchRow("Music", eventMusic) { scope.launch { settings.setEventEnabled("music", it) } }
                SettingSwitchRow("Calls", eventCalls) { scope.launch { settings.setEventEnabled("calls", it) } }
                SettingSwitchRow("Notifications", eventNotifs) { scope.launch { settings.setEventEnabled("notifications", it) } }
                SettingSwitchRow("Charging", eventCharging) { scope.launch { settings.setEventEnabled("charging", it) } }
            }
        }

        Card {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("General", style = MaterialTheme.typography.titleMedium)
                SettingSwitchRow("Start on boot", startOnBoot) { scope.launch { settings.setStartOnBoot(it) } }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onRestartService) { Text("Restart Service") }
            OutlinedButton(onClick = onStopService) { Text("Stop Service") }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    granted: Boolean,
    onFix: (() -> Unit)?,
    positiveLabel: String = "Enabled",
    negativeLabel: String = "Not granted"
) {
    Card {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(label, style = MaterialTheme.typography.titleMedium)
                Text(if (granted) positiveLabel else negativeLabel, style = MaterialTheme.typography.bodySmall)
            }
            if (!granted && onFix != null) {
                TextButton(onClick = onFix) { Text("Fix") }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
