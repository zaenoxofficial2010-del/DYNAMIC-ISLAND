package com.example.dynamicisland.events

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log

/**
 * Reads real, currently-active media sessions via MediaSessionManager.
 * Requires the app to also be an enabled NotificationListener (Android requirement
 * for third-party apps to query active sessions) — see IslandNotificationListener,
 * whose ComponentName is passed in here.
 */
class MediaEventSource(
    private val context: Context,
    private val listenerComponent: ComponentName,
    private val eventManager: EventManager
) {
    companion object {
        private const val KEY = "media"
        private const val TAG = "MediaEventSource"
    }

    private var activeController: MediaController? = null
    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publish(activeController)
        }

        override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
            publish(activeController)
        }
    }

    fun start() {
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        try {
            manager.addOnActiveSessionsChangedListener({ controllers ->
                bindTo(controllers?.firstOrNull())
            }, listenerComponent)
            bindTo(manager.getActiveSessions(listenerComponent).firstOrNull())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification access not granted; media events unavailable", e)
        }
    }

    private fun bindTo(controller: MediaController?) {
        activeController?.unregisterCallback(callback)
        activeController = controller
        controller?.registerCallback(callback)
        publish(controller)
    }

    private fun publish(controller: MediaController?) {
        val metadata = controller?.metadata
        val playback = controller?.playbackState
        if (controller == null || metadata == null || playback == null ||
            playback.state == PlaybackState.STATE_STOPPED || playback.state == PlaybackState.STATE_NONE
        ) {
            eventManager.clear(KEY)
            return
        }
        val title = metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) ?: "Unknown"
        val artist = metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        eventManager.push(
            KEY,
            IslandEvent.Media(
                title = title,
                artist = artist,
                isPlaying = playback.state == PlaybackState.STATE_PLAYING
            )
        )
    }

    fun stop() {
        activeController?.unregisterCallback(callback)
        activeController = null
    }
}
