package com.example.dynamicisland.events

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A self-contained countdown timer owned by the Island itself.
 * Note: Android has no public API for a normal app to read another app's
 * (e.g. the system Clock app's) private timer state, so this is implemented
 * as the Island's own timer feature, started from the control-center UI.
 */
class TimerEventSource(
    private val scope: CoroutineScope,
    private val eventManager: EventManager
) {
    companion object {
        private const val KEY = "timer"
    }

    private var job: Job? = null

    fun start(label: String, totalSeconds: Int) {
        job?.cancel()
        job = scope.launch {
            var remaining = totalSeconds
            while (remaining >= 0) {
                eventManager.push(KEY, IslandEvent.Timer(label, remaining, isRunning = true))
                delay(1000)
                remaining--
            }
            eventManager.clear(KEY)
        }
    }

    fun stop() {
        job?.cancel()
        eventManager.clear(KEY)
    }
}
