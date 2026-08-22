package com.example.dynamicisland.overlay

/**
 * All visual/behavioral states the Island can be in.
 * A single state manager (IslandOverlayManager) owns transitions between these.
 */
enum class IslandState {
    HIDDEN,
    COLLAPSED,
    EXPANDING,
    EXPANDED,
    COLLAPSING,
    MEDIA,
    CALL,
    NOTIFICATION,
    CHARGING,
    TIMER,
    ALARM
}
