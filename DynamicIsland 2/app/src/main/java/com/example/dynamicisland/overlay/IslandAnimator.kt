package com.example.dynamicisland.overlay

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/** Shared spring specs so every dimension (width/height/corner) morphs in sync. */
object IslandAnimator {
    fun <T> springSpec() = spring<T>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    val COLLAPSED_WIDTH_DP = 120
    val COLLAPSED_HEIGHT_DP = 34
    val EXPANDED_WIDTH_DP = 320
    val EXPANDED_HEIGHT_DP = 130
    val CORNER_COLLAPSED_DP = 20
    val CORNER_EXPANDED_DP = 32
}
