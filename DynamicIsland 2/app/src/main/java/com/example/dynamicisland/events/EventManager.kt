package com.example.dynamicisland.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Owns the single "current" event shown by the Island.
 * Higher priority events preempt lower ones. When a preempting event's
 * autoCollapseMs elapses, control reverts to the highest-priority event
 * still active (e.g. a call ending reveals the music that was playing underneath).
 */
class EventManager(private val scope: CoroutineScope) {

    private val activeEvents = linkedMapOf<String, IslandEvent>()
    private var collapseJob: Job? = null

    private val _currentEvent = MutableStateFlow<IslandEvent?>(null)
    val currentEvent: StateFlow<IslandEvent?> = _currentEvent

    fun push(key: String, event: IslandEvent) {
        activeEvents[key] = event
        recompute()
        scheduleAutoCollapse(key, event)
    }

    fun clear(key: String) {
        activeEvents.remove(key)
        recompute()
    }

    fun clearAll() {
        activeEvents.clear()
        collapseJob?.cancel()
        _currentEvent.value = null
    }

    private fun recompute() {
        val top = activeEvents.values.maxByOrNull { it.priority }
        _currentEvent.value = top
    }

    private fun scheduleAutoCollapse(key: String, event: IslandEvent) {
        val timeout = event.autoCollapseMs ?: return
        collapseJob?.cancel()
        collapseJob = scope.launch {
            delay(timeout)
            activeEvents.remove(key)
            recompute()
        }
    }
}
