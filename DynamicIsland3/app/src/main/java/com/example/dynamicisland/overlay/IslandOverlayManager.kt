package com.example.dynamicisland.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.dynamicisland.events.EventManager

/**
 * Owns exactly one overlay ComposeView added to WindowManager with
 * TYPE_APPLICATION_OVERLAY. Completely independent of MainActivity's view
 * hierarchy — this class is instantiated and driven only by DynamicIslandService.
 *
 * Guarantees a single overlay instance: addOverlay() is a no-op if already added.
 */
class IslandOverlayManager(
    private val context: Context,
    private val eventManager: EventManager
) : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val positionManager = IslandPositionManager(context)

    private var composeView: ComposeView? = null
    private val expandedState = mutableStateOf(false)

    val isAdded: Boolean get() = composeView != null

    @SuppressLint("ClickableViewAccessibility")
    fun addOverlay(onLongPress: () -> Unit) {
        if (isAdded) return // never allow duplicate overlays

        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        val placement = positionManager.computePlacement()

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(this@IslandOverlayManager)
            setViewTreeViewModelStoreOwner(this@IslandOverlayManager)
            setViewTreeSavedStateRegistryOwner(this@IslandOverlayManager)
            setContent {
                IslandView(
                    eventManager = eventManager,
                    expanded = expandedState.value,
                    onTap = { expandedState.value = !expandedState.value },
                    onLongPress = onLongPress
                )
            }
        }

        val layoutParams = WindowManager.LayoutParams(
            placement.recommendedWidthPx,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = placement.topOffsetPx
        }

        windowManager.addView(view, layoutParams)
        composeView = view
    }

    fun removeOverlay() {
        val view = composeView ?: return
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        windowManager.removeView(view)
        composeView = null
    }

    fun collapse() {
        expandedState.value = false
    }
}
