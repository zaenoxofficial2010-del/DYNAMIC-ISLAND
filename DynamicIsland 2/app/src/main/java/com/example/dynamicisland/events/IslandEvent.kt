package com.example.dynamicisland.events

import com.example.dynamicisland.overlay.IslandState

/**
 * All the discrete events an EventSource can push into the EventManager.
 * Each event carries its own priority so higher-priority events (calls, alarms)
 * can preempt lower-priority ones (charging, battery) without flicker.
 */
sealed class IslandEvent(val state: IslandState, val priority: Int, val autoCollapseMs: Long?) {

    data class Call(
        val callerName: String,
        val isIncoming: Boolean,
        val durationSeconds: Int = 0
    ) : IslandEvent(IslandState.CALL, priority = 100, autoCollapseMs = null)

    data class Alarm(val label: String) :
        IslandEvent(IslandState.ALARM, priority = 90, autoCollapseMs = 8_000)

    data class Timer(
        val label: String,
        val remainingSeconds: Int,
        val isRunning: Boolean
    ) : IslandEvent(IslandState.TIMER, priority = 80, autoCollapseMs = null)

    data class Media(
        val title: String,
        val artist: String,
        val isPlaying: Boolean,
        val albumArtUri: String? = null
    ) : IslandEvent(IslandState.MEDIA, priority = 60, autoCollapseMs = null)

    data class Notification(
        val appName: String,
        val title: String,
        val text: String
    ) : IslandEvent(IslandState.NOTIFICATION, priority = 40, autoCollapseMs = 4_000)

    data class Charging(val isFull: Boolean, val percent: Int) :
        IslandEvent(IslandState.CHARGING, priority = 20, autoCollapseMs = 3_000)

    data object Clear : IslandEvent(IslandState.COLLAPSED, priority = -1, autoCollapseMs = null)
}
