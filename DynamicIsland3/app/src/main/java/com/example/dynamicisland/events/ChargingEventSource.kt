package com.example.dynamicisland.events

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

/** Listens to real ACTION_BATTERY_CHANGED / ACTION_POWER_CONNECTED broadcasts. */
class ChargingEventSource(
    private val context: Context,
    private val eventManager: EventManager
) {
    companion object {
        private const val KEY = "charging"
    }

    private var wasCharging = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val percent = if (level >= 0) (level * 100 / scale) else 0
                    wasCharging = true
                    eventManager.push(KEY, IslandEvent.Charging(isFull = false, percent = percent))
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    wasCharging = false
                    eventManager.clear(KEY)
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
                    val percent = if (level >= 0) (level * 100 / scale) else 0
                    if (wasCharging && percent >= 100 && status == BatteryManager.BATTERY_STATUS_FULL) {
                        eventManager.push(KEY, IslandEvent.Charging(isFull = true, percent = 100))
                    }
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun stop() {
        runCatching { context.unregisterReceiver(receiver) }
    }
}
