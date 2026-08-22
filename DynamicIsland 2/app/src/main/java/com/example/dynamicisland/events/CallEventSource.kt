package com.example.dynamicisland.events

import android.content.Context
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Reads real call state via TelephonyCallback (API 31+). Caller identity is limited
 * to what READ_PHONE_STATE legitimately exposes to a normal app — no CNAP/CNAM lookup.
 */
@RequiresApi(31)
class CallEventSource(
    private val context: Context,
    private val scope: CoroutineScope,
    private val eventManager: EventManager
) {
    companion object {
        private const val KEY = "call"
    }

    private var durationJob: Job? = null
    private var telephonyManager: TelephonyManager? = null

    private val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    eventManager.push(KEY, IslandEvent.Call(callerName = "Incoming call", isIncoming = true))
                }
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    startDurationTicker()
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    durationJob?.cancel()
                    eventManager.clear(KEY)
                }
            }
        }
    }

    fun start() {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager = tm
        val executor = Executor { command -> command.run() }
        tm.registerTelephonyCallback(executor, callback)
    }

    private fun startDurationTicker() {
        durationJob?.cancel()
        durationJob = scope.launch {
            var seconds = 0
            while (true) {
                eventManager.push(KEY, IslandEvent.Call(callerName = "On call", isIncoming = false, durationSeconds = seconds))
                delay(1000)
                seconds++
            }
        }
    }

    fun stop() {
        durationJob?.cancel()
        telephonyManager?.unregisterTelephonyCallback(callback)
    }
}
